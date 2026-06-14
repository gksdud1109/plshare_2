package com.plshare.backend.domain.gift.controller

import com.plshare.backend.domain.gift.dto.*
import com.plshare.backend.domain.gift.service.GiftService
import com.plshare.backend.global.response.ApiResponse
import com.plshare.backend.global.security.CurrentUserId
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * 감성 선물 API. 발신자/수신자 신원은 [CurrentUserId](세션 principal)로만 해석한다.
 *
 * - POST /api/gifts              : 선물 생성 → {token, url}  (인증 필수)
 * - GET  /api/gifts/{token}      : 공개 선물 뷰 (인증 불필요)
 * - POST /api/gifts/{token}/open : 언박싱 진입 (status OPENED + openedAt, 멱등, 익명 허용)
 * - POST /api/gifts/{token}/save : 라이브러리 저장 (status SAVED + savedByUserId, 인증 필수, 멱등)
 */
@RestController
@RequestMapping("/api/gifts")
class GiftController(
    private val giftService: GiftService,
) {

    @PostMapping
    fun create(
        @RequestBody req: CreateGiftRequest,
        @CurrentUserId senderId: UUID,
    ): ApiResponse<GiftCreatedResponse> =
        ApiResponse.ok(giftService.create(req, senderId, requireOwnedAsset = true))

    /** 받은(저장한) 선물 목록 — 인증 필수. (literal 경로라 /{token} 보다 우선 매칭) */
    @GetMapping("/received")
    fun received(@CurrentUserId userId: UUID): ApiResponse<List<GiftSummaryResponse>> =
        ApiResponse.ok(giftService.received(userId))

    /** 보낸 선물 목록 — 인증 필수. */
    @GetMapping("/sent")
    fun sent(@CurrentUserId userId: UUID): ApiResponse<List<GiftSummaryResponse>> =
        ApiResponse.ok(giftService.sent(userId))

    @GetMapping("/{token}")
    fun view(@PathVariable token: String): ApiResponse<GiftViewResponse> =
        ApiResponse.ok(giftService.view(token))

    @PostMapping("/{token}/open")
    fun open(@PathVariable token: String): ApiResponse<GiftViewResponse> =
        ApiResponse.ok(giftService.open(token))

    @PostMapping("/{token}/save")
    fun save(
        @PathVariable token: String,
        @CurrentUserId userId: UUID,
    ): ApiResponse<GiftViewResponse> =
        ApiResponse.ok(giftService.save(token, userId))
}
