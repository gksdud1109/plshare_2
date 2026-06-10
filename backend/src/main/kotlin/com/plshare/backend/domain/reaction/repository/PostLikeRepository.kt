package com.plshare.backend.domain.reaction.repository

import com.plshare.backend.domain.reaction.model.PostLike
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * PostLike 저장소.
 *
 * polyenm_pan ReactionRepository 패턴 차용 — existsBy, countBy, deleteBy 메서드 네이밍.
 * plshare 변경: ReactionTargetType enum 없이 post 전용으로 단순화. UUID 키 사용.
 */
interface PostLikeRepository : JpaRepository<PostLike, UUID> {
    fun existsByPostIdAndUserId(postId: UUID, userId: UUID): Boolean
    fun countByPostId(postId: UUID): Long
    fun deleteByPostIdAndUserId(postId: UUID, userId: UUID): Long
    fun findByPostIdAndUserId(postId: UUID, userId: UUID): PostLike?
}
