package com.plshare.backend.domain.export.controller

import com.plshare.backend.domain.export.dto.CreateExportRequest
import com.plshare.backend.domain.export.dto.ExportJobDto
import com.plshare.backend.domain.export.dto.ExportResultDto
import com.plshare.backend.domain.export.repository.ExportJobRepository
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
    private val exportJobRepository: ExportJobRepository
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
        return ApiResponse.ok(ExportJobDto.from(job))
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

    private fun requireOwner(ownerId: UUID?, principal: ApplicationPrincipal?) {
        if (principal != null && ownerId != principal.userId) {
            throw ApiException(ErrorCode.FORBIDDEN, "Export job does not belong to the current user")
        }
    }
}
