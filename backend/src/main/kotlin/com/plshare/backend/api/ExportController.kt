package com.plshare.backend.api

import com.plshare.backend.api.dto.CreateExportRequest
import com.plshare.backend.api.dto.ExportJobDto
import com.plshare.backend.api.dto.ExportResultDto
import com.plshare.backend.application.service.ExportService
import com.plshare.backend.domain.repository.ExportJobRepository
import org.springframework.http.ResponseEntity
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
        val job = exportJobRepository.findById(jobId).orElseThrow()
        return ResponseEntity.accepted().body(ExportJobDto.from(job))
    }

    @GetMapping("/exports/{jobId}")
    fun getExport(@PathVariable jobId: UUID): ExportJobDto {
        val job = exportJobRepository.findById(jobId).orElseThrow {
            NoSuchElementException("Export job not found: $jobId")
        }
        return ExportJobDto.from(job)
    }

    @GetMapping("/exports/{jobId}/result")
    fun getExportResult(@PathVariable jobId: UUID): ExportResultDto {
        val job = exportJobRepository.findById(jobId).orElseThrow {
            NoSuchElementException("Export job not found: $jobId")
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
