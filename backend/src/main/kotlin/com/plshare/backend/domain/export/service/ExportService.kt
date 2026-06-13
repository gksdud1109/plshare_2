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
 *   소스가 "youtube"인 트랙: Track.youtubeVideoId 컬럼에 videoId가 저장됨.
 *   소스가 "youtube"가 아닌 트랙: videoId 없음 → failed 처리 + reason="no_video_id".
 *   search.list(100u/호출) 기반 매칭은 현재 범위 외(TODO 후속).
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
    private val youtubeQuotaGuard: YouTubeQuotaGuard
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun requestExport(idempotencyKey: String, assetId: UUID, targetPlatform: String): UUID {
        val existing = exportJobRepository.findByIdempotencyKey(idempotencyKey)
        if (existing != null) return existing.id

        val asset = assetRepository.findById(assetId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "Asset not found: $assetId")
        }

        if (targetPlatform != "apple" && targetPlatform != "youtube") {
            throw ApiException(ErrorCode.VALIDATION_FAILED, "지원하지 않는 export 플랫폼: $targetPlatform")
        }

        val job = ExportJob(
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
     *   - asset.sourcePlatform == "youtube": Track.youtubeVideoId = videoId
     *   - 그 외: videoId 없음 → failed + reason="no_video_id"
     *     (search.list 매칭은 범위 외, TODO 후속 — 100u/호출로 추가 쿼터 필요)
     *
     * accessToken은 현재 빈 문자열로 전달(demo 환경에서는 무시됨).
     * 실제 연동 시 사용자 OAuth2 토큰을 ExportJob에 추가하거나 별도 토큰 저장소에서 조회해야 한다.
     */
    private fun runYouTubeExport(job: ExportJob) {
        try {
            job.start()
            exportJobRepository.save(job)

            val asset = assetRepository.findById(job.assetId).orElseThrow()

            // 쿼터 예약: 실패 시 QUOTA_EXCEEDED ApiException → fail()
            val estimatedUnits = YouTubeQuotaGuard.estimatedCost(asset.tracks.size)
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
            val accessToken = "" // TODO: 실제 사용자 OAuth2 토큰 주입 (현재 demo 환경에서 무시됨)
            val playlistRef = youtubeWriteAdapter.createPlaylist(asset.title, asset.description, accessToken)

            job.markReady()
            exportJobRepository.save(job)

            job.markExecuting()
            exportJobRepository.save(job)

            // 트랙별 videoId 확보 및 추가
            val results = mutableListOf<YouTubeMusicTrackResult>()
            for (track in asset.tracks) {
                val videoId = resolveVideoId(track, asset.sourcePlatform)
                if (videoId == null) {
                    results.add(
                        YouTubeMusicTrackResult(
                            videoId = null,
                            title = track.name,
                            status = "failed",
                            reason = "no_video_id: 소스 플랫폼(${asset.sourcePlatform})에서 YouTube videoId를 확인할 수 없습니다. search.list 매칭은 범위 외(TODO)"
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
     * sourcePlatform="youtube"인 경우 Track.youtubeVideoId 컬럼에 videoId가 저장된다.
     * 다른 플랫폼 소스 트랙은 videoId가 없으므로 null 반환.
     *
     * @param track 대상 트랙
     * @param assetSourcePlatform asset의 소스 플랫폼 ("youtube", "spotify" 등)
     * @return videoId (없으면 null)
     */
    private fun resolveVideoId(track: com.plshare.backend.domain.asset.model.Track, assetSourcePlatform: String): String? {
        return if (assetSourcePlatform == "youtube") {
            // youtube 소스 트랙의 videoId는 전용 컬럼(youtubeVideoId)에 저장됨
            track.youtubeVideoId
        } else {
            // 다른 플랫폼 소스: videoId 없음
            // TODO 후속: search.list(100u/호출)로 매칭 — 현재 범위 외
            null
        }
    }
}
