package com.plshare.backend.domain.post.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * 플레이리스트/트랙 카드 공유 포스트 엔티티.
 *
 * 불변식:
 * - [text]는 500자 이하 (서비스 계층에서 VALIDATION_FAILED로 강제).
 * - [authorId]는 반드시 존재하는 users.id 참조 (서비스에서 NOT_FOUND 보장).
 * - [assetId], [trackId]는 첨부된 자산/트랙 UUID (nullable — 텍스트 전용 포스트 허용).
 * - [deleted] = true 이면 피드에서 제외된다(soft-delete). polyenm_pan comment/follow soft-delete 패턴 차용.
 *   hard-delete 대신 soft-delete를 쓰는 이유: 팔로잉 피드 집계, 좋아요·댓글 참조 정합성 유지.
 *
 * 컨벤션: id는 UUID(수동), createdAt은 LocalDateTime.now() 수동 — BaseEntity(Long IDENTITY) 미차용.
 */
@Entity
@Table(
    name = "posts",
    indexes = [
        Index(name = "idx_posts_author_created", columnList = "author_id, created_at"),
        Index(name = "idx_posts_created_at", columnList = "created_at"),
        Index(name = "idx_posts_deleted", columnList = "deleted"),
    ]
)
class Post(
    @Id
    val id: UUID = UUID.randomUUID(),

    /** 작성자 users.id 참조. FK 없이 UUID만 보관 — 도메인 경계 분리. */
    @Column(name = "author_id", nullable = false, updatable = false)
    val authorId: UUID,

    /** 포스트 본문. 500자 이하 불변식은 PostService에서 VALIDATION_FAILED로 강제. */
    @Column(nullable = false, length = 500)
    var text: String,

    /** 첨부된 Asset UUID. nullable — 텍스트 전용 포스트 허용. */
    @Column(name = "asset_id")
    val assetId: UUID? = null,

    /** 첨부된 단일 Track UUID. assetId와 독립적으로 사용 가능. */
    @Column(name = "track_id")
    val trackId: UUID? = null,

    /** 무드 태그 (예: "calm", "energetic"). PRD §5 — 포스트=텍스트+무드태그. */
    @Column(name = "mood_tag", length = 50)
    val moodTag: String? = null,

    /**
     * 소프트 삭제 플래그.
     * true이면 피드 쿼리에서 제외. polyenm_pan CommentService.delete -> markDeleted() 패턴 차용.
     * plshare 변경: polyenm BaseEntity.markDeleted() 없이 서비스에서 직접 flag를 세운다.
     */
    @Column(nullable = false)
    var deleted: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
