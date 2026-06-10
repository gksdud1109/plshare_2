package com.plshare.backend.domain.post.dto

import com.plshare.backend.domain.post.model.Post
import com.plshare.backend.domain.user.model.User
import java.time.LocalDateTime
import java.util.UUID

/** 포스트 생성 요청 DTO. */
data class CreatePostRequest(
    /**
     * NOTE (pre-session integration): 세션/JWT 통합 전 임시 파라미터. UserController의 userId 파라미터 패턴과 동일.
     * Spring Security 도입 후 @AuthenticationPrincipal로 교체 예정.
     */
    val authorId: UUID,
    val text: String,
    val assetId: UUID? = null,
    val trackId: UUID? = null,
    val moodTag: String? = null,
)

/** 포스트 응답 DTO. 작성자 정보(handle/displayName/avatarUrl)를 포함한다. */
data class PostResponse(
    val id: UUID,
    val authorId: UUID,
    val authorHandle: String,
    val authorDisplayName: String,
    val authorAvatarUrl: String?,
    val text: String,
    val assetId: UUID?,
    val trackId: UUID?,
    val moodTag: String?,
    val likeCount: Long,
    val commentCount: Long,
    val likedByMe: Boolean,
    val createdAt: LocalDateTime,
) {
    companion object {
        /**
         * Post + User + 집계(likeCount, commentCount, likedByMe)로부터 응답 DTO 생성.
         * DTO 변환은 companion object from(…) 패턴 — plshare 컨벤션(UserMeDto 등) 차용.
         */
        fun from(
            post: Post,
            author: User,
            likeCount: Long,
            commentCount: Long,
            likedByMe: Boolean,
        ): PostResponse = PostResponse(
            id = post.id,
            authorId = post.authorId,
            authorHandle = author.handle,
            authorDisplayName = author.displayName,
            authorAvatarUrl = author.avatarUrl,
            text = post.text,
            assetId = post.assetId,
            trackId = post.trackId,
            moodTag = post.moodTag,
            likeCount = likeCount,
            commentCount = commentCount,
            likedByMe = likedByMe,
            createdAt = post.createdAt,
        )
    }
}
