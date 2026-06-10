package com.plshare.backend.domain.reaction.controller

import com.plshare.backend.domain.reaction.service.PostLikeService
import com.plshare.backend.global.response.ApiResponse
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * 포스트 좋아요 REST 컨트롤러.
 *
 * NOTE (pre-session integration): userId는 세션/JWT 통합 전 임시 파라미터.
 * POST /api/posts/{id}/like?userId=  — 좋아요 추가 (멱등)
 * DELETE /api/posts/{id}/like?userId= — 좋아요 취소 (멱등)
 */
@RestController
@RequestMapping("/api/posts/{id}/like")
class PostLikeController(private val postLikeService: PostLikeService) {

    @PostMapping
    fun like(
        @PathVariable id: UUID,
        @RequestParam userId: UUID,
    ): ApiResponse<Long> =
        ApiResponse.ok(postLikeService.like(id, userId))

    @DeleteMapping
    fun unlike(
        @PathVariable id: UUID,
        @RequestParam userId: UUID,
    ): ApiResponse<Long> =
        ApiResponse.ok(postLikeService.unlike(id, userId))
}
