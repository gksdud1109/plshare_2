package com.plshare.backend.domain.comment.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * 포스트에 달리는 댓글.
 *
 * polyenm_pan Comment 패턴 차용 — soft-delete(deleted 플래그), postId/authorId 참조, text 길이 제한.
 * plshare 변경:
 * - polyenm: answerId(Long), BaseEntity 상속(Long IDENTITY id, markDeleted())
 * - plshare: postId(UUID), Long 대신 UUID id, 수동 createdAt, deleted 필드 직접 노출.
 * - 대댓글(parentId) 미지원 — MVP P0 범위 외.
 *
 * 불변식:
 * - [text]는 300자 이하 (CommentService에서 VALIDATION_FAILED 강제).
 * - [deleted] = true 이면 목록에서 제외(소프트 삭제). "삭제된 댓글입니다" 표시 가능하도록 row 유지.
 */
@Entity
@Table(
    name = "comments",
    indexes = [
        Index(name = "idx_comments_post_id", columnList = "post_id"),
        Index(name = "idx_comments_author_id", columnList = "author_id"),
    ]
)
class Comment(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "post_id", nullable = false, updatable = false)
    val postId: UUID,

    @Column(name = "author_id", nullable = false, updatable = false)
    val authorId: UUID,

    @Column(nullable = false, length = 300)
    var text: String,

    /**
     * 소프트 삭제 플래그.
     * true이면 목록 쿼리에서 제외하나, row를 유지해 "삭제된 댓글입니다" 렌더링을 지원한다.
     * polyenm_pan CommentService의 soft-delete 흐름 차용.
     */
    @Column(nullable = false)
    var deleted: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
