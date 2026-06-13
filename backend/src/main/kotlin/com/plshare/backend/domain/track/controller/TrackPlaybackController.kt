package com.plshare.backend.domain.track.controller

import com.plshare.backend.domain.track.service.TrackPlaybackService
import com.plshare.backend.global.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class TrackYouTubeDto(val videoId: String?)

@RestController
@RequestMapping("/api/tracks")
class TrackPlaybackController(
    private val trackPlaybackService: TrackPlaybackService,
) {
    /**
     * Public: resolve a track to a playable YouTube videoId for inline gift/share
     * playback. Anonymous (gift recipients have no session) — guarded by quota.
     */
    @GetMapping("/{trackId}/youtube")
    fun youtube(@PathVariable trackId: UUID): ApiResponse<TrackYouTubeDto> =
        ApiResponse.ok(TrackYouTubeDto(trackPlaybackService.resolveYouTubeVideoId(trackId)))
}
