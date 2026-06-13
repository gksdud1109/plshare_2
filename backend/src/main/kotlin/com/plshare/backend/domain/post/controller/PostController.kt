package com.plshare.backend.domain.post.controller

import com.plshare.backend.domain.post.dto.CreatePostRequest
import com.plshare.backend.domain.post.dto.PostResponse
import com.plshare.backend.domain.post.service.PostService
import com.plshare.backend.global.response.ApiResponse
import com.plshare.backend.global.response.PageResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID
import com.plshare.backend.global.security.ApplicationPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal

/**
 * 포스트 REST 컨트롤러.
 *
 * NOTE (pre-session integration): authorId/requesterId/viewerId/userId는 세션/JWT 통합 전 임시 파라미터.
 * Spring Security 도입 후 @AuthenticationPrincipal로 교체 예정. UserController와 동일 패턴.
 *
 * polyenm_pan 컨벤션: API 인터페이스 분리(@RestController : XxxApi) 없이 직접 @RestController.
 * plshare CONVENTIONS.md — api 인터페이스 분리는 선택적 후속.
 */
@RestController
@RequestMapping("/api")
class PostController(private val postService: PostService) {

    /** 포스트 생성. body: {authorId, text, assetId?, trackId?, moodTag?} */
    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody req: CreatePostRequest,
        @AuthenticationPrincipal principal: ApplicationPrincipal?,
    ): ApiResponse<PostResponse> =
        ApiResponse.ok(
            postService.create(
                req.copy(authorId = principal?.userId ?: req.authorId),
                requireOwnedAsset = principal != null,
            )
        )

    /** 전체 공개 피드 (최신순). */
    @GetMapping("/posts")
    fun listAll(
        @RequestParam(required = false) viewerId: UUID?,
        @AuthenticationPrincipal principal: ApplicationPrincipal?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<PostResponse>> =
        ApiResponse.ok(postService.listAll(principal?.userId ?: viewerId, PageRequest.of(page, size)))

    /** 팔로잉 피드: userId가 팔로우한 작성자의 포스트. */
    @GetMapping("/feed")
    fun feed(
        @RequestParam(required = false) userId: UUID?,
        @AuthenticationPrincipal principal: ApplicationPrincipal?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<PostResponse>> =
        ApiResponse.ok(
            postService.listFeed(
                principal?.userId ?: requireNotNull(userId) { "userId required" },
                PageRequest.of(page, size),
            )
        )

    /** 프로필 피드: handle 작성자의 포스트 목록 (fe-feed-001 BLOCKER 해소). */
    @GetMapping("/users/{handle}/posts")
    fun listByAuthor(
        @PathVariable handle: String,
        @RequestParam(required = false) viewerId: UUID?,
        @AuthenticationPrincipal principal: ApplicationPrincipal?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<PostResponse>> =
        ApiResponse.ok(
            postService.listByAuthorHandle(
                handle,
                principal?.userId ?: viewerId,
                PageRequest.of(page, size),
            )
        )

    /** 포스트 단건 조회. */
    @GetMapping("/posts/{id}")
    fun getById(
        @PathVariable id: UUID,
        @RequestParam(required = false) viewerId: UUID?,
        @AuthenticationPrincipal principal: ApplicationPrincipal?,
    ): ApiResponse<PostResponse> =
        ApiResponse.ok(postService.getById(id, principal?.userId ?: viewerId))

    /** 포스트 삭제 (작성자만, soft-delete). */
    @DeleteMapping("/posts/{id}")
    fun delete(
        @PathVariable id: UUID,
        @RequestParam(required = false) requesterId: UUID?,
        @AuthenticationPrincipal principal: ApplicationPrincipal?,
    ): ApiResponse<Unit> {
        postService.delete(id, principal?.userId ?: requireNotNull(requesterId))
        return ApiResponse.ok(null)
    }
}
