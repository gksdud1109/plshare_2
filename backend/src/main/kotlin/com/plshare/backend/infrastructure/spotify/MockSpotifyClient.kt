package com.plshare.backend.infrastructure.spotify

import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Duration

@Component
@Primary
@Profile("demo")
class MockSpotifyClient : SpotifyClient {

    private val playlists: Map<String, SpotifyPlaylistResponse> = listOf(
        buildPlaylist(
            id = "pl-late-night",
            name = "Late Night Drives",
            cover = "https://picsum.photos/seed/latenight/600/600",
            tracks = listOf(
                Triple("The Weeknd", "Blinding Lights", 200040),
                Triple("Daft Punk", "Something About Us", 230000),
                Triple("M83", "Midnight City", 244000),
                Triple("Tame Impala", "The Less I Know The Better", 216320),
                Triple("Phoenix", "1901", 196000),
                Triple("LCD Soundsystem", "All My Friends", 442000)
            )
        ),
        buildPlaylist(
            id = "pl-weekend-reset",
            name = "Weekend Reset",
            cover = "https://picsum.photos/seed/weekend/600/600",
            tracks = listOf(
                Triple("Bon Iver", "Holocene", 337000),
                Triple("Fleet Foxes", "White Winter Hymnal", 167000),
                Triple("Iron & Wine", "Naked As We Came", 154000),
                Triple("Sufjan Stevens", "Mystery of Love", 247000),
                Triple("The National", "Bloodbuzz Ohio", 281000)
            )
        ),
        buildPlaylist(
            id = "pl-coffee-chill",
            name = "Coffee & Chill",
            cover = "https://picsum.photos/seed/coffee/600/600",
            tracks = listOf(
                Triple("Norah Jones", "Don't Know Why", 187000),
                Triple("Jack Johnson", "Better Together", 207000),
                Triple("Corinne Bailey Rae", "Put Your Records On", 215000),
                Triple("Jose Gonzalez", "Heartbeats", 161000),
                Triple("Vance Joy", "Riptide", 204000),
                Triple("Lord Huron", "The Night We Met", 208000),
                Triple("Hozier", "Cherry Wine", 240000)
            )
        )
    ).associateBy { it.id }

    private fun buildPlaylist(
        id: String,
        name: String,
        cover: String,
        tracks: List<Triple<String, String, Int>>
    ): SpotifyPlaylistResponse {
        val items = tracks.mapIndexed { idx, (artist, title, duration) ->
            val isrc = "MOCK${id.takeLast(4).uppercase()}${"%04d".format(idx)}"
            SpotifyTrackItem(
                track = SpotifyTrack(
                    id = "${id}-track-$idx",
                    name = title,
                    artists = listOf(SpotifyArtist(name = artist)),
                    durationMs = duration,
                    externalIds = mapOf("isrc" to isrc)
                )
            )
        }
        return SpotifyPlaylistResponse(
            id = id,
            name = name,
            images = listOf(SpotifyImage(url = cover)),
            tracks = SpotifyTrackContainer(items = items)
        )
    }

    override fun getAccessToken(): Mono<String> = Mono.just("mock-token")

    override fun getPlaylist(playlistId: String, accessToken: String): Mono<SpotifyPlaylistResponse> {
        val pl = playlists[playlistId]
            ?: return Mono.error(IllegalArgumentException("Mock playlist not found: $playlistId"))
        return Mono.just(pl).delayElement(Duration.ofMillis(300))
    }

    override fun listUserPlaylists(accessToken: String): Mono<List<SpotifyPlaylistResponse>> {
        return Mono.just(playlists.values.toList())
    }
}
