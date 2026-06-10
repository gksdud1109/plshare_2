package com.plshare.backend.domain.follow.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * 단방향 팔로우 관계 (Instagram/X 방식).
 *
 * polyenm_pan Follow 패턴 차용 — (followerId, followeeId) unique 제약, countByFollowerId/followeeId.
 * plshare 변경:
 * - polyenm: Long id, Long followerId/followeeId, Instant createdAt (AuditingEntityListener)
 * - plshare: UUID id, UUID followerId/followeeId, LocalDateTime createdAt (수동) — UUID id 컨벤션 일관 적용.
 *
 * 불변식:
 * - followerId == followeeId 는 서비스에서 VALIDATION_FAILED로 거부 (자기 팔로우 금지).
 * - (followerId, followeeId) 쌍은 DB 유니크 제약으로 중복 보장.
 */
@Entity
@Table(
    name = "follows",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_follows_follower_followee", columnNames = ["follower_id", "followee_id"]),
    ],
    indexes = [
        Index(name = "idx_follows_follower_id", columnList = "follower_id"),
        Index(name = "idx_follows_followee_id", columnList = "followee_id"),
    ]
)
class Follow(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "follower_id", nullable = false, updatable = false)
    val followerId: UUID,

    @Column(name = "followee_id", nullable = false, updatable = false)
    val followeeId: UUID,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
