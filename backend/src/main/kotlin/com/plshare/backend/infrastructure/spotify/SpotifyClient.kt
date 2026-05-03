package com.plshare.backend.infrastructure.spotify

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.Duration

@Component
class SpotifyClient(
    private val webClient: WebClient,
    @Value("\${spotify.client-id}") private val clientId: String,
    @Value("\${spotify.client-secret}") private val clientSecret: String
) {
    private val authClient = WebClient.create("https://accounts.spotify.com")
    private val apiClient = WebClient.create("https://api.spotify.com/v1")

    fun getAccessToken(): Mono<String> {
        return authClient.post()
            .uri("/api/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue("grant_type=client_credentials")
            .headers { it.setBasicAuth(clientId, clientSecret) }
            .retrieve()
            .bodyToMono<SpotifyTokenResponse>()
            .map { it.accessToken }
            .timeout(Duration.ofSeconds(5)) // Timeout for resilience
    }

    fun getPlaylist(playlistId: String, accessToken: String): Mono<SpotifyPlaylistResponse> {
        return apiClient.get()
            .uri("/playlists/$playlistId")
            .headers { it.setBearerAuth(accessToken) }
            .retrieve()
            .bodyToMono<SpotifyPlaylistResponse>()
            .timeout(Duration.ofSeconds(10)) // Playlist could be large
    }
}
