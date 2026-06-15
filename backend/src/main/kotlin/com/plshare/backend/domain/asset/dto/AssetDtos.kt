package com.plshare.backend.domain.asset.dto

import com.plshare.backend.domain.asset.model.Asset
import com.plshare.backend.domain.asset.model.AssetKind
import com.plshare.backend.domain.asset.model.Track
import java.time.LocalDateTime
import java.util.UUID

data class TrackDto(
    val id: UUID,
    val name: String,
    val artist: String,
    val durationMs: Int?,
    val isrc: String?,
    val spotifyId: String?,
    val appleMusicId: String?,
    val youtubeVideoId: String?
) {
    companion object {
        fun from(t: Track) = TrackDto(
            id = t.id,
            name = t.name,
            artist = t.artist,
            durationMs = t.durationMs,
            isrc = t.isrc,
            spotifyId = t.spotifyId,
            appleMusicId = t.appleMusicId,
            youtubeVideoId = t.youtubeVideoId
        )
    }
}

data class AssetSummaryDto(
    val id: UUID,
    val title: String,
    val coverUrl: String?,
    val description: String?,
    val emotionTags: List<String>,
    val trackCount: Int,
    val sourcePlatform: String,
    val assetKind: AssetKind,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(a: Asset) = AssetSummaryDto(
            id = a.id,
            title = a.title,
            coverUrl = a.coverUrl,
            description = a.description,
            emotionTags = a.emotionTags.toList(),
            trackCount = a.tracks.size,
            sourcePlatform = a.sourcePlatform,
            assetKind = a.assetKind,
            createdAt = a.createdAt
        )
    }
}

data class AssetDetailDto(
    val id: UUID,
    val title: String,
    val coverUrl: String?,
    val description: String?,
    val diaryText: String?,
    val emotionTags: List<String>,
    val photoUrls: List<String>,
    val sourcePlatform: String,
    val shareToken: String?,
    val tracks: List<TrackDto>,
    val assetKind: AssetKind,
    val moodVideoId: String?,
    val moodChannelName: String?,
    val moodTrackListText: String?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(a: Asset) = AssetDetailDto(
            id = a.id,
            title = a.title,
            coverUrl = a.coverUrl,
            description = a.description,
            diaryText = a.diaryText,
            emotionTags = a.emotionTags.toList(),
            photoUrls = a.photoUrls.toList(),
            sourcePlatform = a.sourcePlatform,
            shareToken = a.shareToken,
            tracks = a.tracks.map { TrackDto.from(it) },
            assetKind = a.assetKind,
            moodVideoId = a.moodVideoId,
            moodChannelName = a.moodChannelName,
            moodTrackListText = a.moodTrackListText,
            createdAt = a.createdAt
        )
    }
}

data class UpdateAssetRequest(
    val title: String? = null,
    val diaryText: String? = null,
    val description: String? = null,
    val coverUrl: String? = null,
    val emotionTags: List<String>? = null,
    val photoUrls: List<String>? = null
)

data class ShareResponseDto(
    val shareToken: String,
    val url: String
)
