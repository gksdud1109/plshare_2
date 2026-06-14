package com.plshare.backend.domain.follow.controller

import com.plshare.backend.domain.follow.dto.FollowStatsResponse
import com.plshare.backend.domain.follow.service.FollowService
import com.plshare.backend.global.response.ApiResponse
import com.plshare.backend.global.security.CurrentUserId
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * 팔로우 REST 컨트롤러. 신원은 [CurrentUserId](세션 principal)로 해석한다.
 * POST   /api/users/{handle}/follow        — 팔로우
 * DELETE /api/users/{handle}/follow        — 언팔로우
 * GET    /api/users/{handle}/follow-stats  — 팔로우 통계 (viewer 선택적)
 *
 * User 엔티티 수정 금지 — handle 경로로 대상 유저를 조회하고 UUID만 사용.
 */
@RestController
@RequestMapping("/api/users/{handle}")
class FollowController(private val followService: FollowService) {

    @PostMapping("/follow")
    fun follow(
        @PathVariable handle: String,
        @CurrentUserId followerId: UUID,
    ): ApiResponse<Unit> {
        followService.follow(followerId, handle)
        return ApiResponse.ok(null)
    }

    @DeleteMapping("/follow")
    fun unfollow(
        @PathVariable handle: String,
        @CurrentUserId followerId: UUID,
    ): ApiResponse<Unit> {
        followService.unfollow(followerId, handle)
        return ApiResponse.ok(null)
    }

    @GetMapping("/follow-stats")
    fun stats(
        @PathVariable handle: String,
        @CurrentUserId viewerId: UUID?,
    ): ApiResponse<FollowStatsResponse> =
        ApiResponse.ok(followService.stats(handle, viewerId))
}
