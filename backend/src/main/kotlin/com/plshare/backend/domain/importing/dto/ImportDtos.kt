package com.plshare.backend.domain.importing.dto

import com.plshare.backend.domain.importing.model.ImportJob
import com.plshare.backend.infrastructure.spotify.SpotifyPlaylistResponse
import com.plshare.backend.infrastructure.youtube.YouTubePlaylistSummary
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

// ----- YouTube Playlists -----
data class YouTubePlaylistSummaryDto(
    val id: String,
    val name: String,
    val coverUrl: String?,
    val trackCount: Int
) {
    companion object {
        fun from(p: YouTubePlaylistSummary) = YouTubePlaylistSummaryDto(
            id = p.id,
            name = p.title,
            coverUrl = p.coverUrl,
            trackCount = p.items.size
        )
    }
}

// ----- Imports -----
data class CreateImportRequest(
    val playlistId: String,
    /**
     * 소스 플랫폼. 기본값 "spotify" — 기존 FE/E2E 요청(playlistId만 보내는 형태) 하위호환.
     * 지원값: "spotify" | "youtube"
     */
    val sourcePlatform: String = "spotify",
    val spotifyGrantId: UUID? = null,
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
