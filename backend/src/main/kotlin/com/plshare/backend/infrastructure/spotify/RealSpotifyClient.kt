package com.plshare.backend.infrastructure.spotify

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
 * Real Spotify adapter (production / non-demo profile).
 * Implements OAuth 2.0 Authorization Code + PKCE flow plus the legacy
 * client_credentials path used for anonymous metadata fetches.
 */
@Component("spotifyClient")
@Profile("!demo")
class RealSpotifyClient(
    @Value("\${spotify.client-id}") private val clientId: String,
    @Value("\${spotify.client-secret:}") private val clientSecret: String
) : SpotifyClient {

    private val authClient = WebClient.create(AUTH_BASE)
    private val apiClient = WebClient.create(API_BASE)

    // --- client credentials (anonymous) ---

    override fun getAccessToken(): Mono<String> {
        return authClient.post()
            .uri("/api/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue("grant_type=client_credentials")
            .headers { it.setBasicAuth(clientId, clientSecret) }
            .retrieve()
            .bodyToMono<SpotifyTokenResponse>()
            .map { it.accessToken }
            .timeout(Duration.ofSeconds(5))
    }

    override fun getPlaylist(playlistId: String, accessToken: String): Mono<SpotifyPlaylistResponse> {
        return apiClient.get()
            .uri("/playlists/$playlistId")
            .headers { it.setBearerAuth(accessToken) }
            .retrieve()
            .bodyToMono<SpotifyPlaylistResponse>()
            .timeout(Duration.ofSeconds(10))
    }

    override fun listUserPlaylists(accessToken: String): Mono<List<SpotifyPlaylistResponse>> {
        // For anonymous/client-credentials path we cannot list user playlists.
        return Mono.just(emptyList())
    }

    // --- OAuth 2.0 Authorization Code + PKCE ---

    override fun buildAuthorizationUrl(
        state: String,
        codeChallenge: String,
        redirectUri: String,
        scopes: List<String>
    ): String {
        return UriComponentsBuilder.fromHttpUrl("$AUTH_BASE/authorize")
            .queryParam("response_type", "code")
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("state", state)
            .queryParam("code_challenge_method", "S256")
            .queryParam("code_challenge", codeChallenge)
            .queryParam("scope", scopes.joinToString(" "))
            .build()
            .encode()
            .toUriString()
    }

    override fun exchangeCodeForToken(
        code: String,
        codeVerifier: String,
        redirectUri: String
    ): Mono<SpotifyTokenSet> {
        val form: MultiValueMap<String, String> = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("code", code)
            add("redirect_uri", redirectUri)
            add("client_id", clientId)
            add("code_verifier", codeVerifier)
        }
        return authClient.post()
            .uri("/api/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(form))
            .retrieve()
            .bodyToMono<SpotifyTokenResponse>()
            .timeout(Duration.ofSeconds(10))
            .map { it.toTokenSet() }
    }

    override fun refreshAccessToken(refreshToken: String): Mono<SpotifyTokenSet> {
        val form: MultiValueMap<String, String> = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "refresh_token")
            add("refresh_token", refreshToken)
            add("client_id", clientId)
        }
        return authClient.post()
            .uri("/api/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(form))
            .retrieve()
            .bodyToMono<SpotifyTokenResponse>()
            .timeout(Duration.ofSeconds(10))
            .map {
                // Spotify may omit refresh_token on refresh; fall back to original.
                it.toTokenSet().copy(refreshToken = it.refreshToken ?: refreshToken)
            }
    }

    override fun getCurrentUserPlaylists(accessToken: String): Mono<List<SpotifyPlaylistResponse>> {
        return apiClient.get()
            .uri { it.path("/me/playlists").queryParam("limit", 50).build() }
            .headers { it.setBearerAuth(accessToken) }
            .retrieve()
            .bodyToMono<SpotifyUserPlaylistsResponse>()
            .timeout(Duration.ofSeconds(10))
            .map { it.items }
    }

    private fun SpotifyTokenResponse.toTokenSet(): SpotifyTokenSet =
        SpotifyTokenSet(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = tokenType,
            expiresInSeconds = expiresIn.toLong(),
            scope = scope
        )

    companion object {
        private const val AUTH_BASE = "https://accounts.spotify.com"
        private const val API_BASE = "https://api.spotify.com/v1"
    }
}
