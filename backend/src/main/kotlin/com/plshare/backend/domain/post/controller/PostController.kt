package com.plshare.backend.domain.post.controller

import com.plshare.backend.domain.post.dto.CreatePostRequest
import com.plshare.backend.domain.post.dto.PostResponse
import com.plshare.backend.domain.post.service.PostService
import com.plshare.backend.global.response.ApiResponse
import com.plshare.backend.global.response.PageResponse
import com.plshare.backend.global.security.CurrentUserId
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * 포스트 REST 컨트롤러.
 *
 * 신원(작성자/요청자/뷰어)은 [CurrentUserId] 로 세션 principal 에서만 해석한다.
 * 쓰기는 인증 필수(`UUID`), 공개 GET 의 viewer 는 선택적(`UUID?`) — 미인증이면 개인화(likedByMe) 없음.
 *
 * polyenm_pan 컨벤션: API 인터페이스 분리 없이 직접 @RestController.
 */
@RestController
@RequestMapping("/api")
class PostController(private val postService: PostService) {

    /** 포스트 생성. body: {text, assetId?, trackId?, moodTag?} */
    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody req: CreatePostRequest,
        @CurrentUserId authorId: UUID,
    ): ApiResponse<PostResponse> =
        ApiResponse.ok(postService.create(req, authorId, requireOwnedAsset = true))

    /** 전체 공개 피드 (최신순). */
    @GetMapping("/posts")
    fun listAll(
        @CurrentUserId viewerId: UUID?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<PostResponse>> =
        ApiResponse.ok(postService.listAll(viewerId, PageRequest.of(page, size)))

    /** 팔로잉 피드: 현재 사용자가 팔로우한 작성자의 포스트. */
    @GetMapping("/feed")
    fun feed(
        @CurrentUserId userId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<PostResponse>> =
        ApiResponse.ok(postService.listFeed(userId, PageRequest.of(page, size)))

    /** 프로필 피드: handle 작성자의 포스트 목록 (fe-feed-001 BLOCKER 해소). */
    @GetMapping("/users/{handle}/posts")
    fun listByAuthor(
        @PathVariable handle: String,
        @CurrentUserId viewerId: UUID?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<PostResponse>> =
        ApiResponse.ok(
            postService.listByAuthorHandle(handle, viewerId, PageRequest.of(page, size))
        )

    /** 포스트 단건 조회. */
    @GetMapping("/posts/{id}")
    fun getById(
        @PathVariable id: UUID,
        @CurrentUserId viewerId: UUID?,
    ): ApiResponse<PostResponse> =
        ApiResponse.ok(postService.getById(id, viewerId))

    /** 포스트 삭제 (작성자만, soft-delete). */
    @DeleteMapping("/posts/{id}")
    fun delete(
        @PathVariable id: UUID,
        @CurrentUserId requesterId: UUID,
    ): ApiResponse<Unit> {
        postService.delete(id, requesterId)
        return ApiResponse.ok(null)
    }
}
