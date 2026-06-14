package com.plshare.backend.domain.auth.controller

import com.plshare.backend.domain.user.repository.UserRepository
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import com.plshare.backend.global.response.ApiResponse
import com.plshare.backend.global.security.ApplicationSessionService
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * 데모 세션 발급 — `@Profile("demo")` 한정.
 *
 * 시드된 데모 유저(handle "demo")에 대해 실제 서명된 애플리케이션 세션 토큰을 발급한다.
 * 이로써 demo 도 prod 와 동일하게 Bearer 토큰으로 신원을 전달한다 — 컨트롤러의 요청 param
 * 폴백을 제거할 수 있게 하는 핵심 조각이다. 실 자격증명이 필요한 OAuth 핸드셰이크 없이
 * 로컬 데모를 인증 상태로 만든다. prod 프로파일에는 이 빈이 로드되지 않는다.
 */
@RestController
@RequestMapping("/api/auth/demo-session")
@Profile("demo")
class DemoSessionController(
    private val userRepository: UserRepository,
    private val applicationSessions: ApplicationSessionService,
) {

    @PostMapping
    fun issue(): ApiResponse<DemoSessionResponse> {
        val demoUser = userRepository.findByHandle("demo")
            ?: throw ApiException(ErrorCode.NOT_FOUND, "데모 유저가 시드되지 않았습니다")
        val token = applicationSessions.issue(demoUser.id)
        return ApiResponse.ok(
            DemoSessionResponse(
                sessionToken = token,
                userId = demoUser.id,
                handle = demoUser.handle,
            )
        )
    }

    data class DemoSessionResponse(
        val sessionToken: String,
        val userId: UUID,
        val handle: String,
    )
}
