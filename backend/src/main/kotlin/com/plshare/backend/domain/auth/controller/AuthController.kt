package com.plshare.backend.domain.auth.controller

import com.plshare.backend.domain.auth.model.OauthHandshake
import com.plshare.backend.domain.auth.repository.OauthHandshakeRepository
import com.plshare.backend.domain.auth.service.SpotifyAccessGrantService
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import com.plshare.backend.global.response.ApiResponse
import com.plshare.backend.infrastructure.spotify.PkceHelper
import com.plshare.backend.infrastructure.spotify.SpotifyClient
import com.plshare.backend.domain.user.model.User
import com.plshare.backend.domain.user.repository.UserRepository
import com.plshare.backend.global.security.ApplicationSessionService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.transaction.annotation.Transactional
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.view.RedirectView
import com.plshare.backend.global.security.ApplicationPrincipal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

@RestController
@RequestMapping("/api/auth/spotify")
class AuthController(
    private val spotifyClient: SpotifyClient,
    private val grantService: SpotifyAccessGrantService,
    private val handshakeRepository: OauthHandshakeRepository,
    private val pkceHelper: PkceHelper,
    private val userRepository: UserRepository,
    private val applicationSessions: ApplicationSessionService,
    @Value("\${spotify.redirect-uri:http://localhost:8080/api/auth/spotify/callback}")
    private val redirectUri: String,
    @Value("\${spotify.scopes:playlist-read-private,playlist-read-collaborative}")
    private val scopesCsv: String,
    @Value("\${spotify.fe-redirect:http://localhost:3000/import}")
    private val frontendRedirect: String
) {
    private val scopes: List<String>
        get() = scopesCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    /**
     * Begins the OAuth 2.0 PKCE flow.
     * Persists a fresh handshake (state + verifier) and 302 redirects to Spotify.
     */
    @GetMapping("/start")
    @Transactional
    fun start(
        @RequestParam("returnTo", required = false) returnTo: String?,
        @AuthenticationPrincipal principal: ApplicationPrincipal?,
    ): RedirectView {
        val state = pkceHelper.generateState()
        val verifier = pkceHelper.generateCodeVerifier()
        val challenge = pkceHelper.codeChallenge(verifier)

        handshakeRepository.save(
            OauthHandshake(
                state = state,
                codeVerifier = verifier,
                redirectUri = redirectUri,
                userId = principal?.userId,
                returnPath = safeReturnPath(returnTo),
            )
        )

        val url = spotifyClient.buildAuthorizationUrl(
            state = state,
            codeChallenge = challenge,
            redirectUri = redirectUri,
            scopes = scopes
        )
        return RedirectView(url)
    }

    /**
     * Spotify redirect target. Validates handshake, exchanges code for tokens,
     * saves a `SpotifyAccessGrant`, then redirects to the FE with the grant id.
     */
    @GetMapping("/callback")
    @Transactional
    fun callback(
        @RequestParam("code", required = false) code: String?,
        @RequestParam("state", required = false) state: String?,
        @RequestParam("error", required = false) error: String?,
        response: HttpServletResponse
    ): RedirectView {
        if (error != null) {
            return RedirectView("$frontendRedirect?error=$error")
        }
        require(!code.isNullOrBlank()) { "Missing code" }
        require(!state.isNullOrBlank()) { "Missing state" }

        val handshake = handshakeRepository.findByState(state)
            ?: throw ApiException(ErrorCode.NOT_FOUND, "Unknown OAuth state")

        if (handshake.consumedAt != null) {
            throw ApiException(ErrorCode.CONFLICT, "OAuth state already consumed")
        }
        val ageMinutes = ChronoUnit.MINUTES.between(handshake.createdAt, LocalDateTime.now())
        require(ageMinutes < HANDSHAKE_TTL_MINUTES) { "OAuth state expired" }

        val tokens = spotifyClient
            .exchangeCodeForToken(code, handshake.codeVerifier, handshake.redirectUri)
            .block()
            ?: throw ApiException(ErrorCode.UPSTREAM_ERROR, "Token exchange returned empty response")

        handshake.consumedAt = LocalDateTime.now()
        handshakeRepository.save(handshake)

        val user = handshake.userId
            ?.let { userRepository.findById(it).orElseThrow {
                ApiException(ErrorCode.NOT_FOUND, "User not found: $it")
            } }
            ?: userRepository.save(
                User(
                    displayName = "Spotify User",
                    handle = uniqueSpotifyHandle(),
                )
            )
        val grant = grantService.saveNewGrant(user.id, tokens)
        return RedirectView(frontendCallbackUrl(
            applicationSessions.issue(user.id, grant.id),
            handshake.returnPath,
        ))
    }

    /**
     * Force-refresh the access token for a grant.
     */
    @PostMapping("/refresh")
    fun refresh(
        @RequestBody body: RefreshRequest,
        @AuthenticationPrincipal principal: ApplicationPrincipal?,
    ): ApiResponse<GrantStatusResponse> {
        val grant = grantService.findGrant(body.grantId)
            ?: throw ApiException(ErrorCode.NOT_FOUND, "Grant not found: ${body.grantId}")
        requireGrantOwner(grant.userId, grant.id, principal)
        val refreshToken = grant.refreshToken
            ?: throw ApiException(ErrorCode.VALIDATION_FAILED, "Grant has no refresh token")

        val tokens = spotifyClient.refreshAccessToken(refreshToken).block()
            ?: throw ApiException(ErrorCode.UPSTREAM_ERROR, "Refresh returned empty response")
        val updated = grantService.persistRefresh(body.grantId, tokens)
        return ApiResponse.ok(GrantStatusResponse.from(updated, expiringSoon = false))
    }

    /**
     * Lightweight grant introspection used by the FE to know whether to refresh.
     */
    @GetMapping("/me")
    fun me(
        @RequestParam("grantId") grantId: UUID,
        @AuthenticationPrincipal principal: ApplicationPrincipal?,
    ): ApiResponse<GrantStatusResponse> {
        val grant = grantService.findGrant(grantId)
            ?: throw ApiException(ErrorCode.NOT_FOUND, "Grant not found: $grantId")
        requireGrantOwner(grant.userId, grant.id, principal)
        val expiringSoon = grantService.isExpiringSoon(grant)
        return ApiResponse.ok(GrantStatusResponse.from(grant, expiringSoon))
    }

    data class RefreshRequest(val grantId: UUID)

    data class GrantStatusResponse(
        val grantId: UUID,
        val userId: UUID,
        val expiresAt: LocalDateTime,
        val expiringSoon: Boolean,
        val scope: String?
    ) {
        companion object {
            fun from(grant: com.plshare.backend.domain.auth.model.SpotifyAccessGrant, expiringSoon: Boolean) =
                GrantStatusResponse(
                    grantId = grant.id,
                    userId = grant.userId,
                    expiresAt = grant.expiresAt,
                    expiringSoon = expiringSoon,
                    scope = grant.scope
                )
        }
    }

    companion object {
        private const val HANDSHAKE_TTL_MINUTES = 10
    }

    private fun uniqueSpotifyHandle(): String {
        repeat(10) {
            val candidate = "spotify-${UUID.randomUUID().toString().take(8)}"
            if (userRepository.findByHandle(candidate) == null) return candidate
        }
        return "spotify-${System.currentTimeMillis()}"
    }

    private fun requireGrantOwner(userId: UUID, grantId: UUID, principal: ApplicationPrincipal?) {
        if (principal != null &&
            (principal.userId != userId || principal.spotifyGrantId != grantId)
        ) {
            throw ApiException(ErrorCode.FORBIDDEN, "Spotify grant does not belong to the current user")
        }
    }

    private fun frontendCallbackUrl(session: String, returnPath: String?): String {
        val frontend = URI(frontendRedirect)
        val origin = "${frontend.scheme}://${frontend.rawAuthority}"
        val next = safeReturnPath(returnPath) ?: frontend.rawPath.ifBlank { "/import" }
        return "$origin/api/auth/callback?session=${encode(session)}&next=${encode(next)}"
    }

    private fun safeReturnPath(value: String?): String? =
        value?.takeIf { it.startsWith("/") && !it.startsWith("//") && it.length <= 1024 }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)
}
