package com.plshare.backend.domain.follow.repository

import com.plshare.backend.domain.follow.model.Follow
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * Follow 저장소.
 *
 * polyenm_pan FollowRepository 패턴 차용 — existsBy, deleteBy, countBy 네이밍 그대로.
 * plshare 변경: Long id → UUID id, Long followerId/followeeId → UUID.
 */
interface FollowRepository : JpaRepository<Follow, UUID> {
    fun existsByFollowerIdAndFolloweeId(followerId: UUID, followeeId: UUID): Boolean
    fun deleteByFollowerIdAndFolloweeId(followerId: UUID, followeeId: UUID): Long
    fun countByFollowerId(followerId: UUID): Long
    fun countByFolloweeId(followeeId: UUID): Long

    /** 팔로잉 피드용: followerId가 팔로우하는 followeeId 목록. */
    fun findAllByFollowerId(followerId: UUID): List<Follow>
}
