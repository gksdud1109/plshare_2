package com.plshare.backend.domain.export.service

import com.plshare.backend.domain.asset.model.Track
import com.plshare.backend.domain.asset.repository.AssetRepository
import com.plshare.backend.domain.export.model.ExportJob
import com.plshare.backend.domain.export.model.ExportJobStatus
import com.plshare.backend.domain.export.model.ExportMatchStatus
import com.plshare.backend.domain.export.model.ExportTrackMatch
import com.plshare.backend.domain.export.repository.ExportJobRepository
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import com.plshare.backend.infrastructure.apple.AppleMusicTrackInput
import com.plshare.backend.infrastructure.apple.AppleMusicWriteAdapter
import com.plshare.backend.infrastructure.youtube.YouTubeQuotaGuard
import com.plshare.backend.infrastructure.youtube.YouTubeSearchCandidate
import com.plshare.backend.infrastructure.youtube.YouTubeWriteAdapter
import com.plshare.backend.infrastructure.youtube.YouTubeClient
import com.plshare.backend.domain.auth.service.GoogleAccessGrantService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
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
    private val exportTrackMatchRepository: com.plshare.backend.domain.export.repository.ExportTrackMatchRepository,
    private val youtubeClient: YouTubeClient? = null,
    private val googleAccessGrantService: GoogleAccessGrantService? = null,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        /** Match confidence at or above this is treated as a confident MATCHED. */
        const val CONFIDENCE_THRESHOLD = 0.7
        /** search.list costs 100 quota units; reserve before a candidate lookup. */
        const val CANDIDATE_SEARCH_UNITS = 100L
        private val NOISE_TOKENS = setOf(
            "live", "cover", "remix", "acoustic", "lyric", "lyrics", "sped",
            "slowed", "reverb", "instrumental", "karaoke", "performance", "concert",
        )
    }

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

            // 트랙별 매칭 + 추가 + per-track match 영속화.
            // 재실행 안전: 같은 job의 기존 match 레코드를 먼저 제거한다.
            exportTrackMatchRepository.findByExportJobId(job.id)
                .takeIf { it.isNotEmpty() }
                ?.let { exportTrackMatchRepository.deleteAll(it) }

            var added = 0
            var failed = 0
            for (track in asset.tracks) {
                val resolved = resolveMatch(track, accessToken)
                var status = resolved.status
                var matchedVideoId = resolved.videoId

                if (matchedVideoId != null) {
                    try {
                        youtubeWriteAdapter.addItem(playlistRef.externalId, matchedVideoId, accessToken)
                        added++
                    } catch (e: ApiException) {
                        log.warn(
                            "YouTube addItem failed for track '{}' (videoId={}): {}",
                            track.name, matchedVideoId, e.message,
                        )
                        status = ExportMatchStatus.FAILED
                        matchedVideoId = null
                        failed++
                    }
                } else {
                    failed++
                }

                exportTrackMatchRepository.save(
                    ExportTrackMatch(
                        exportJobId = job.id,
                        trackId = track.id,
                        title = track.name,
                        artist = track.artist,
                        matchedVideoId = matchedVideoId,
                        matchedTitle = if (matchedVideoId != null) resolved.title else null,
                        confidence = resolved.confidence,
                        status = status,
                    )
                )
            }

            job.complete(
                externalPlaylistId = playlistRef.externalId,
                externalUrl = playlistRef.externalUrl,
                matched = added,
                failed = failed
            )
            exportJobRepository.save(job)

            val needsReview = exportTrackMatchRepository.findByExportJobId(job.id)
                .count { it.status != ExportMatchStatus.MATCHED }
            log.info(
                "YouTube export completed for job {}: added={}, failed={}, needsReview={}",
                job.id, added, failed, needsReview,
            )
        } catch (e: Exception) {
            log.error("YouTube export failed for job {}", job.id, e)
            job.fail(e.message ?: "unknown_error")
            exportJobRepository.save(job)
        }
    }

    // ─── 트랙 매칭 (confidence) ────────────────────────────────────────────────

    private data class ResolvedMatch(
        val videoId: String?,
        val title: String?,
        val confidence: Double?,
        val status: ExportMatchStatus,
    )

    /**
     * 트랙을 대상 YouTube 비디오에 매칭하고 신뢰도/상태를 산출한다.
     *   - youtube 소스 트랙: 저장된 videoId를 그대로 사용(exact, 1.0).
     *   - 그 외: searchVideoCandidates의 top 후보와 제목/아티스트 유사도로 신뢰도 산출.
     *     threshold 미만이면 ALTERNATIVE(검토 필요), 후보 없으면 FAILED.
     */
    private fun resolveMatch(track: Track, accessToken: String): ResolvedMatch {
        track.youtubeVideoId?.takeIf { it.isNotBlank() }?.let {
            return ResolvedMatch(it, track.name, 1.0, ExportMatchStatus.MATCHED)
        }
        val primary = youtubeClient
            ?.searchVideoCandidates(track.name, track.artist, accessToken)
            ?.block()
            ?.firstOrNull()
            ?: return ResolvedMatch(null, null, null, ExportMatchStatus.FAILED)

        val confidence = matchConfidence(track.name, track.artist, primary)
        val status = if (confidence >= CONFIDENCE_THRESHOLD) {
            ExportMatchStatus.MATCHED
        } else {
            ExportMatchStatus.ALTERNATIVE
        }
        return ResolvedMatch(primary.videoId, primary.title, confidence, status)
    }

    /**
     * 트랙(아티스트+제목)과 후보(채널+제목)의 토큰 Jaccard 유사도(0–1).
     * 소스가 요청하지 않은 live/cover/remix 류 노이즈 토큰이 후보에 있으면 감점한다
     * — 스튜디오 플레이리스트 변환에서 라이브/커버는 보통 의도한 곡이 아니다.
     */
    fun matchConfidence(trackName: String, trackArtist: String, candidate: YouTubeSearchCandidate): Double {
        val query = tokenize("$trackArtist $trackName")
        val target = tokenize("${candidate.channelTitle.orEmpty()} ${candidate.title}")
        if (query.isEmpty() || target.isEmpty()) return 0.0
        val jaccard = query.intersect(target).size.toDouble() / query.union(target).size.toDouble()
        val noisePenalty = if (target.any { it in NOISE_TOKENS && it !in query }) 0.25 else 0.0
        return (jaccard - noisePenalty).coerceIn(0.0, 1.0)
    }

    private fun tokenize(s: String): Set<String> =
        s.lowercase()
            .replace(Regex("[^a-z0-9가-힣\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .toSet()

    // ─── 수동 검토 (manual review) ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    fun getMatches(jobId: UUID, requesterId: UUID?): List<ExportTrackMatch> {
        val job = exportJobRepository.findById(jobId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "Export job not found: $jobId")
        }
        requireOwner(job.ownerId, requesterId)
        return exportTrackMatchRepository.findByExportJobId(jobId)
    }

    /**
     * 한 트랙에 대한 대체 후보를 재검색한다(quota 100u 예약). 검토 UI가 이 목록에서
     * 올바른 비디오를 고른 뒤 [overrideMatch]로 확정한다.
     */
    fun getCandidates(jobId: UUID, trackId: UUID, requesterId: UUID?): List<YouTubeSearchCandidate> {
        val job = exportJobRepository.findById(jobId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "Export job not found: $jobId")
        }
        requireOwner(job.ownerId, requesterId)
        val match = exportTrackMatchRepository.findByExportJobIdAndTrackId(jobId, trackId)
            ?: throw ApiException(ErrorCode.NOT_FOUND, "No match for track $trackId in job $jobId")

        youtubeQuotaGuard.reserve(CANDIDATE_SEARCH_UNITS)
        val token = youtubeTokenFor(job.ownerId)
        return youtubeClient?.searchVideoCandidates(match.title, match.artist, token)?.block() ?: emptyList()
    }

    /**
     * 사용자가 선택한 비디오로 트랙 매칭을 확정한다. 외부 플레이리스트에 추가하고
     * match를 MATCHED(reviewed=true)로 갱신한 뒤 job 집계를 재계산한다.
     */
    @Transactional
    fun overrideMatch(
        jobId: UUID,
        trackId: UUID,
        videoId: String,
        videoTitle: String?,
        requesterId: UUID?,
    ): ExportTrackMatch {
        val job = exportJobRepository.findById(jobId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "Export job not found: $jobId")
        }
        requireOwner(job.ownerId, requesterId)
        val match = exportTrackMatchRepository.findByExportJobIdAndTrackId(jobId, trackId)
            ?: throw ApiException(ErrorCode.NOT_FOUND, "No match for track $trackId in job $jobId")

        val token = youtubeTokenFor(job.ownerId)
        job.externalPlaylistId?.let { playlistId ->
            youtubeWriteAdapter.addItem(playlistId, videoId, token)
        }

        match.applyOverride(videoId, videoTitle)
        exportTrackMatchRepository.save(match)
        recomputeJobCounts(job)
        return match
    }

    /** override 이후 per-track match를 진실의 원천으로 job 집계/상태를 재계산. */
    private fun recomputeJobCounts(job: ExportJob) {
        val matches = exportTrackMatchRepository.findByExportJobId(job.id)
        val matched = matches.count { it.matchedVideoId != null && it.status != ExportMatchStatus.FAILED }
        val failed = matches.size - matched
        job.matchedTracks = matched
        job.failedTracks = failed
        job.status = if (failed == 0) ExportJobStatus.COMPLETED else ExportJobStatus.PARTIAL
        job.updatedAt = LocalDateTime.now()
        exportJobRepository.save(job)
    }

    private fun youtubeTokenFor(ownerId: UUID?): String =
        ownerId?.let { googleAccessGrantService?.getValidYouTubeToken(it) } ?: "demo-youtube-token"

    private fun requireOwner(jobOwnerId: UUID?, requesterId: UUID?) {
        if (requesterId != null && jobOwnerId != requesterId) {
            throw ApiException(ErrorCode.FORBIDDEN, "Export job does not belong to the current user")
        }
    }
}
