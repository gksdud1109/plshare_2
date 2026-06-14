package com.plshare.backend.domain.reaction.controller

import com.plshare.backend.domain.reaction.service.PostLikeService
import com.plshare.backend.global.response.ApiResponse
import com.plshare.backend.global.security.CurrentUserId
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * 포스트 좋아요 REST 컨트롤러. 신원은 [CurrentUserId](세션 principal)로 해석한다.
 * POST   /api/posts/{id}/like — 좋아요 추가 (멱등)
 * DELETE /api/posts/{id}/like — 좋아요 취소 (멱등)
 */
@RestController
@RequestMapping("/api/posts/{id}/like")
class PostLikeController(private val postLikeService: PostLikeService) {

    @PostMapping
    fun like(
        @PathVariable id: UUID,
        @CurrentUserId userId: UUID,
    ): ApiResponse<Long> =
        ApiResponse.ok(postLikeService.like(id, userId))

    @DeleteMapping
    fun unlike(
        @PathVariable id: UUID,
        @CurrentUserId userId: UUID,
    ): ApiResponse<Long> =
        ApiResponse.ok(postLikeService.unlike(id, userId))
}
