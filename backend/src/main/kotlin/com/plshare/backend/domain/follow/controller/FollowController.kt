package com.plshare.backend.domain.follow.controller

import com.plshare.backend.domain.follow.dto.FollowStatsResponse
import com.plshare.backend.domain.follow.service.FollowService
import com.plshare.backend.global.response.ApiResponse
import org.springframework.web.bind.annotation.*
import java.util.UUID
import com.plshare.backend.global.security.ApplicationPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal

/**
 * 팔로우 REST 컨트롤러.
 *
 * NOTE (pre-session integration): followerId는 세션/JWT 통합 전 임시 파라미터.
 * POST   /api/users/{handle}/follow?followerId=    — 팔로우
 * DELETE /api/users/{handle}/follow?followerId=    — 언팔로우
 * GET    /api/users/{handle}/follow-stats?viewerId= — 팔로우 통계
 *
 * User 엔티티 수정 금지 — handle 경로로 대상 유저를 조회하고 UUID만 사용.
 */
@RestController
@RequestMapping("/api/users/{handle}")
class FollowController(private val followService: FollowService) {

    @PostMapping("/follow")
    fun follow(
        @PathVariable handle: String,
        @RequestParam(required = false) followerId: UUID?,
        @AuthenticationPrincipal principal: ApplicationPrincipal?,
    ): ApiResponse<Unit> {
        followService.follow(principal?.userId ?: requireNotNull(followerId), handle)
        return ApiResponse.ok(null)
    }

    @DeleteMapping("/follow")
    fun unfollow(
        @PathVariable handle: String,
        @RequestParam(required = false) followerId: UUID?,
        @AuthenticationPrincipal principal: ApplicationPrincipal?,
    ): ApiResponse<Unit> {
        followService.unfollow(principal?.userId ?: requireNotNull(followerId), handle)
        return ApiResponse.ok(null)
    }

    @GetMapping("/follow-stats")
    fun stats(
        @PathVariable handle: String,
        @RequestParam(required = false) viewerId: UUID?,
        @AuthenticationPrincipal principal: ApplicationPrincipal?,
    ): ApiResponse<FollowStatsResponse> =
        ApiResponse.ok(followService.stats(handle, principal?.userId ?: viewerId))
}
