package com.plshare.backend.domain.reaction.service

import com.plshare.backend.domain.post.repository.PostRepository
import com.plshare.backend.domain.reaction.model.PostLike
import com.plshare.backend.domain.reaction.repository.PostLikeRepository
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 포스트 좋아요 토글 서비스.
 *
 * polyenm_pan ReactionService의 멱등 처리 패턴 차용:
 * - existsBy 사전 조회로 흔한 중복 요청을 거른다.
 * - 동시 첫 like 경쟁은 DB unique 제약 (post_id, user_id)이 최종 판단 기준.
 * - DataIntegrityViolationException은 catch해 no-op 처리 — polyenm DuplicateSafeInserter의
 *   REQUIRES_NEW 격리 없이 단순 catch로 처리 (plshare MVP 수준).
 *
 * plshare 변경:
 * - polyenm: ReactionTargetType enum + DuplicateSafeInserter 인프라 의존
 * - plshare: post 전용 단순 PostLike 엔티티, UUID 키, catch 방식 멱등.
 */
@Service
@Transactional(readOnly = true)
class PostLikeService(
    private val likes: PostLikeRepository,
    private val posts: PostRepository,
) {

    /**
     * 좋아요 추가 (멱등).
     * 이미 존재하면 no-op. 반환값: 최신 likeCount.
     */
    @Transactional
    fun like(postId: UUID, userId: UUID): Long {
        requirePostExists(postId)
        if (!likes.existsByPostIdAndUserId(postId, userId)) {
            try {
                likes.save(PostLike(postId = postId, userId = userId))
            } catch (_: DataIntegrityViolationException) {
                // 동시 요청 경쟁에서 패배: 이미 존재하므로 no-op
            }
        }
        return likes.countByPostId(postId)
    }

    /**
     * 좋아요 취소 (멱등).
     * 존재하지 않으면 no-op. 반환값: 최신 likeCount.
     */
    @Transactional
    fun unlike(postId: UUID, userId: UUID): Long {
        requirePostExists(postId)
        likes.deleteByPostIdAndUserId(postId, userId)
        return likes.countByPostId(postId)
    }

    private fun requirePostExists(postId: UUID) {
        if (posts.findByIdAndDeletedFalse(postId) == null) {
            throw ApiException(ErrorCode.NOT_FOUND, "포스트를 찾을 수 없습니다: $postId")
        }
    }
}
