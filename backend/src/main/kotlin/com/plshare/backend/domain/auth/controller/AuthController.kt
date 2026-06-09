package com.plshare.backend.domain.auth.controller

import com.plshare.backend.domain.auth.model.OauthHandshake
import com.plshare.backend.domain.auth.repository.OauthHandshakeRepository
import com.plshare.backend.domain.auth.service.SpotifyAccessGrantService
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import com.plshare.backend.infrastructure.spotify.PkceHelper
import com.plshare.backend.infrastructure.spotify.SpotifyClient
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.view.RedirectView
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@RestController
@RequestMapping("/api/auth/spotify")
class AuthController(
    private val spotifyClient: SpotifyClient,
    private val grantService: SpotifyAccessGrantService,
    private val handshakeRepository: OauthHandshakeRepository,
    private val pkceHelper: PkceHelper,
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
    fun start(): RedirectView {
        val state = pkceHelper.generateState()
        val verifier = pkceHelper.generateCodeVerifier()
        val challenge = pkceHelper.codeChallenge(verifier)

        handshakeRepository.save(
            OauthHandshake(
                state = state,
                codeVerifier = verifier,
                redirectUri = redirectUri
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

        // No real auth/identity yet — assign an anonymous user id per grant.
        val grant = grantService.saveNewGrant(UUID.randomUUID(), tokens)
        return RedirectView("$frontendRedirect?session=${grant.id}")
    }

    /**
     * Force-refresh the access token for a grant.
     */
    @PostMapping("/refresh")
    fun refresh(@RequestBody body: RefreshRequest): ResponseEntity<GrantStatusResponse> {
        val grant = grantService.findGrant(body.grantId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        val refreshToken = grant.refreshToken
            ?: return ResponseEntity.badRequest().build()

        val tokens = spotifyClient.refreshAccessToken(refreshToken).block()
            ?: throw ApiException(ErrorCode.UPSTREAM_ERROR, "Refresh returned empty response")
        val updated = grantService.persistRefresh(body.grantId, tokens)
        return ResponseEntity.ok(GrantStatusResponse.from(updated, expiringSoon = false))
    }

    /**
     * Lightweight grant introspection used by the FE to know whether to refresh.
     */
    @GetMapping("/me")
    fun me(@RequestParam("grantId") grantId: UUID): ResponseEntity<GrantStatusResponse> {
        val grant = grantService.findGrant(grantId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        val expiringSoon = grantService.isExpiringSoon(grant)
        return ResponseEntity.ok(GrantStatusResponse.from(grant, expiringSoon))
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
}
