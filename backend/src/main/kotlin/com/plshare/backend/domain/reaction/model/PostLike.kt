package com.plshare.backend.domain.reaction.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * 포스트 좋아요 연결 테이블.
 *
 * polyenm_pan Reaction 패턴 차용 — target-type 일반화 없이 post 전용 단순 테이블로 단순화.
 * plshare 변경:
 * - polyenm: Long IDENTITY id, Instant createdAt (AuditingEntityListener)
 * - plshare:  UUID id, LocalDateTime createdAt (수동) — BaseEntity 미차용 일관 적용.
 * - polyenm의 DuplicateSafeInserter 패턴은 현재 미도입; DB unique 제약이 경쟁 조건 최종 판단.
 *
 * 불변식: (postId, userId) 쌍은 DB 유니크 제약으로 중복 반응을 원천 차단한다.
 * 서비스는 멱등하게 처리한다 — 이미 존재하면 no-op, 중복 DataIntegrityViolationException은 catch.
 */
@Entity
@Table(
    name = "post_likes",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_post_likes_post_user", columnNames = ["post_id", "user_id"]),
    ],
    indexes = [
        Index(name = "idx_post_likes_post_id", columnList = "post_id"),
        Index(name = "idx_post_likes_user_id", columnList = "user_id"),
    ]
)
class PostLike(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "post_id", nullable = false, updatable = false)
    val postId: UUID,

    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
