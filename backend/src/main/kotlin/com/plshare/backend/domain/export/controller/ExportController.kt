package com.plshare.backend.domain.export.controller

import com.plshare.backend.domain.export.dto.CreateExportRequest
import com.plshare.backend.domain.export.dto.ExportCandidateDto
import com.plshare.backend.domain.export.dto.ExportJobDto
import com.plshare.backend.domain.export.dto.ExportMappingDto
import com.plshare.backend.domain.export.dto.ExportResultDto
import com.plshare.backend.domain.export.dto.OverrideMatchRequest
import com.plshare.backend.domain.export.repository.ExportJobRepository
import com.plshare.backend.domain.export.repository.ExportTrackMatchRepository
import com.plshare.backend.domain.export.service.ExportService
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import com.plshare.backend.global.response.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.UUID
import com.plshare.backend.global.security.ApplicationPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal

@RestController
@RequestMapping("/api")
class ExportController(
    private val exportService: ExportService,
    private val exportJobRepository: ExportJobRepository,
    private val exportTrackMatchRepository: ExportTrackMatchRepository,
) {
    @PostMapping("/exports")
    fun createExport(
        @RequestHeader("X-Idempotency-Key") idempotencyKey: String,
        @RequestBody body: CreateExportRequest,
        @AuthenticationPrincipal principal: ApplicationPrincipal?,
    ): ResponseEntity<ApiResponse<ExportJobDto>> {
        val jobId = exportService.requestExport(
            idempotencyKey,
            body.assetId,
            body.targetPlatform,
            principal?.userId,
        )
        val job = exportJobRepository.findById(jobId).orElseThrow {
            ApiException(ErrorCode.INTERNAL, "Created export job not found: $jobId")
        }
        return ResponseEntity.accepted().body(ApiResponse.ok(ExportJobDto.from(job)))
    }

    @GetMapping("/exports/{jobId}")
    @Transactional(readOnly = true)
    fun getExport(
        @PathVariable jobId: UUID,
        @AuthenticationPrincipal principal: ApplicationPrincipal?,
    ): ApiResponse<ExportJobDto> {
        val job = exportJobRepository.findById(jobId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "Export job not found: $jobId")
        }
        requireOwner(job.ownerId, principal)
        val matches = exportTrackMatchRepository.findByExportJobId(jobId)
        return ApiResponse.ok(ExportJobDto.from(job, matches))
    }

    @GetMapping("/exports/{jobId}/result")
    @Transactional(readOnly = true)
    fun getExportResult(
        @PathVariable jobId: UUID,
        @AuthenticationPrincipal principal: ApplicationPrincipal?,
    ): ApiResponse<ExportResultDto> {
        val job = exportJobRepository.findById(jobId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "Export job not found: $jobId")
        }
        requireOwner(job.ownerId, principal)
        return ApiResponse.ok(ExportResultDto(
            jobId = job.id,
            status = job.status.name.lowercase(),
            externalPlaylistId = job.externalPlaylistId,
            externalUrl = job.externalUrl,
            matchedTracks = job.matchedTracks,
            failedTracks = job.failedTracks
        ))
    }

    /** Alternative video candidates for reviewing a low-confidence/failed track match. */
    @GetMapping("/exports/{jobId}/matches/{trackId}/candidates")
    fun getMatchCandidates(
        @PathVariable jobId: UUID,
        @PathVariable trackId: UUID,
        @AuthenticationPrincipal principal: ApplicationPrincipal?,
    ): ApiResponse<List<ExportCandidateDto>> {
        val candidates = exportService.getCandidates(jobId, trackId, principal?.userId)
        return ApiResponse.ok(candidates.map { ExportCandidateDto.from(it) })
    }

    /** Confirm a user-chosen video for a track — adds it to the playlist and marks the match reviewed. */
    @PostMapping("/exports/{jobId}/matches/{trackId}")
    fun overrideMatch(
        @PathVariable jobId: UUID,
        @PathVariable trackId: UUID,
        @RequestBody body: OverrideMatchRequest,
        @AuthenticationPrincipal principal: ApplicationPrincipal?,
    ): ApiResponse<ExportMappingDto> {
        if (body.videoId.isBlank()) {
            throw ApiException(ErrorCode.VALIDATION_FAILED, "videoId is required")
        }
        val match = exportService.overrideMatch(jobId, trackId, body.videoId, body.title, principal?.userId)
        return ApiResponse.ok(ExportMappingDto.from(match))
    }

    private fun requireOwner(ownerId: UUID?, principal: ApplicationPrincipal?) {
        if (principal != null && ownerId != principal.userId) {
            throw ApiException(ErrorCode.FORBIDDEN, "Export job does not belong to the current user")
        }
    }
}
