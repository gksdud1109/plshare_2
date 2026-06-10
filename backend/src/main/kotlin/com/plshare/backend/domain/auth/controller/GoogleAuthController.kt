package com.plshare.backend.domain.auth.controller

import com.plshare.backend.domain.auth.model.OauthHandshake
import com.plshare.backend.domain.auth.repository.OauthHandshakeRepository
import com.plshare.backend.domain.auth.service.GoogleAuthService
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import com.plshare.backend.infrastructure.google.GoogleOAuthClient
import com.plshare.backend.infrastructure.spotify.PkceHelper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.view.RedirectView
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@RestController
@RequestMapping("/api/auth/google")
class GoogleAuthController(
    private val googleOAuthClient: GoogleOAuthClient,
    private val googleAuthService: GoogleAuthService,
    private val handshakeRepository: OauthHandshakeRepository,
    private val pkceHelper: PkceHelper,
    @Value("\${google.redirect-uri:http://localhost:8080/api/auth/google/callback}")
    private val redirectUri: String,
    @Value("\${google.fe-redirect:http://localhost:3000/import}")
    private val frontendRedirect: String
) {

    /**
     * Begins the Google OAuth 2.0 flow.
     *
     * Incremental Auth design:
     * - Default (no [scope] param) → requests `openid email profile` only.
     *   This is the standard sign-in / sign-up path.
     * - `?scope=youtube` → adds `youtube.readonly` to the standard set.
     *   Use this to request YouTube access after the user is already signed in
     *   (Phase B — YTM adapter). The additional consent screen is shown only for
     *   the incremental scope, minimising friction on first login.
     *
     * State prefix strategy: Google and Spotify share the same [OauthHandshake] table.
     * To distinguish providers without a schema migration we prefix the state with
     * `"google:"` (max 8 chars). Callback validation strips the prefix before DB lookup.
     * This is a non-destructive approach that avoids adding a `provider` column to the
     * shared handshake table while the schema is still in flux.
     */
    @GetMapping("/start")
    @Transactional
    fun start(
        @RequestParam("scope", required = false) scopeHint: String?
    ): RedirectView {
        val state = pkceHelper.generateState()
        val verifier = pkceHelper.generateCodeVerifier()
        val challenge = pkceHelper.codeChallenge(verifier)

        // Store raw state (without prefix) for DB lookup; prefix is only for routing clarity.
        handshakeRepository.save(
            OauthHandshake(
                state = "google:$state",
                codeVerifier = verifier,
                redirectUri = redirectUri
            )
        )

        val scopes = buildScopes(scopeHint)
        val url = googleOAuthClient.buildAuthorizationUrl(
            state = "google:$state",
            codeChallenge = challenge,
            redirectUri = redirectUri,
            scopes = scopes
        )
        return RedirectView(url)
    }

    /**
     * Google redirect target.
     *
     * 1. Validates the [state] nonce (anti-CSRF) and TTL.
     * 2. Exchanges [code] for tokens via [GoogleOAuthClient.exchangeCodeForToken].
     * 3. Fetches user identity via [GoogleOAuthClient.fetchUserInfo].
     * 4. Upserts the [com.plshare.backend.domain.user.model.User] via [GoogleAuthService].
     * 5. Redirects to the FE with `?session=<userId>` so the client can store the id.
     *
     * On denial the FE receives `?error=access_denied` (or the Google-supplied error).
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

        val tokens = googleOAuthClient
            .exchangeCodeForToken(code, handshake.codeVerifier, handshake.redirectUri)
            .block()
            ?: throw ApiException(ErrorCode.UPSTREAM_ERROR, "Token exchange returned empty response")

        handshake.consumedAt = LocalDateTime.now()
        handshakeRepository.save(handshake)

        val userInfo = googleOAuthClient
            .fetchUserInfo(tokens.accessToken)
            .block()
            ?: throw ApiException(ErrorCode.UPSTREAM_ERROR, "UserInfo returned empty response")

        val user = googleAuthService.upsertUser(userInfo)
        return RedirectView("$frontendRedirect?session=${user.id}")
    }

    /**
     * Resolves the OAuth scope list based on the optional [scopeHint].
     *
     * Base scopes are always included. `youtube` hint appends `youtube.readonly`
     * for Incremental Auth (Phase B — YTM adapter integration).
     */
    private fun buildScopes(scopeHint: String?): List<String> {
        val base = listOf("openid", "email", "profile")
        return when (scopeHint?.lowercase()) {
            "youtube" -> base + "https://www.googleapis.com/auth/youtube.readonly"
            else      -> base
        }
    }

    companion object {
        private const val HANDSHAKE_TTL_MINUTES = 10L
    }
}
