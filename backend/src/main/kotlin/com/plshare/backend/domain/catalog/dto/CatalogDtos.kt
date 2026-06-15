package com.plshare.backend.domain.catalog.dto

import com.plshare.backend.domain.catalog.model.CuratedTrack
import java.util.UUID

/** GET /api/catalog/tracks 항목. */
data class CuratedTrackDto(
    val id: UUID,
    val title: String,
    val artist: String,
    val youtubeVideoId: String,
    val durationMs: Int?,
    val mood: String,
    val coverUrl: String?,
) {
    companion object {
        fun from(t: CuratedTrack) = CuratedTrackDto(
            id = t.id,
            title = t.title,
            artist = t.artist,
            youtubeVideoId = t.youtubeVideoId,
            durationMs = t.durationMs,
            mood = t.mood,
            coverUrl = t.coverUrl,
        )
    }
}

/** POST /api/assets/compose 요청 — 카탈로그 트랙을 골라 내 플레이리스트(Asset)로 조립. */
data class ComposeAssetRequest(
    val title: String,
    val coverUrl: String? = null,
    val description: String? = null,
    val emotionTags: List<String> = emptyList(),
    val trackIds: List<UUID>,
)

/**
 * POST /api/assets/mood-video 요청 — 단일 유튜브 무드영상을 한 단위 자산(MOOD_VIDEO)으로.
 * videoUrlOrId 는 전체 URL 또는 11자 videoId 둘 다 허용(서버에서 추출).
 */
data class CreateMoodVideoRequest(
    val title: String,
    val videoUrlOrId: String,
    val channelName: String? = null,
    val trackListText: String? = null,
    val coverUrl: String? = null,
    val emotionTags: List<String> = emptyList(),
)

/** 조립 결과 — 생성된 자산으로 이동하기 위한 최소 정보. */
data class ComposedAssetResponse(
    val id: UUID,
    val title: String,
    val trackCount: Int,
)
