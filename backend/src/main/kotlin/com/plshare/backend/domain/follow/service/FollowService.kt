package com.plshare.backend.domain.follow.service

import com.plshare.backend.domain.follow.dto.FollowStatsResponse
import com.plshare.backend.domain.follow.model.Follow
import com.plshare.backend.domain.follow.repository.FollowRepository
import com.plshare.backend.domain.user.repository.UserRepository
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 팔로우 도메인 서비스.
 *
 * polyenm_pan FollowService 패턴 차용:
 * - 자기 팔로우 방지: followerId == followeeId → VALIDATION_FAILED.
 * - 멱등 처리: existsBy 사전 조회 + DataIntegrityViolationException catch.
 * - countByFollowerId/countByFolloweeId로 팔로잉/팔로워 수 집계.
 *
 * plshare 변경:
 * - Long id → UUID id, Long followerId/followeeId → UUID.
 * - polyenm DuplicateSafeInserter → 단순 catch 방식.
 * - User 엔티티 수정 금지 — handle로 유저 조회 후 UUID 추출.
 */
@Service
@Transactional(readOnly = true)
class FollowService(
    private val follows: FollowRepository,
    private val users: UserRepository,
) {

    /**
     * 팔로우 (멱등).
     * followerId == followeeId이면 VALIDATION_FAILED.
     * 대상 유저가 없으면 NOT_FOUND.
     * 이미 팔로우 중이면 no-op.
     */
    @Transactional
    fun follow(followerId: UUID, targetHandle: String) {
        val followee = users.findByHandle(targetHandle)
            ?: throw ApiException(ErrorCode.NOT_FOUND, "유저를 찾을 수 없습니다: $targetHandle")
        if (followerId == followee.id) {
            throw ApiException(ErrorCode.VALIDATION_FAILED, "자기 자신을 팔로우할 수 없습니다")
        }
        // 팔로워 본인 존재 확인
        users.findById(followerId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "팔로워 유저를 찾을 수 없습니다: $followerId")
        }
        if (!follows.existsByFollowerIdAndFolloweeId(followerId, followee.id)) {
            try {
                follows.save(Follow(followerId = followerId, followeeId = followee.id))
            } catch (_: DataIntegrityViolationException) {
                // 동시 요청 경쟁에서 패배: 이미 존재하므로 no-op
            }
        }
    }

    /**
     * 언팔로우 (멱등).
     * 팔로우 관계가 없으면 no-op.
     */
    @Transactional
    fun unfollow(followerId: UUID, targetHandle: String) {
        val followee = users.findByHandle(targetHandle)
            ?: throw ApiException(ErrorCode.NOT_FOUND, "유저를 찾을 수 없습니다: $targetHandle")
        follows.deleteByFollowerIdAndFolloweeId(followerId, followee.id)
    }

    /** handle에 해당하는 유저의 팔로우 통계. */
    fun stats(handle: String, viewerId: UUID?): FollowStatsResponse {
        val target = users.findByHandle(handle)
            ?: throw ApiException(ErrorCode.NOT_FOUND, "유저를 찾을 수 없습니다: $handle")
        val followerCount = follows.countByFolloweeId(target.id)
        val followingCount = follows.countByFollowerId(target.id)
        val isFollowing = viewerId != null && follows.existsByFollowerIdAndFolloweeId(viewerId, target.id)
        return FollowStatsResponse(
            followerCount = followerCount,
            followingCount = followingCount,
            isFollowing = isFollowing,
        )
    }
}
