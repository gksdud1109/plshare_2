package com.plshare.backend.infrastructure.spotify

import reactor.core.publisher.Mono

/**
 * Abstraction over Spotify Web API + OAuth.
 *
 * - `getAccessToken()` returns a client_credentials token used for anonymous metadata calls
 *   (kept for backward compatibility with current import flow).
 * - `buildAuthorizationUrl` / `exchangeCodeForToken` / `refreshAccessToken` implement the
 *   OAuth 2.0 Authorization Code + PKCE flow used by `AuthController`.
 * - `getCurrentUserPlaylists` requires a user-bound access token (the result of PKCE).
 */
interface SpotifyClient {
    // --- client credentials (anonymous) ---
    fun getAccessToken(): Mono<String>
    fun getPlaylist(playlistId: String, accessToken: String): Mono<SpotifyPlaylistResponse>
    fun listUserPlaylists(accessToken: String): Mono<List<SpotifyPlaylistResponse>>

    // --- OAuth 2.0 Authorization Code + PKCE ---
    fun buildAuthorizationUrl(
        state: String,
        codeChallenge: String,
        redirectUri: String,
        scopes: List<String>
    ): String

    fun exchangeCodeForToken(
        code: String,
        codeVerifier: String,
        redirectUri: String
    ): Mono<SpotifyTokenSet>

    fun refreshAccessToken(refreshToken: String): Mono<SpotifyTokenSet>

    fun getCurrentUserPlaylists(accessToken: String): Mono<List<SpotifyPlaylistResponse>>
}
