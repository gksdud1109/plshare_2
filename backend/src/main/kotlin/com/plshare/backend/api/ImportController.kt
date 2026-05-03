package com.plshare.backend.api

import com.plshare.backend.api.dto.CreateImportRequest
import com.plshare.backend.api.dto.ImportJobDto
import com.plshare.backend.api.dto.PlaylistSummaryDto
import com.plshare.backend.application.service.ImportService
import com.plshare.backend.domain.repository.ImportJobRepository
import com.plshare.backend.infrastructure.spotify.SpotifyClient
import org.springframework.http.ResponseEntity
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
    fun listPlaylists(): List<PlaylistSummaryDto> {
        val token = spotifyClient.getAccessToken().block() ?: error("Failed to get token")
        val playlists = spotifyClient.listUserPlaylists(token).block() ?: emptyList()
        return playlists.map { PlaylistSummaryDto.from(it) }
    }

    @PostMapping("/imports")
    fun createImport(
        @RequestHeader("X-Idempotency-Key") idempotencyKey: String,
        @RequestBody body: CreateImportRequest
    ): ResponseEntity<ImportJobDto> {
        val jobId = importService.requestImport(idempotencyKey, body.playlistId)
        val job = importJobRepository.findById(jobId).orElseThrow()
        return ResponseEntity.accepted().body(ImportJobDto.from(job))
    }

    @GetMapping("/imports/{jobId}")
    fun getImport(@PathVariable jobId: UUID): ImportJobDto {
        val job = importJobRepository.findById(jobId).orElseThrow {
            NoSuchElementException("Import job not found: $jobId")
        }
        return ImportJobDto.from(job)
    }
}
