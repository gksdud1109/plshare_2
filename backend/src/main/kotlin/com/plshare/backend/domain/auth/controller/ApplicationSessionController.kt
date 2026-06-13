package com.plshare.backend.domain.auth.controller

import com.plshare.backend.global.response.ApiResponse
import com.plshare.backend.global.security.ApplicationPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/auth/session")
class ApplicationSessionController {
    @GetMapping
    fun session(
        @AuthenticationPrincipal principal: ApplicationPrincipal,
    ): ApiResponse<SessionResponse> =
        ApiResponse.ok(SessionResponse(principal.userId, principal.spotifyGrantId))

    data class SessionResponse(
        val userId: UUID,
        val spotifyGrantId: UUID?,
    )
}
