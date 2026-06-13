package com.plshare.backend.domain.export.service

import com.plshare.backend.domain.asset.repository.AssetRepository
import com.plshare.backend.domain.export.model.ExportJob
import com.plshare.backend.domain.export.repository.ExportJobRepository
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import com.plshare.backend.infrastructure.apple.AppleMusicTrackInput
import com.plshare.backend.infrastructure.apple.AppleMusicWriteAdapter
import com.plshare.backend.infrastructure.youtube.YouTubeMusicTrackResult
import com.plshare.backend.infrastructure.youtube.YouTubeQuotaGuard
import com.plshare.backend.infrastructure.youtube.YouTubeWriteAdapter
import com.plshare.backend.infrastructure.youtube.YouTubeClient
import com.plshare.backend.domain.auth.service.GoogleAccessGrantService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 플레이리스트 export 서비스.
 *
 * ## 플랫폼 분기
 *   - "apple"   → [AppleMusicWriteAdapter] (기존 동작 불변)
 *   - "youtube" → [YouTubeWriteAdapter] + [YouTubeQuotaGuard]
 *   - 기타 → [ApiException](VALIDATION_FAILED)
 *
 * ## YouTube export videoId 확보 정책
 *   소스가 "youtube"인 트랙은 저장된 Track.youtubeVideoId를 사용한다.
 *   그 외 플랫폼 트랙은 search.list로 videoId를 검색한다.
 *   검색 결과가 없을 때만 failed 처리한다.
 *
 * ## YouTube 쿼터 예약
 *   export 시작 전 [YouTubeQuotaGuard.reserve](50 + 50*트랙수)를 호출한다.
 *   예산 초과 시 job을 FAILED(QUOTA_EXCEEDED 사유)로 즉시 종료한다.
 */
