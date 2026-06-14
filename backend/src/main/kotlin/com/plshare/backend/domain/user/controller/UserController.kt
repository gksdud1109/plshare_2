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
import com.plshare.backend.global.security.CurrentUserId

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userRepository: UserRepository
) {

    /**
     * Returns the private profile for the current session user.
     * 신원은 [CurrentUserId](세션 principal)로만 해석한다 — 미인증이면 401.
     */
    @GetMapping("/me")
    @Transactional(readOnly = true)
    fun me(
        @CurrentUserId userId: UUID,
    ): ApiResponse<UserMeDto> {
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
