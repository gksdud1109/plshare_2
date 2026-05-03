package com.plshare.backend.infrastructure.apple

import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration

/**
 * Real Apple MusicKit write adapter (prod profile).
 *
 * Authentication model:
 *  - Authorization: Bearer <developer-token>  (issued by [AppleDeveloperTokenProvider])
 *  - Music-User-Token: <user-token>           (passed in per request from the frontend)
 *
 * Endpoints used:
 *  - POST   /v1/me/library/playlists
 *  - POST   /v1/me/library/playlists/{id}/tracks
 *  - GET    /v1/me/library/playlists/{id}?include=tracks
 *
 * On 429 responses the WebClient pipeline retries with exponential backoff (up to 3
 * attempts). All other 4xx errors propagate immediately.
 *
 * The legacy 2-arg overloads (without user token) are intentionally unsupported here:
 * they delegate to the user-token variants with an empty string, which causes Apple to
 * return 401 — the right behaviour for prod where ExportService will be updated to pass
 * the real token. Until then this class is wired but never exercised in demo runs.
 */
@Component
@Profile("!demo")
class RealAppleMusicWriteAdapter(
    private val tokenProvider: AppleDeveloperTokenProvider,
    private val songLookup: AppleSongLookupClient,
    webClientBuilder: WebClient.Builder
) : AppleMusicWriteAdapter {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val webClient: WebClient = webClientBuilder
        .baseUrl(API_BASE_URL)
        .build()

    override fun createPlaylist(name: String, description: String?): Mono<AppleMusicPlaylistRef> =
        createPlaylist(name, description, "")

    override fun addTracks(
        playlistRef: AppleMusicPlaylistRef,
        tracks: List<AppleMusicTrackInput>
    ): Mono<AppleMusicAddResult> = addTracks(playlistRef, tracks, "")

    override fun verify(playlistRef: AppleMusicPlaylistRef, expected: Int): Mono<AppleMusicVerifyResult> =
        verify(playlistRef, expected, "")

    override fun createPlaylist(
        name: String,
        description: String?,
        userToken: String
    ): Mono<AppleMusicPlaylistRef> {
        val body = mapOf(
            "attributes" to mapOf(
                "name" to name,
                "description" to (description ?: "")
            )
        )
        return webClient.post()
            .uri("/v1/me/library/playlists")
            .header("Authorization", "Bearer ${tokenProvider.provide()}")
            .header("Music-User-Token", userToken)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .retryWhen(rateLimitRetry())
            .map { json ->
                val id = json.path("data").path(0).path("id").asText("")
                AppleMusicPlaylistRef(
                    externalId = id,
                    externalUrl = "https://music.apple.com/library/playlist/$id"
                )
            }
    }

    override fun addTracks(
        playlistRef: AppleMusicPlaylistRef,
        tracks: List<AppleMusicTrackInput>,
        userToken: String
    ): Mono<AppleMusicAddResult> {
        val isrcs = tracks.mapNotNull { it.isrc }.distinct()
        val lookupMono = if (isrcs.isEmpty()) Mono.just(emptyMap()) else songLookup.lookupBatch(isrcs)

        return lookupMono.flatMap { isrcToId ->
            val results = tracks.map { input ->
                val songId = input.isrc?.let { isrcToId[it] }
                if (songId == null) {
                    AppleMusicTrackResult(
                        isrc = input.isrc,
                        title = input.title,
                        artist = input.artist,
                        appleSongId = null,
                        status = "skipped",
                        reason = "no_match"
                    )
                } else {
                    AppleMusicTrackResult(
                        isrc = input.isrc,
                        title = input.title,
                        artist = input.artist,
                        appleSongId = songId,
                        status = "matched"
                    )
                }
            }
            val matchedIds = results.mapNotNull { it.appleSongId }
            val matched = results.count { it.status == "matched" }
            val skipped = results.count { it.status == "skipped" }

            val send = if (matchedIds.isEmpty()) {
                Mono.just(AppleMusicAddResult(matched, skipped, results))
            } else {
                val body = mapOf(
                    "data" to matchedIds.map { id -> mapOf("id" to id, "type" to "songs") }
                )
                webClient.post()
                    .uri("/v1/me/library/playlists/{id}/tracks", playlistRef.externalId)
                    .header("Authorization", "Bearer ${tokenProvider.provide()}")
                    .header("Music-User-Token", userToken)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .retryWhen(rateLimitRetry())
                    .thenReturn(AppleMusicAddResult(matched, skipped, results))
            }
            send
        }
    }

    override fun verify(
        playlistRef: AppleMusicPlaylistRef,
        expected: Int,
        userToken: String
    ): Mono<AppleMusicVerifyResult> {
        return webClient.get()
            .uri("/v1/me/library/playlists/{id}?include=tracks", playlistRef.externalId)
            .header("Authorization", "Bearer ${tokenProvider.provide()}")
            .header("Music-User-Token", userToken)
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .retryWhen(rateLimitRetry())
            .map { json ->
                val tracks = json.path("data").path(0).path("relationships").path("tracks").path("data")
                val actual = if (tracks.isArray) tracks.size() else 0
                AppleMusicVerifyResult(expected = expected, actual = actual, ok = actual >= expected)
            }
    }

    private fun rateLimitRetry(): Retry =
        Retry.backoff(3, Duration.ofMillis(500))
            .maxBackoff(Duration.ofSeconds(5))
            .filter { err ->
                err is WebClientResponseException && err.statusCode == HttpStatus.TOO_MANY_REQUESTS
            }
            .doBeforeRetry { signal ->
                log.warn("Apple Music rate-limited, retry {} due to {}", signal.totalRetries() + 1, signal.failure().message)
            }

    companion object {
        const val API_BASE_URL = "https://api.music.apple.com"
    }
}
