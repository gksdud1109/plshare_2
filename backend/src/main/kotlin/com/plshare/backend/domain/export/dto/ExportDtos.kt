package com.plshare.backend.domain.export.dto

import com.plshare.backend.domain.export.model.ExportJob
import com.plshare.backend.domain.export.model.ExportTrackMatch
import com.plshare.backend.infrastructure.youtube.YouTubeSearchCandidate
import java.util.UUID

data class CreateExportRequest(
    val assetId: UUID,
    val targetPlatform: String = "youtube"
)

/** Per-track export match, surfaced for manual review. status ∈ matched|alternative|failed. */
data class ExportMappingDto(
    val trackId: UUID,
    val title: String,
    val artist: String,
    val videoId: String?,
    val matchedTitle: String?,
    val confidence: Double?,
    val status: String,
    val reviewed: Boolean,
) {
    companion object {
        fun from(m: ExportTrackMatch) = ExportMappingDto(
            trackId = m.trackId,
            title = m.title,
            artist = m.artist,
            videoId = m.matchedVideoId,
            matchedTitle = m.matchedTitle,
            confidence = m.confidence,
            status = m.status.name.lowercase(),
            reviewed = m.reviewed,
        )
    }
}

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
    val lastError: String?,
    val mappings: List<ExportMappingDto>,
) {
    companion object {
        fun from(j: ExportJob, matches: List<ExportTrackMatch> = emptyList()) = ExportJobDto(
            jobId = j.id,
            assetId = j.assetId,
            targetPlatform = j.targetPlatform,
            status = j.status.name.lowercase(),
            totalTracks = j.totalTracks,
            matchedTracks = j.matchedTracks,
            failedTracks = j.failedTracks,
            externalPlaylistId = j.externalPlaylistId,
            externalUrl = j.externalUrl,
            lastError = j.lastError,
            mappings = matches.map { ExportMappingDto.from(it) },
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

/** Candidate video offered to the user when reviewing a low-confidence/failed match. */
data class ExportCandidateDto(
    val videoId: String,
    val title: String,
    val channelTitle: String?,
) {
    companion object {
        fun from(c: YouTubeSearchCandidate) = ExportCandidateDto(
            videoId = c.videoId,
            title = c.title,
            channelTitle = c.channelTitle,
        )
    }
}

/** Body for POST /api/exports/{jobId}/matches/{trackId} — confirm a chosen video. */
data class OverrideMatchRequest(
    val videoId: String,
    val title: String? = null,
)
