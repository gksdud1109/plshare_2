package com.plshare.backend.infrastructure.google

import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Demo implementation of [GoogleOAuthClient].
 * Active under `@Profile("demo")` — no real HTTP calls, no credentials required.
 * Returns a fixed demo user so the Google OAuth flow works end-to-end in local development.
 */
@Component
@Primary
@Profile("demo")
class MockGoogleOAuthClient : GoogleOAuthClient {

    override fun buildAuthorizationUrl(
        state: String,
        codeChallenge: String?,
        redirectUri: String,
        scopes: List<String>
    ): String = "https://demo-google-auth/?state=$state&scope=${scopes.joinToString("%20")}"

    override fun exchangeCodeForToken(
        code: String,
        codeVerifier: String,
        redirectUri: String
    ): Mono<GoogleTokenSet> = Mono.just(
        GoogleTokenSet(
            accessToken = "mock-google-access-${code.takeLast(6)}",
            refreshToken = "mock-google-refresh-${code.takeLast(6)}",
            tokenType = "Bearer",
            expiresInSeconds = 3600,
            scope = "openid email profile"
        )
    )

    override fun fetchUserInfo(accessToken: String): Mono<GoogleUserInfo> = Mono.just(
        GoogleUserInfo(
            sub = "google-demo-sub-001",
            email = "demo@plshare.app",
            name = "Demo User",
            picture = "https://picsum.photos/seed/demouser/200/200"
        )
    )
}
