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

    @Transactional
    fun resolveYouTubeVideoId(trackId: UUID): String? {
        val track = trackRepository.findById(trackId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "Track not found: $trackId")
        }
        track.youtubeVideoId?.takeIf { it.isNotBlank() }?.let { return it }

        youtubeQuotaGuard?.reserve(SEARCH_UNITS)
        val videoId = youtubeClient
            ?.searchVideoCandidates(track.name, track.artist, "demo-youtube-token")
            ?.block()
            ?.firstOrNull()
            ?.videoId
        if (videoId.isNullOrBlank()) {
            log.info("No YouTube candidate for track {} ({} - {})", trackId, track.artist, track.name)
            return null
        }
        track.youtubeVideoId = videoId
        trackRepository.save(track)
        return videoId
    }

    companion object {
        private const val SEARCH_UNITS = 100L
    }
}
