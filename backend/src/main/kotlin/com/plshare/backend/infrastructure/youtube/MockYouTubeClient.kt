package com.plshare.backend.infrastructure.youtube

import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * YouTube 읽기 어댑터 데모 구현체.
 *
 * - @Profile("demo") / @Primary : 실제 API 키 없이 개발/데모 환경에서 동작.
 * - 픽스처 데이터 설계 의도:
 *     1) "K-Pop Drive" — ISRC null, cover/live 노이즈 트랙 의도 포함 (fuzzy/low-confidence 경로 검증용)
 *     2) "Lo-fi Study"  — ISRC null, 정상 트랙
 *   YouTube Data API v3 현실: track ISRC 미제공 → 모든 mock 트랙도 ISRC null.
 */
@Component
@Primary
@Profile("demo")
class MockYouTubeClient : YouTubeClient {

    // ─── fixture data ─────────────────────────────────────────────────────────

    private val playlists: List<YouTubePlaylistSummary> = listOf(
        buildPlaylist(
            id = "PLytm_kpop_drive_demo001",
            title = "K-Pop Drive",
            coverUrl = "https://picsum.photos/seed/kpopdrive/600/600",
            rawTracks = listOf(
                // (videoId, title, artistHint, durationMs)
                Track("kpop_v001", "ELEVEN", "IVE", 185_000),
                Track("kpop_v002", "After LIKE", "IVE", 192_000),
                // 커버 버전 — 의도적 노이즈. normalizeTitle 이 "[Cover]" 를 제거하지 못해
                // fuzzy score 가 낮아질 수 있으며 새 canonical이 생성될 수 있다. Low Confidence 경로 검증용.
                Track("kpop_v003", "Hype Boy [Cover ver.]", "Various Artists", 200_000),
                // 라이브 버전 — "(Live at Seoul)" 노이즈.
                // normalizeTitle 의 live 패턴이 매칭되면 제거되어 fuzzy 매칭 가능.
                Track("kpop_v004", "Attention (Live at Seoul Fest)", "NewJeans", 195_000),
                Track("kpop_v005", "Ditto", "NewJeans", 253_000),
                Track("kpop_v006", "Love Dive", "IVE", 190_000)
            )
        ),
        buildPlaylist(
            id = "PLytm_lofi_study_demo002",
            title = "Lo-fi Study",
            coverUrl = "https://picsum.photos/seed/lofistudy/600/600",
            rawTracks = listOf(
                Track("lofi_v001", "Coffee Shop Vibes", "ChillHop Music", 180_000),
                Track("lofi_v002", "Late Night Coding", "Lo-Fi Beats", 195_000),
                Track("lofi_v003", "Rainy Window", "Dreamy Beats", 210_000),
                Track("lofi_v004", "Calm Focus", "ChillHop Music", 225_000),
                Track("lofi_v005", "Library Ambience", "StudyBeats", 240_000)
            )
        )
    )

    private data class Track(
        val videoId: String,
        val title: String,
        val artistHint: String,
        val durationMs: Long
    )

    private fun buildPlaylist(
        id: String,
        title: String,
        coverUrl: String,
        rawTracks: List<Track>
    ): YouTubePlaylistSummary = YouTubePlaylistSummary(
        id = id,
        title = title,
        coverUrl = coverUrl,
        items = rawTracks.map { t ->
            YouTubeTrackItem(
                videoId = t.videoId,
                title = t.title,
                artistHint = t.artistHint,
                durationMs = t.durationMs
            )
        }
    )

    // ─── interface impl ───────────────────────────────────────────────────────

    override fun listUserPlaylists(accessToken: String): Mono<List<YouTubePlaylistSummary>> =
        Mono.just(playlists)

    override fun getPlaylist(playlistId: String, accessToken: String): Mono<YouTubePlaylistItem> {
        val summary = playlists.firstOrNull { it.id == playlistId }
            ?: return Mono.error(IllegalArgumentException("Mock YouTube playlist not found: $playlistId"))
        return Mono.just(
            YouTubePlaylistItem(
                id = summary.id,
                snippet = YouTubePlaylistSnippet(
                    title = summary.title,
                    thumbnails = summary.coverUrl?.let { url ->
                        mapOf("high" to YouTubeThumbnail(url))
                    }
                )
            )
        )
    }

    override fun getPlaylistItems(playlistId: String, accessToken: String): Mono<List<YouTubePlaylistItemEntry>> {
        val summary = playlists.firstOrNull { it.id == playlistId }
            ?: return Mono.error(IllegalArgumentException("Mock YouTube playlist not found: $playlistId"))
        return Mono.just(
            summary.items.map { track ->
                YouTubePlaylistItemEntry(
                    snippet = YouTubePlaylistItemSnippet(
                        title = track.title,
                        resourceId = YouTubeResourceId(kind = "youtube#video", videoId = track.videoId),
                        channelTitle = track.artistHint
                    )
                )
            }
        )
    }

    override fun getVideoDetails(videoIds: List<String>, accessToken: String): Mono<List<YouTubeVideoItem>> {
        val allItems = playlists.flatMap { it.items }.associateBy { it.videoId }
        val results = videoIds.mapNotNull { id ->
            allItems[id]?.let { track ->
                YouTubeVideoItem(
                    id = id,
                    snippet = YouTubeVideoSnippet(
                        title = track.title,
                        channelTitle = track.artistHint
                    ),
                    contentDetails = YouTubeContentDetails(
                        // mock은 ms를 역산해 ISO 8601로 반환
                        duration = msToDurationIso(track.durationMs)
                    )
                )
            }
        }
        return Mono.just(results)
    }

    // ─── private helpers ──────────────────────────────────────────────────────

    /** Long ms → ISO 8601 PT string (mock 전용 역변환 헬퍼). */
    private fun msToDurationIso(ms: Long): String {
        val totalSeconds = ms / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return buildString {
            append("PT")
            if (h > 0) append("${h}H")
            if (m > 0) append("${m}M")
            if (s > 0 || (h == 0L && m == 0L)) append("${s}S")
        }
    }
}
