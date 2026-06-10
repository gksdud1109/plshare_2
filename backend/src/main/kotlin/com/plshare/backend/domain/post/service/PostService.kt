package com.plshare.backend.domain.post.service

import com.plshare.backend.domain.asset.repository.AssetRepository
import com.plshare.backend.domain.comment.repository.CommentRepository
import com.plshare.backend.domain.follow.repository.FollowRepository
import com.plshare.backend.domain.post.dto.CreatePostRequest
import com.plshare.backend.domain.post.dto.PostResponse
import com.plshare.backend.domain.post.model.Post
import com.plshare.backend.domain.post.repository.PostRepository
import com.plshare.backend.domain.reaction.repository.PostLikeRepository
import com.plshare.backend.domain.user.repository.UserRepository
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import com.plshare.backend.global.response.PageResponse
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 포스트 도메인 서비스.
 *
 * polyenm_pan CommentService의 서비스 책임 분리 패턴 차용:
 * - 작성자 존재 검증 → UserRepository로 직접 조회 (순환 방지).
 * - soft-delete: deleted=true 세팅 후 저장 (polyenm BaseEntity.markDeleted() 패턴을 서비스에서 직접 구현).
 *
 * plshare 변경:
 * - authorId는 UUID (Long 대신).
 * - likeCount/commentCount/likedByMe는 쿼리 시점에 집계 — N+1 주의: 현재 단건·소규모 피드에 한정.
 *   대규모 트래픽 시 batch count 쿼리로 교체 필요.
 */
@Service
@Transactional(readOnly = true)
class PostService(
    private val posts: PostRepository,
    private val users: UserRepository,
    private val assets: AssetRepository,
    private val likes: PostLikeRepository,
    private val comments: CommentRepository,
    private val follows: FollowRepository,
) {

    @Transactional
    fun create(req: CreatePostRequest): PostResponse {
        if (req.text.length > 500) {
            throw ApiException(ErrorCode.VALIDATION_FAILED, "포스트 본문은 500자를 초과할 수 없습니다")
        }
        val author = users.findById(req.authorId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "작성자를 찾을 수 없습니다: ${req.authorId}")
        }
        if (req.assetId != null && !assets.existsById(req.assetId)) {
            throw ApiException(ErrorCode.NOT_FOUND, "첨부 자산을 찾을 수 없습니다: ${req.assetId}")
        }
        val post = posts.save(
            Post(
                authorId = req.authorId,
                text = req.text,
                assetId = req.assetId,
                trackId = req.trackId,
                moodTag = req.moodTag,
            )
        )
        return PostResponse.from(post, author, likeCount = 0, commentCount = 0, likedByMe = false)
    }

    /** 전체 피드 (최신순, soft-deleted 제외). */
    fun listAll(viewerId: UUID?, pageable: Pageable): PageResponse<PostResponse> {
        val page = posts.findAllByDeletedFalseOrderByCreatedAtDesc(pageable)
        return toPageResponse(page.content, pageable, page.totalElements, viewerId)
    }

    /** 팔로잉 피드: viewerId(userId)가 팔로우한 작성자의 포스트만 반환. */
    fun listFeed(userId: UUID, pageable: Pageable): PageResponse<PostResponse> {
        val followingIds = follows.findAllByFollowerId(userId).map { it.followeeId }
        if (followingIds.isEmpty()) {
            return PageResponse(
                content = emptyList(),
                page = pageable.pageNumber,
                size = pageable.pageSize,
                totalElements = 0,
                totalPages = 0,
                hasNext = false,
            )
        }
        val page = posts.findAllByAuthorIdInAndDeletedFalseOrderByCreatedAtDesc(followingIds, pageable)
        return toPageResponse(page.content, pageable, page.totalElements, userId)
    }

    fun getById(id: UUID, viewerId: UUID?): PostResponse {
        val post = requirePost(id)
        return toResponse(post, viewerId)
    }

    @Transactional
    fun delete(id: UUID, requesterId: UUID) {
        val post = requirePost(id)
        if (post.authorId != requesterId) {
            throw ApiException(ErrorCode.FORBIDDEN, "본인 포스트만 삭제할 수 있습니다")
        }
        post.deleted = true
        posts.save(post)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // internal helpers

    private fun requirePost(id: UUID): Post =
        posts.findByIdAndDeletedFalse(id)
            ?: throw ApiException(ErrorCode.NOT_FOUND, "포스트를 찾을 수 없습니다: $id")

    private fun toResponse(post: Post, viewerId: UUID?): PostResponse {
        val author = users.findById(post.authorId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "작성자를 찾을 수 없습니다: ${post.authorId}")
        }
        val likeCount = likes.countByPostId(post.id)
        val commentCount = comments.countByPostIdAndDeletedFalse(post.id)
        val likedByMe = viewerId != null && likes.existsByPostIdAndUserId(post.id, viewerId)
        return PostResponse.from(post, author, likeCount, commentCount, likedByMe)
    }

    private fun toPageResponse(
        postList: List<Post>,
        pageable: Pageable,
        totalElements: Long,
        viewerId: UUID?,
    ): PageResponse<PostResponse> {
        val authorIds = postList.map { it.authorId }.toSet()
        val authorMap = users.findAllById(authorIds).associateBy { it.id }
        val responses = postList.map { post ->
            val author = authorMap[post.authorId]
                ?: throw ApiException(ErrorCode.NOT_FOUND, "작성자를 찾을 수 없습니다: ${post.authorId}")
            val likeCount = likes.countByPostId(post.id)
            val commentCount = comments.countByPostIdAndDeletedFalse(post.id)
            val likedByMe = viewerId != null && likes.existsByPostIdAndUserId(post.id, viewerId)
            PostResponse.from(post, author, likeCount, commentCount, likedByMe)
        }
        val pageImpl = PageImpl(responses, pageable, totalElements)
        return PageResponse.from(pageImpl) { it }
    }
}
