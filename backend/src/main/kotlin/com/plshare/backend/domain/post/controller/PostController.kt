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
    fun create(@RequestBody req: CreatePostRequest): ApiResponse<PostResponse> =
        ApiResponse.ok(postService.create(req))

    /** 전체 공개 피드 (최신순). */
    @GetMapping("/posts")
    fun listAll(
        @RequestParam(required = false) viewerId: UUID?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<PostResponse>> =
        ApiResponse.ok(postService.listAll(viewerId, PageRequest.of(page, size)))

    /** 팔로잉 피드: userId가 팔로우한 작성자의 포스트. */
    @GetMapping("/feed")
    fun feed(
        @RequestParam userId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<PostResponse>> =
        ApiResponse.ok(postService.listFeed(userId, PageRequest.of(page, size)))

    /** 포스트 단건 조회. */
    @GetMapping("/posts/{id}")
    fun getById(
        @PathVariable id: UUID,
        @RequestParam(required = false) viewerId: UUID?,
    ): ApiResponse<PostResponse> =
        ApiResponse.ok(postService.getById(id, viewerId))

    /** 포스트 삭제 (작성자만, soft-delete). */
    @DeleteMapping("/posts/{id}")
    fun delete(
        @PathVariable id: UUID,
        @RequestParam requesterId: UUID,
    ): ApiResponse<Unit> {
        postService.delete(id, requesterId)
        return ApiResponse.ok(null)
    }
}
