package com.plshare.backend.domain.comment.service

import com.plshare.backend.domain.comment.dto.CommentResponse
import com.plshare.backend.domain.comment.dto.CreateCommentRequest
import com.plshare.backend.domain.comment.model.Comment
import com.plshare.backend.domain.comment.repository.CommentRepository
import com.plshare.backend.domain.post.repository.PostRepository
import com.plshare.backend.domain.user.repository.UserRepository
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import com.plshare.backend.global.response.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 댓글 도메인 서비스.
 *
 * polyenm_pan CommentService 패턴 차용:
 * - soft-delete: comment.deleted = true 세팅 후 저장.
 * - 작성자 검증: UserRepository 직접 조회.
 * - 포스트 존재 검증: PostRepository 직접 조회 (순환 의존 방지 — PostService 미주입).
 *
 * plshare 변경:
 * - answerId(Long) → postId(UUID).
 * - polyenm BaseEntity.markDeleted() → 서비스에서 comment.deleted = true 직접 세팅.
 * - 대댓글(parentId) 미지원(MVP P0 외).
 */
@Service
@Transactional(readOnly = true)
class CommentService(
    private val comments: CommentRepository,
    private val posts: PostRepository,
    private val users: UserRepository,
) {

    @Transactional
    fun create(postId: UUID, authorId: UUID, req: CreateCommentRequest): CommentResponse {
        if (req.text.length > 300) {
            throw ApiException(ErrorCode.VALIDATION_FAILED, "댓글은 300자를 초과할 수 없습니다")
        }
        requirePostExists(postId)
        val author = users.findById(authorId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "작성자를 찾을 수 없습니다: $authorId")
        }
        val saved = comments.save(
            Comment(postId = postId, authorId = authorId, text = req.text)
        )
        return CommentResponse.from(saved, author)
    }

    fun listByPost(postId: UUID, pageable: Pageable): PageResponse<CommentResponse> {
        requirePostExists(postId)
        val page = comments.findAllByPostIdAndDeletedFalseOrderByCreatedAtAsc(postId, pageable)
        val authorIds = page.content.map { it.authorId }.toSet()
        val authorMap = users.findAllById(authorIds).associateBy { it.id }
        return PageResponse.from(page) { comment ->
            val author = authorMap[comment.authorId]
                ?: throw ApiException(ErrorCode.NOT_FOUND, "작성자를 찾을 수 없습니다: ${comment.authorId}")
            CommentResponse.from(comment, author)
        }
    }

    @Transactional
    fun delete(commentId: UUID, requesterId: UUID) {
        val comment = comments.findById(commentId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "댓글을 찾을 수 없습니다: $commentId")
        }
        if (comment.deleted) throw ApiException(ErrorCode.NOT_FOUND, "댓글을 찾을 수 없습니다: $commentId")
        if (comment.authorId != requesterId) {
            throw ApiException(ErrorCode.FORBIDDEN, "본인 댓글만 삭제할 수 있습니다")
        }
        comment.deleted = true
        comments.save(comment)
    }

    private fun requirePostExists(postId: UUID) {
        if (posts.findByIdAndDeletedFalse(postId) == null) {
            throw ApiException(ErrorCode.NOT_FOUND, "포스트를 찾을 수 없습니다: $postId")
        }
    }
}
