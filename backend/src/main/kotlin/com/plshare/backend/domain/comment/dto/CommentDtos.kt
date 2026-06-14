package com.plshare.backend.domain.comment.dto

import com.plshare.backend.domain.comment.model.Comment
import com.plshare.backend.domain.user.model.User
import java.time.LocalDateTime
import java.util.UUID

/** 댓글 생성 요청 DTO. 작성자는 @CurrentUserId(세션 principal)로 해석한다. */
data class CreateCommentRequest(
    val text: String,
)

/** 댓글 응답 DTO. 작성자 정보를 포함한다. */
data class CommentResponse(
    val id: UUID,
    val postId: UUID,
    val authorId: UUID,
    val authorHandle: String,
    val authorDisplayName: String,
    val authorAvatarUrl: String?,
    val text: String,
    val deleted: Boolean,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(comment: Comment, author: User): CommentResponse = CommentResponse(
            id = comment.id,
            postId = comment.postId,
            authorId = comment.authorId,
            authorHandle = author.handle,
            authorDisplayName = author.displayName,
            authorAvatarUrl = author.avatarUrl,
            text = if (comment.deleted) "삭제된 댓글입니다" else comment.text,
            deleted = comment.deleted,
            createdAt = comment.createdAt,
        )
    }
}
