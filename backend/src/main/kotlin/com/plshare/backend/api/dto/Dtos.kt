package com.plshare.backend.api.dto

import com.plshare.backend.domain.entity.Asset
import com.plshare.backend.domain.entity.ExportJob
import com.plshare.backend.domain.entity.ImportJob
import com.plshare.backend.domain.entity.Track
import com.plshare.backend.infrastructure.spotify.SpotifyPlaylistResponse
import java.time.LocalDateTime
import java.util.UUID

// ----- Playlists -----
data class PlaylistSummaryDto(
    val id: String,
    val name: String,
    val coverUrl: String?,
    val trackCount: Int
) {
    companion object {
        fun from(p: SpotifyPlaylistResponse) = PlaylistSummaryDto(
            id = p.id,
            name = p.name,
            coverUrl = p.images.firstOrNull()?.url,
            trackCount = p.tracks.items.size
        )
    }
}

// ----- Imports -----
data class CreateImportRequest(
    val playlistId: String
)

data class ImportJobDto(
    val jobId: UUID,
    val status: String,
    val totalTracks: Int,
    val processedTracks: Int,
    val progress: Int,
    val assetId: UUID?,
    val errorCode: String?,
    val errorMessage: String?
) {
    companion object {
        fun from(job: ImportJob): ImportJobDto {
            val pct = if (job.totalTracks > 0) (job.processedTracks * 100) / job.totalTracks else 0
            return ImportJobDto(
                jobId = job.id,
                status = job.status.name.lowercase(),
                totalTracks = job.totalTracks,
                processedTracks = job.processedTracks,
                progress = pct,
                assetId = job.assetId,
                errorCode = job.errorCode,
                errorMessage = job.errorMessage
            )
        }
    }
}

// ----- Assets -----
data class TrackDto(
    val id: UUID,
    val name: String,
    val artist: String,
    val durationMs: Int?,
    val isrc: String?,
    val spotifyId: String?,
    val appleMusicId: String?
) {
    companion object {
        fun from(t: Track) = TrackDto(
            id = t.id,
            name = t.name,
            artist = t.artist,
            durationMs = t.durationMs,
            isrc = t.isrc,
            spotifyId = t.spotifyId,
            appleMusicId = t.appleMusicId
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

// ----- Exports -----
data class CreateExportRequest(
    val assetId: UUID,
    val targetPlatform: String = "apple"
)

data class ExportJobDto(
    val jobId: UUID,
    val assetId: UUID,
    val targetPlatform: String,
    val status: String,
    val totalTracks: Int,
    val matchedTracks: Int,
    val failedTracks: Int,
    val externalPlaylistId: String?,
    val externalUrl: String?,
    val lastError: String?
) {
    companion object {
        fun from(j: ExportJob) = ExportJobDto(
            jobId = j.id,
            assetId = j.assetId,
            targetPlatform = j.targetPlatform,
            status = j.status.name.lowercase(),
            totalTracks = j.totalTracks,
            matchedTracks = j.matchedTracks,
            failedTracks = j.failedTracks,
            externalPlaylistId = j.externalPlaylistId,
            externalUrl = j.externalUrl,
            lastError = j.lastError
        )
    }
}

data class ExportResultDto(
    val jobId: UUID,
    val status: String,
    val externalPlaylistId: String?,
    val externalUrl: String?,
    val matchedTracks: Int,
    val failedTracks: Int
)
