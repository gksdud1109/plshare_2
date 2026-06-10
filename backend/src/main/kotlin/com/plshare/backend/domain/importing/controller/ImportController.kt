package com.plshare.backend.domain.importing.controller

import com.plshare.backend.domain.importing.dto.CreateImportRequest
import com.plshare.backend.domain.importing.dto.ImportJobDto
import com.plshare.backend.domain.importing.dto.PlaylistSummaryDto
import com.plshare.backend.domain.importing.dto.YouTubePlaylistSummaryDto
import com.plshare.backend.domain.importing.repository.ImportJobRepository
import com.plshare.backend.domain.importing.service.ImportService
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import com.plshare.backend.global.response.ApiResponse
import com.plshare.backend.infrastructure.spotify.SpotifyClient
import com.plshare.backend.infrastructure.youtube.YouTubeClient
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api")
class ImportController(
    private val spotifyClient: SpotifyClient,
    private val youTubeClient: YouTubeClient,
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

    /**
     * YouTube 플레이리스트 목록 조회.
     * demo 환경: MockYouTubeClient fixture 반환. prod: Bearer 토큰 필요.
     *
     * GET /api/youtube/playlists
     */
    @GetMapping("/youtube/playlists")
    fun listYouTubePlaylists(): ApiResponse<List<YouTubePlaylistSummaryDto>> {
        // demo에서는 mock이 토큰 무검증. prod에서는 GoogleAuthService에서 토큰 조달 필요.
        val token = "demo-youtube-token"
        val playlists = youTubeClient.listUserPlaylists(token).block()
            ?: throw ApiException(ErrorCode.UPSTREAM_ERROR, "YouTube 플레이리스트 조회 실패")
        return ApiResponse.ok(playlists.map { YouTubePlaylistSummaryDto.from(it) })
    }

    /**
     * Import 요청. sourcePlatform 파라미터로 Spotify/YouTube 분기.
     *
     * 기존 FE/E2E 요청(playlistId만 보냄)과 하위호환:
     *   { "playlistId": "..." }          → sourcePlatform 기본값 "spotify"
     *   { "playlistId": "...", "sourcePlatform": "youtube" } → YouTube import
     */
    @PostMapping("/imports")
    fun createImport(
        @RequestHeader("X-Idempotency-Key") idempotencyKey: String,
        @RequestBody body: CreateImportRequest
    ): ResponseEntity<ApiResponse<ImportJobDto>> {
        val jobId = importService.requestImport(
            idempotencyKey = idempotencyKey,
            playlistId = body.playlistId,
            sourcePlatform = body.sourcePlatform
        )
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
