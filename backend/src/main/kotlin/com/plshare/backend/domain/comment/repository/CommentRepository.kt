package com.plshare.backend.domain.comment.repository

import com.plshare.backend.domain.comment.model.Comment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * Comment 저장소.
 *
 * polyenm_pan CommentRepository 패턴 차용 — findAllByXxxAndDeletedFalse 네이밍, countByXxx.
 * plshare 변경: answerId(Long) → postId(UUID), 대댓글(parentId) 미지원(MVP P0 외).
 */
interface CommentRepository : JpaRepository<Comment, UUID> {
    /** 포스트의 댓글 목록 (soft-deleted 제외, 최신순). */
    fun findAllByPostIdAndDeletedFalseOrderByCreatedAtAsc(postId: UUID, pageable: Pageable): Page<Comment>

    /** 포스트의 비삭제 댓글 수. */
    fun countByPostIdAndDeletedFalse(postId: UUID): Long
}
