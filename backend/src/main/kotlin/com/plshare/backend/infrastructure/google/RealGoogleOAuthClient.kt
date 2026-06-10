package com.plshare.backend.infrastructure.google

import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * Real Google OAuth 2.0 adapter (production / non-demo profile).
 *
 * Credentials are injected from `application-prod.yml` (or env vars).
 * If `google.client-id` is blank the bean still starts but will throw
 * [ApiException(UPSTREAM_ERROR)] at call time — matching the Apple/Spotify pattern
 * where misconfiguration surfaces at invocation rather than boot.
 *
 * Endpoints used:
 * - Authorization: https://accounts.google.com/o/oauth2/v2/auth
 * - Token:         https://oauth2.googleapis.com/token
 * - UserInfo:      https://openidconnect.googleapis.com/v1/userinfo
 */
@Component("googleOAuthClient")
@Profile("!demo")
class RealGoogleOAuthClient(
    @Value("\${google.client-id:}") private val clientId: String,
    @Value("\${google.client-secret:}") private val clientSecret: String,
    @Value("\${google.redirect-uri:http://localhost:8080/api/auth/google/callback}")
    private val defaultRedirectUri: String
) : GoogleOAuthClient {

    private val tokenClient = WebClient.create(TOKEN_BASE)
    private val userInfoClient = WebClient.create(USERINFO_BASE)

    override fun buildAuthorizationUrl(
        state: String,
        codeChallenge: String?,
        redirectUri: String,
        scopes: List<String>
    ): String {
        requireCredentials()
        val builder = UriComponentsBuilder.fromHttpUrl("$AUTH_BASE/auth")
            .queryParam("response_type", "code")
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("state", state)
            .queryParam("scope", scopes.joinToString(" "))
            .queryParam("access_type", "offline")   // request refresh_token
            .queryParam("prompt", "consent")         // always show consent to get refresh_token

        if (codeChallenge != null) {
            builder
                .queryParam("code_challenge_method", "S256")
                .queryParam("code_challenge", codeChallenge)
        }

        return builder.build().encode().toUriString()
    }

    override fun exchangeCodeForToken(
        code: String,
        codeVerifier: String,
        redirectUri: String
    ): Mono<GoogleTokenSet> {
        requireCredentials()
        val form: MultiValueMap<String, String> = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("code", code)
            add("redirect_uri", redirectUri)
            add("client_id", clientId)
            add("client_secret", clientSecret)
            add("code_verifier", codeVerifier)
        }
        return tokenClient.post()
            .uri("/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(form))
            .retrieve()
            .bodyToMono<GoogleTokenResponse>()
            .timeout(Duration.ofSeconds(10))
            .map { it.toTokenSet() }
    }

    override fun fetchUserInfo(accessToken: String): Mono<GoogleUserInfo> {
        requireCredentials()
        return userInfoClient.get()
            .uri("/v1/userinfo")
            .headers { it.setBearerAuth(accessToken) }
            .retrieve()
            .bodyToMono<GoogleUserInfo>()
            .timeout(Duration.ofSeconds(10))
    }

    /**
     * Fail fast at call time (not at boot) when credentials are unconfigured.
     * Matches Apple / Spotify convention: demo profile boots without credentials,
     * real profile must have them set before any call is made.
     */
    private fun requireCredentials() {
        if (clientId.isBlank()) {
            throw ApiException(
                ErrorCode.UPSTREAM_ERROR,
                "Google OAuth client-id is not configured. " +
                    "Set google.client-id in application-prod.yml or the GOOGLE_CLIENT_ID env var."
            )
        }
    }

    private fun GoogleTokenResponse.toTokenSet() = GoogleTokenSet(
        accessToken = accessToken,
        refreshToken = refreshToken,
        tokenType = tokenType,
        expiresInSeconds = expiresIn.toLong(),
        scope = scope,
        idToken = idToken
    )

    companion object {
        private const val AUTH_BASE = "https://accounts.google.com/o/oauth2/v2"
        private const val TOKEN_BASE = "https://oauth2.googleapis.com"
        private const val USERINFO_BASE = "https://openidconnect.googleapis.com"
    }
}
