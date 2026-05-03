package com.plshare.backend.infrastructure.spotify

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Duration

interface SpotifyClient {
    fun getAccessToken(): Mono<String>
    fun getPlaylist(playlistId: String, accessToken: String): Mono<SpotifyPlaylistResponse>
    fun listUserPlaylists(accessToken: String): Mono<List<SpotifyPlaylistResponse>>
}

@Component
@Profile("!demo")
class RealSpotifyClient(
    @Value("\${spotify.client-id}") private val clientId: String,
    @Value("\${spotify.client-secret}") private val clientSecret: String
) : SpotifyClient {
    private val authClient = WebClient.create("https://accounts.spotify.com")
    private val apiClient = WebClient.create("https://api.spotify.com/v1")

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
        // Real implementation would call /me/playlists; demo only.
        return Mono.just(emptyList())
    }
}
