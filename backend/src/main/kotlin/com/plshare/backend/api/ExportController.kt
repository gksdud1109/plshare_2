package com.plshare.backend.api

import com.plshare.backend.api.dto.CreateExportRequest
import com.plshare.backend.api.dto.ExportJobDto
import com.plshare.backend.api.dto.ExportResultDto
import com.plshare.backend.application.service.ExportService
import com.plshare.backend.domain.repository.ExportJobRepository
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api")
class ExportController(
    private val exportService: ExportService,
    private val exportJobRepository: ExportJobRepository
) {
    @PostMapping("/exports")
    fun createExport(
        @RequestHeader("X-Idempotency-Key") idempotencyKey: String,
        @RequestBody body: CreateExportRequest
    ): ResponseEntity<ExportJobDto> {
        val jobId = exportService.requestExport(idempotencyKey, body.assetId, body.targetPlatform)
        val job = exportJobRepository.findById(jobId).orElseThrow {
            ApiException(ErrorCode.INTERNAL, "Created export job not found: $jobId")
        }
        return ResponseEntity.accepted().body(ExportJobDto.from(job))
    }

    @GetMapping("/exports/{jobId}")
    @Transactional(readOnly = true)
    fun getExport(@PathVariable jobId: UUID): ExportJobDto {
        val job = exportJobRepository.findById(jobId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "Export job not found: $jobId")
        }
        return ExportJobDto.from(job)
    }

    @GetMapping("/exports/{jobId}/result")
    @Transactional(readOnly = true)
    fun getExportResult(@PathVariable jobId: UUID): ExportResultDto {
        val job = exportJobRepository.findById(jobId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "Export job not found: $jobId")
        }
        return ExportResultDto(
            jobId = job.id,
            status = job.status.name.lowercase(),
            externalPlaylistId = job.externalPlaylistId,
            externalUrl = job.externalUrl,
            matchedTracks = job.matchedTracks,
            failedTracks = job.failedTracks
        )
    }
}