@Service
class ExportService(
    private val exportJobRepository: ExportJobRepository,
    private val assetRepository: AssetRepository,
    private val appleAdapter: AppleMusicWriteAdapter,
    private val youtubeWriteAdapter: YouTubeWriteAdapter,
    private val youtubeQuotaGuard: YouTubeQuotaGuard,
    private val youtubeClient: YouTubeClient? = null,
    private val googleAccessGrantService: GoogleAccessGrantService? = null,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun requestExport(
        idempotencyKey: String,
        assetId: UUID,
        targetPlatform: String,
        ownerId: UUID? = null,
    ): UUID {
        val existing = exportJobRepository.findByIdempotencyKey(idempotencyKey)
        if (existing != null) {
            if (ownerId != null && existing.ownerId != ownerId) {
                throw ApiException(ErrorCode.CONFLICT, "Idempotency key belongs to another user")
            }
            return existing.id
        }

        val asset = assetRepository.findById(assetId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "Asset not found: $assetId")
        }
        if (ownerId != null && asset.ownerId != ownerId) {
            throw ApiException(ErrorCode.FORBIDDEN, "Asset does not belong to the current user")
        }

        if (targetPlatform != "apple" && targetPlatform != "youtube") {
            throw ApiException(ErrorCode.VALIDATION_FAILED, "지원하지 않는 export 플랫폼: $targetPlatform")
        }

        val job = ExportJob(
            ownerId = ownerId,
            assetId = asset.id,
            targetPlatform = targetPlatform,
            idempotencyKey = idempotencyKey,
            totalTracks = asset.tracks.size
        )
        exportJobRepository.save(job)

        runExport(job.id)

        return job.id
    }

    @Async
    fun runExport(jobId: UUID) {
        // outer @Transactional commit may not be visible yet — retry briefly.
        var attempt = 0
        var found: ExportJob? = null
        while (attempt < 10 && found == null) {
            found = exportJobRepository.findById(jobId).orElse(null)
            if (found == null) {
                Thread.sleep(100)
                attempt++
            }
        }
        val job = found ?: run {
            log.error("Export job not found after retries: $jobId")
            return
        }

        when (job.targetPlatform) {
            "apple" -> runAppleExport(job)
            "youtube" -> runYouTubeExport(job)
            else -> {
                log.error("Unknown targetPlatform '{}' for job {}", job.targetPlatform, jobId)
                job.fail("unsupported_platform: ${job.targetPlatform}")
                exportJobRepository.save(job)
            }
        }
    }

    // ─── Apple export (기존 동작 불변) ───────────────────────────────────────

    private fun runAppleExport(job: ExportJob) {
        try {
            job.start()
            exportJobRepository.save(job)

            val asset = assetRepository.findById(job.assetId).orElseThrow()
            val inputs = asset.tracks.map {
                AppleMusicTrackInput(isrc = it.isrc, title = it.name, artist = it.artist)
            }

            val ref = appleAdapter.createPlaylist(asset.title, asset.description).block()
                ?: throw IllegalStateException("Apple playlist creation failed")

            job.markReady()
            exportJobRepository.save(job)

            job.markExecuting()
            exportJobRepository.save(job)

            val addResult = appleAdapter.addTracks(ref, inputs).block()
                ?: throw IllegalStateException("Apple addTracks failed")

            appleAdapter.verify(ref, addResult.matched).block()

            job.complete(
                externalPlaylistId = ref.externalId,
                externalUrl = ref.externalUrl,
                matched = addResult.matched,
                failed = addResult.skipped
            )
            exportJobRepository.save(job)
        } catch (e: Exception) {
            log.error("Apple export failed for job {}", job.id, e)
            job.fail(e.message ?: "unknown_error")
            exportJobRepository.save(job)
        }
    }

    // ─── YouTube export ──────────────────────────────────────────────────────

    /**
     * YouTube Music export 실행.
     *
     * videoId 확보 전략:
     *   - asset.sourcePlatform == "youtube": Track.youtubeVideoId 우선
     *   - 그 외: search.list 결과 사용
     *
     * 인증된 작업은 사용자 Google access grant를 사용한다.
     */
    private fun runYouTubeExport(job: ExportJob) {
        try {
            job.start()
            exportJobRepository.save(job)

            val asset = assetRepository.findById(job.assetId).orElseThrow()

            // 쿼터 예약: 실패 시 QUOTA_EXCEEDED ApiException → fail()
            val searchCount = asset.tracks.count { it.youtubeVideoId.isNullOrBlank() }
            val estimatedUnits = YouTubeQuotaGuard.estimatedCost(asset.tracks.size, searchCount)
            try {
                youtubeQuotaGuard.reserve(estimatedUnits)
            } catch (e: ApiException) {
                if (e.code == ErrorCode.QUOTA_EXCEEDED) {
                    log.warn("YouTube quota exceeded for job {}: {}", job.id, e.message)
                    job.fail("QUOTA_EXCEEDED: ${e.message}")
                    exportJobRepository.save(job)
                    return
                }
                throw e
            }

            // 플레이리스트 생성
            val accessToken = job.ownerId?.let { ownerId ->
                googleAccessGrantService?.getValidYouTubeToken(ownerId)
                    ?: throw IllegalStateException("Google access grant service unavailable")
            } ?: "demo-youtube-token"
            val playlistRef = youtubeWriteAdapter.createPlaylist(asset.title, asset.description, accessToken)

            job.markReady()
            exportJobRepository.save(job)

            job.markExecuting()
            exportJobRepository.save(job)

            // 트랙별 videoId 확보 및 추가
            val results = mutableListOf<YouTubeMusicTrackResult>()
            for (track in asset.tracks) {
                val videoId = resolveVideoId(track, accessToken)
                if (videoId == null) {
                    results.add(
                        YouTubeMusicTrackResult(
                            videoId = null,
                            title = track.name,
                            status = "failed",
                            reason = "no_video_id: ${track.artist} - ${track.name} 검색 결과가 없습니다."
                        )
                    )
                    continue
                }

                try {
                    youtubeWriteAdapter.addItem(playlistRef.externalId, videoId, accessToken)
                    results.add(
                        YouTubeMusicTrackResult(
                            videoId = videoId,
                            title = track.name,
                            status = "added"
                        )
                    )
                } catch (e: ApiException) {
                    log.warn("YouTube addItem failed for track '{}' (videoId={}): {}", track.name, videoId, e.message)
                    results.add(
                        YouTubeMusicTrackResult(
                            videoId = videoId,
                            title = track.name,
                            status = "failed",
                            reason = e.message ?: "addItem_error"
                        )
                    )
                }
            }

            val added = results.count { it.status == "added" }
            val failed = results.count { it.status == "failed" }

            job.complete(
                externalPlaylistId = playlistRef.externalId,
                externalUrl = playlistRef.externalUrl,
                matched = added,
                failed = failed
            )
            exportJobRepository.save(job)

            log.info("YouTube export completed for job {}: added={}, failed={}", job.id, added, failed)
        } catch (e: Exception) {
            log.error("YouTube export failed for job {}", job.id, e)
            job.fail(e.message ?: "unknown_error")
            exportJobRepository.save(job)
        }
    }

    /**
     * 트랙에서 YouTube videoId를 확보한다.
     *
     * @param track 대상 트랙
     * @return videoId (없으면 null)
     */
    private fun resolveVideoId(
        track: com.plshare.backend.domain.asset.model.Track,
        accessToken: String,
    ): String? {
        track.youtubeVideoId?.takeIf { it.isNotBlank() }?.let { return it }
        return youtubeClient?.searchVideo(track.name, track.artist, accessToken)?.block()
    }
}
