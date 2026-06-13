package com.plshare.backend.domain.gift.controller

import com.plshare.backend.domain.gift.dto.*
import com.plshare.backend.domain.gift.service.GiftService
import com.plshare.backend.global.response.ApiResponse
import org.springframework.web.bind.annotation.*

/**
 * 감성 선물 API.
 *
 * - POST /api/gifts          : 선물 생성 → {token, url}
 * - GET  /api/gifts/{token}  : 공개 선물 뷰 (인증 불필요)
 * - POST /api/gifts/{token}/open : 언박싱 진입 (status OPENED + openedAt, 멱등)
 * - POST /api/gifts/{token}/save : 라이브러리 저장 (status SAVED + savedByUserId)
 */
@RestController
@RequestMapping("/api/gifts")
class GiftController(
    private val giftService: GiftService,
) {

    @PostMapping
    fun create(@RequestBody req: CreateGiftRequest): ApiResponse<GiftCreatedResponse> =
        ApiResponse.ok(giftService.create(req))

    @GetMapping("/{token}")
    fun view(@PathVariable token: String): ApiResponse<GiftViewResponse> =
        ApiResponse.ok(giftService.view(token))

    @PostMapping("/{token}/open")
    fun open(@PathVariable token: String): ApiResponse<GiftViewResponse> =
        ApiResponse.ok(giftService.open(token))

    @PostMapping("/{token}/save")
    fun save(
        @PathVariable token: String,
        @RequestBody req: SaveGiftRequest,
    ): ApiResponse<GiftViewResponse> =
        ApiResponse.ok(giftService.save(token, req))
}
