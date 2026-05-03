package com.plshare.backend.infrastructure.apple

import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.UUID

data class AppleMusicPlaylistRef(
    val externalId: String,
    val externalUrl: String
)

data class AppleMusicTrackInput(
    val isrc: String?,
    val title: String,
    val artist: String
)

data class AppleMusicTrackResult(
    val isrc: String?,
    val title: String,
    val artist: String,
    val appleSongId: String?,
    val status: String, // "matched" | "skipped"
    val reason: String? = null
)

data class AppleMusicAddResult(
    val matched: Int,
    val skipped: Int,
    val results: List<AppleMusicTrackResult>
)

data class AppleMusicVerifyResult(
    val expected: Int,
    val actual: Int,
    val ok: Boolean
)

/**
 * Common interface for both Mock (demo profile) and Real (prod profile) implementations.
 *
 * The legacy 3-arg form (without user token) is used by the demo profile and existing
 * ExportService call sites. The variants with `userToken` are intended for the real
 * MusicKit integration where each request must carry the end-user's Music-User-Token.
 */
interface AppleMusicWriteAdapter {
    fun createPlaylist(name: String, description: String?): Mono<AppleMusicPlaylistRef>

    fun addTracks(
        playlistRef: AppleMusicPlaylistRef,
        tracks: List<AppleMusicTrackInput>
    ): Mono<AppleMusicAddResult>

    fun verify(playlistRef: AppleMusicPlaylistRef, expected: Int): Mono<AppleMusicVerifyResult>

    fun createPlaylist(name: String, description: String?, userToken: String): Mono<AppleMusicPlaylistRef> =
        createPlaylist(name, description)

    fun addTracks(
        playlistRef: AppleMusicPlaylistRef,
        tracks: List<AppleMusicTrackInput>,
        userToken: String
    ): Mono<AppleMusicAddResult> = addTracks(playlistRef, tracks)

    fun verify(
        playlistRef: AppleMusicPlaylistRef,
        expected: Int,
        userToken: String
    ): Mono<AppleMusicVerifyResult> = verify(playlistRef, expected)
}

@Component
@Profile("demo")
@Primary
class MockAppleMusicWriteAdapter : AppleMusicWriteAdapter {

    override fun createPlaylist(name: String, description: String?): Mono<AppleMusicPlaylistRef> {
        val id = "apl-${UUID.randomUUID().toString().take(8)}"
        return Mono.just(
            AppleMusicPlaylistRef(
                externalId = id,
                externalUrl = "https://music.apple.com/library/playlist/$id"
            )
        ).delayElement(Duration.ofSeconds(1))
    }

    override fun addTracks(
        playlistRef: AppleMusicPlaylistRef,
        tracks: List<AppleMusicTrackInput>
    ): Mono<AppleMusicAddResult> {
        val results = tracks.map { input ->
            // Skip when ISRC contains "0003" position to simulate occasional misses.
            val skip = input.isrc?.endsWith("0003") == true
            if (skip) {
                AppleMusicTrackResult(
                    isrc = input.isrc,
                    title = input.title,
                    artist = input.artist,
                    appleSongId = null,
                    status = "skipped",
                    reason = "no_match"
                )
            } else {
                val songId = "apl-song-${(input.isrc ?: input.title).hashCode().toUInt()}"
                AppleMusicTrackResult(
                    isrc = input.isrc,
                    title = input.title,
                    artist = input.artist,
                    appleSongId = songId,
                    status = "matched"
                )
            }
        }
        val matched = results.count { it.status == "matched" }
        val skipped = results.count { it.status == "skipped" }
        return Mono.just(AppleMusicAddResult(matched, skipped, results))
            .delayElement(Duration.ofMillis(800))
    }

    override fun verify(playlistRef: AppleMusicPlaylistRef, expected: Int): Mono<AppleMusicVerifyResult> {
        return Mono.just(AppleMusicVerifyResult(expected = expected, actual = expected, ok = true))
            .delayElement(Duration.ofMillis(300))
    }
}
