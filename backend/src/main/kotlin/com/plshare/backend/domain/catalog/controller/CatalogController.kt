package com.plshare.backend.domain.catalog.controller

import com.plshare.backend.domain.catalog.dto.ComposeAssetRequest
import com.plshare.backend.domain.catalog.dto.ComposedAssetResponse
import com.plshare.backend.domain.catalog.dto.CreateMoodVideoRequest
import com.plshare.backend.domain.catalog.dto.CuratedTrackDto
import com.plshare.backend.domain.catalog.dto.YouTubeCatalogSearchResultDto
import com.plshare.backend.domain.catalog.service.CatalogService
import com.plshare.backend.domain.catalog.service.YouTubeCatalogSearchService
import com.plshare.backend.global.response.ApiResponse
import com.plshare.backend.global.security.CurrentUserId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * 큐레이션 카탈로그 — 둘러보기(공개) + 선택 트랙으로 플레이리스트 조립(인증).
 */
@RestController
@RequestMapping("/api")
class CatalogController(
    private val catalogService: CatalogService,
    private val youTubeCatalogSearchService: YouTubeCatalogSearchService,
) {

    /** 카탈로그 조회 — 공개. ?mood= 로 무드 필터. */
    @GetMapping("/catalog/tracks")
    fun tracks(@RequestParam(required = false) mood: String?): ApiResponse<List<CuratedTrackDto>> =
        ApiResponse.ok(catalogService.listTracks(mood))

    /** API-key tier YouTube music-video search — application authentication required, YouTube OAuth not required. */
    @GetMapping("/catalog/youtube/search")
    fun searchYouTube(
        @RequestParam("q") query: String,
        @Suppress("UNUSED_PARAMETER") @CurrentUserId ownerId: UUID,
    ): ApiResponse<List<YouTubeCatalogSearchResultDto>> =
        ApiResponse.ok(youTubeCatalogSearchService.search(query))

    /** 고른 트랙으로 내 플레이리스트(Asset) 조립 — 인증 필수. 생성된 자산 id 반환. */
    @PostMapping("/assets/compose")
    fun compose(
        @RequestBody req: ComposeAssetRequest,
        @RequestHeader(name = "X-Idempotency-Key") idempotencyKey: String,
        @CurrentUserId ownerId: UUID,
    ): ApiResponse<ComposedAssetResponse> =
        ApiResponse.ok(catalogService.compose(req, ownerId, idempotencyKey))

    /** 단일 유튜브 무드영상을 한 단위 자산(MOOD_VIDEO)으로 — 인증 필수. */
    @PostMapping("/assets/mood-video")
    fun composeMoodVideo(
        @RequestBody req: CreateMoodVideoRequest,
        @RequestHeader(name = "X-Idempotency-Key") idempotencyKey: String,
        @CurrentUserId ownerId: UUID,
    ): ApiResponse<ComposedAssetResponse> =
        ApiResponse.ok(catalogService.composeMoodVideo(req, ownerId, idempotencyKey))
}
