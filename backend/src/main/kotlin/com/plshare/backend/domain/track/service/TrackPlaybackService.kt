package com.plshare.backend.domain.track.service

import com.plshare.backend.domain.track.repository.TrackRepository
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import com.plshare.backend.infrastructure.youtube.YouTubeClient
import com.plshare.backend.infrastructure.youtube.YouTubeQuotaGuard
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves a track to a playable YouTube videoId for inline gift/share playback —
 * the v2.1 "catalog/link" tier where anyone plays without an account. The resolved
 * id is cached on Track.youtubeVideoId so each track is searched at most once.
 *
 * NOTE (prod): the demo Mock resolves deterministically and for free. The real
 * public resolve should use a YouTube Data API *key* search (no user OAuth) —
 * searchVideoCandidates currently bearer-auths, so an API-key search path is a
 * prod follow-up. Quota is reserved per resolve regardless of profile.
 */
@Service
class TrackPlaybackService(
    private val trackRepository: TrackRepository,
    private val youtubeClient: YouTubeClient? = null,
    private val youtubeQuotaGuard: YouTubeQuotaGuard? = null,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * 미해결(검색 후보 없음) 트랙의 음성 캐시 — trackId → 만료 epochMs.
     * 성공 결과는 Track.youtubeVideoId(DB)가 단락시키지만, null 결과는 DB에 남지 않아
     * 재호출마다 reserve(100u)+검색을 반복하던 쿼터 드레인 벡터가 있었다. JVM 인메모리라
     * 재시작 시 트랙당 1회 재검색만 발생한다(영속 음성 캐시는 컬럼 추가 시 후속).
     */
    private val unresolvedUntil = ConcurrentHashMap<UUID, Long>()

    @Transactional
    fun resolveYouTubeVideoId(trackId: UUID): String? {
        val track = trackRepository.findById(trackId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "Track not found: $trackId")
        }
        track.youtubeVideoId?.takeIf { it.isNotBlank() }?.let { return it }

        val now = System.currentTimeMillis()
        unresolvedUntil[trackId]?.let { expiry ->
            if (expiry > now) return null          // 최근 미해결 → 쿼터 소모 없이 단락
            unresolvedUntil.remove(trackId)
        }

        youtubeQuotaGuard?.reserve(SEARCH_UNITS)
        val videoId = youtubeClient
            ?.searchVideoCandidates(track.name, track.artist, "demo-youtube-token")
            ?.block()
            ?.firstOrNull()
            ?.videoId
        if (videoId.isNullOrBlank()) {
            log.info("No YouTube candidate for track {} ({} - {})", trackId, track.artist, track.name)
            rememberUnresolved(trackId, now)
            return null
        }
        track.youtubeVideoId = videoId
        trackRepository.save(track)
        return videoId
    }

    private fun rememberUnresolved(trackId: UUID, now: Long) {
        if (unresolvedUntil.size >= NEGATIVE_CACHE_CAP) {
            unresolvedUntil.entries.removeIf { it.value <= now }   // 만료분 정리(경계)
        }
        unresolvedUntil[trackId] = now + NEGATIVE_CACHE_TTL_MS
    }

    companion object {
        private const val SEARCH_UNITS = 100L
        private const val NEGATIVE_CACHE_TTL_MS = 10 * 60 * 1000L
        private const val NEGATIVE_CACHE_CAP = 10_000
    }
}
