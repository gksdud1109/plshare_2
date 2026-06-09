package com.plshare.backend.domain.importing.controller

import com.plshare.backend.domain.importing.dto.CreateImportRequest
import com.plshare.backend.domain.importing.dto.ImportJobDto
import com.plshare.backend.domain.importing.dto.PlaylistSummaryDto
import com.plshare.backend.domain.importing.repository.ImportJobRepository
import com.plshare.backend.domain.importing.service.ImportService
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import com.plshare.backend.global.response.ApiResponse
import com.plshare.backend.infrastructure.spotify.SpotifyClient
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api")
class ImportController(
    private val spotifyClient: SpotifyClient,
    private val importService: ImportService,
    private val importJobRepository: ImportJobRepository
) {
    @GetMapping("/playlists")
    fun listPlaylists(): ApiResponse<List<PlaylistSummaryDto>> {
        val token = spotifyClient.getAccessToken().block()
            ?: throw ApiException(ErrorCode.UPSTREAM_ERROR, "Spotify 토큰 발급 실패")
        val playlists = spotifyClient.listUserPlaylists(token).block() ?: emptyList()
        return ApiResponse.ok(playlists.map { PlaylistSummaryDto.from(it) })
    }

    @PostMapping("/imports")
    fun createImport(
        @RequestHeader("X-Idempotency-Key") idempotencyKey: String,
        @RequestBody body: CreateImportRequest
    ): ResponseEntity<ApiResponse<ImportJobDto>> {
        val jobId = importService.requestImport(idempotencyKey, body.playlistId)
        val job = importJobRepository.findById(jobId).orElseThrow {
            ApiException(ErrorCode.INTERNAL, "Created import job not found: $jobId")
        }
        return ResponseEntity.accepted().body(ApiResponse.ok(ImportJobDto.from(job)))
    }

    @GetMapping("/imports/{jobId}")
    @Transactional(readOnly = true)
    fun getImport(@PathVariable jobId: UUID): ApiResponse<ImportJobDto> {
        val job = importJobRepository.findById(jobId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "Import job not found: $jobId")
        }
        return ApiResponse.ok(ImportJobDto.from(job))
    }
}
