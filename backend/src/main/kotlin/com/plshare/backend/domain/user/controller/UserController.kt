package com.plshare.backend.domain.user.controller

import com.plshare.backend.domain.user.dto.UserMeDto
import com.plshare.backend.domain.user.dto.UserProfileDto
import com.plshare.backend.domain.user.repository.UserRepository
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import com.plshare.backend.global.response.ApiResponse
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userRepository: UserRepository
) {

    /**
     * Returns the private profile for the given user.
     *
     * NOTE (pre-session integration): This endpoint currently accepts [userId] as a query
     * parameter rather than resolving identity from a session/JWT. This is intentional —
     * full session wiring (Spring Security + JWT) is deferred to a later task. Once that
     * lands, this parameter will be replaced by `@AuthenticationPrincipal`.
     *
     * Until then callers supply the userId obtained from the Google OAuth callback redirect
     * (`?session=<userId>`).
     */
    @GetMapping("/me")
    @Transactional(readOnly = true)
    fun me(@RequestParam("userId") userId: UUID): ApiResponse<UserMeDto> {
        val user = userRepository.findById(userId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "User not found: $userId")
        }
        return ApiResponse.ok(UserMeDto.from(user))
    }

    /**
     * Returns the public profile for the given [handle].
     * Safe to call without authentication.
     */
    @GetMapping("/{handle}")
    @Transactional(readOnly = true)
    fun publicProfile(@PathVariable handle: String): ApiResponse<UserProfileDto> {
        val user = userRepository.findByHandle(handle)
            ?: throw ApiException(ErrorCode.NOT_FOUND, "User not found: $handle")
        return ApiResponse.ok(UserProfileDto.from(user))
    }
}
