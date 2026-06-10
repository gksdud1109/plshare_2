package com.plshare.backend.domain.post.repository

import com.plshare.backend.domain.post.model.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * Post 저장소.
 *
 * 피드 쿼리 전략:
 * - [findAllByDeletedFalseOrderByCreatedAtDesc]: 전체 공개 피드 (최신순, soft-deleted 제외).
 * - [findAllByAuthorIdInAndDeletedFalseOrderByCreatedAtDesc]: 팔로잉 피드 — authorIds in + deleted=false.
 *   polyenm_pan CommentRepository의 answerId in 패턴을 차용. `in` 절 성능은 author_id+created_at 복합 인덱스로 보장.
 */
interface PostRepository : JpaRepository<Post, UUID> {
    /** 전체 피드: deleted=false, 최신순 페이지. */
    fun findAllByDeletedFalseOrderByCreatedAtDesc(pageable: Pageable): Page<Post>

    /** 팔로잉 피드: 특정 authorIds에 속한 포스트만. authorIds 비어 있으면 호출 전에 빈 Page 반환. */
    fun findAllByAuthorIdInAndDeletedFalseOrderByCreatedAtDesc(
        authorIds: Collection<UUID>,
        pageable: Pageable,
    ): Page<Post>

    /** 단건 조회 (soft-delete 필터 포함). */
    fun findByIdAndDeletedFalse(id: UUID): Post?
}
