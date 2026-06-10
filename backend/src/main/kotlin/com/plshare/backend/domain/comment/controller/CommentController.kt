package com.plshare.backend.domain.comment.controller

import com.plshare.backend.domain.comment.dto.CommentResponse
import com.plshare.backend.domain.comment.dto.CreateCommentRequest
import com.plshare.backend.domain.comment.service.CommentService
import com.plshare.backend.global.response.ApiResponse
import com.plshare.backend.global.response.PageResponse
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * 댓글 REST 컨트롤러.
 *
 * NOTE (pre-session integration): authorId/requesterId는 세션/JWT 통합 전 임시 파라미터.
 * POST  /api/posts/{id}/comments              — 댓글 작성
 * GET   /api/posts/{id}/comments?page=&size=  — 댓글 목록 (PageResponse)
 * DELETE /api/comments/{id}?requesterId=       — 댓글 삭제 (작성자만, soft-delete)
 */
@RestController
class CommentController(private val commentService: CommentService) {

    @PostMapping("/api/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable postId: UUID,
        @RequestBody req: CreateCommentRequest,
    ): ApiResponse<CommentResponse> =
        ApiResponse.ok(commentService.create(postId, req))

    @GetMapping("/api/posts/{postId}/comments")
    fun listByPost(
        @PathVariable postId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<CommentResponse>> =
        ApiResponse.ok(commentService.listByPost(postId, PageRequest.of(page, size)))

    @DeleteMapping("/api/comments/{commentId}")
    fun delete(
        @PathVariable commentId: UUID,
        @RequestParam requesterId: UUID,
    ): ApiResponse<Unit> {
        commentService.delete(commentId, requesterId)
        return ApiResponse.ok(null)
    }
}
