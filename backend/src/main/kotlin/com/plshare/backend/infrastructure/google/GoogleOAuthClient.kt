package com.plshare.backend.infrastructure.google

import reactor.core.publisher.Mono

/**
 * Abstraction over Google OAuth 2.0 and OpenID Connect.
 *
 * Scope strategy (Incremental Auth):
 * - Standard sign-in uses `openid email profile`.
 * - YouTube data access (`youtube.readonly`) is requested separately via an additional
 *   consent screen triggered by `GET /api/auth/google/start?scope=youtube`.
 *   This follows Google's recommended incremental authorisation pattern so users are
 *   not prompted for broad permissions upfront.
 *
 * Implementations:
 * - [MockGoogleOAuthClient] — active under `@Profile("demo")`, returns fixed demo data.
 * - [RealGoogleOAuthClient] — active under `@Profile("!demo")`, calls Google's endpoints.
 */
interface GoogleOAuthClient {

    /**
     * Builds the Google authorization URL to redirect the user to.
     *
     * @param state     CSRF-prevention nonce stored in [OauthHandshake].
     * @param codeChallenge PKCE S256 challenge; null if the flow doesn't use PKCE.
     * @param redirectUri The callback URI registered in Google Cloud Console.
     * @param scopes    OAuth scopes to request (e.g. ["openid", "email", "profile"]).
     */
    fun buildAuthorizationUrl(
        state: String,
        codeChallenge: String?,
        redirectUri: String,
        scopes: List<String>
    ): String

    /**
     * Exchanges an authorization code for a [GoogleTokenSet].
     *
     * @param code         Authorization code received at the callback.
     * @param codeVerifier PKCE verifier matching the challenge used in [buildAuthorizationUrl].
     * @param redirectUri  Must exactly match the one used in [buildAuthorizationUrl].
     */
    fun exchangeCodeForToken(
        code: String,
        codeVerifier: String,
        redirectUri: String
    ): Mono<GoogleTokenSet>

    /**
     * Fetches user identity from Google's OpenID Connect userinfo endpoint.
     * Returns sub, email, name, and picture fields.
     *
     * @param accessToken A valid user-scoped access token from [exchangeCodeForToken].
     */
    fun fetchUserInfo(accessToken: String): Mono<GoogleUserInfo>
}
