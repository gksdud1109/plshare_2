package com.plshare.backend.domain.export.dto

import com.plshare.backend.domain.export.model.ExportJob
import java.util.UUID

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
