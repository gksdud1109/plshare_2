package com.plshare.backend.infrastructure.youtube

import com.fasterxml.jackson.annotation.JsonProperty

// ─── playlists.list ───────────────────────────────────────────────────────────

data class YouTubePlaylistListResponse(
    @JsonProperty("nextPageToken") val nextPageToken: String? = null,
    @JsonProperty("items") val items: List<YouTubePlaylistItem> = emptyList()
)

data class YouTubePlaylistItem(
    @JsonProperty("id") val id: String,
    @JsonProperty("snippet") val snippet: YouTubePlaylistSnippet
)

data class YouTubePlaylistSnippet(
    @JsonProperty("title") val title: String,
    @JsonProperty("thumbnails") val thumbnails: Map<String, YouTubeThumbnail>? = null
) {
    /** best-effort cover URL: high → medium → default → null */
    val coverUrl: String?
        get() = thumbnails?.let {
            it["high"]?.url ?: it["medium"]?.url ?: it["default"]?.url
        }
}

data class YouTubeThumbnail(
    @JsonProperty("url") val url: String
)

// ─── playlistItems.list ───────────────────────────────────────────────────────

data class YouTubePlaylistItemListResponse(
    @JsonProperty("nextPageToken") val nextPageToken: String? = null,
    @JsonProperty("items") val items: List<YouTubePlaylistItemEntry> = emptyList()
)

data class YouTubePlaylistItemEntry(
    @JsonProperty("snippet") val snippet: YouTubePlaylistItemSnippet
)

data class YouTubePlaylistItemSnippet(
    /** title of the video within the playlist */
    @JsonProperty("title") val title: String,
    @JsonProperty("resourceId") val resourceId: YouTubeResourceId,
    @JsonProperty("videoOwnerChannelTitle") val channelTitle: String? = null
)

data class YouTubeResourceId(
    @JsonProperty("kind") val kind: String,
    @JsonProperty("videoId") val videoId: String
)

// ─── videos.list(contentDetails) ─────────────────────────────────────────────

data class YouTubeVideoListResponse(
    @JsonProperty("nextPageToken") val nextPageToken: String? = null,
    @JsonProperty("items") val items: List<YouTubeVideoItem> = emptyList()
)

data class YouTubeVideoItem(
    @JsonProperty("id") val id: String,
    @JsonProperty("snippet") val snippet: YouTubeVideoSnippet? = null,
    @JsonProperty("contentDetails") val contentDetails: YouTubeContentDetails? = null
)

data class YouTubeVideoSnippet(
    @JsonProperty("title") val title: String,
    @JsonProperty("channelTitle") val channelTitle: String? = null
)

data class YouTubeContentDetails(
    /**
     * ISO 8601 duration string, e.g. "PT3M30S", "PT1H2M3S", "PT45S".
     * 변환은 [parseDurationMs] 로 처리한다.
     */
    @JsonProperty("duration") val duration: String? = null
)

// ─── domain-level summary returned by YouTubeClient ──────────────────────────

/**
 * YouTubeClient이 반환하는 플레이리스트 단위 요약 뷰.
 * Spotify의 SpotifyPlaylistResponse에 대응하는 YouTube 측 DTO.
 */
data class YouTubePlaylistSummary(
    val id: String,
    val title: String,
    val coverUrl: String?,
    val items: List<YouTubeTrackItem>
)

/**
 * 플레이리스트 아이템 + 비디오 상세를 병합한 트랙 뷰.
 *
 * - [videoId]   : YouTube videoId — MatchInput.sourceId 로 사용된다.
 * - [title]     : 비디오 제목 (가수명 포함 형태가 많음 — "Artist - Song Title").
 * - [artistHint]: channelTitle 기반 아티스트 힌트 (YTM 현실 반영, 정확도 낮음).
 * - [durationMs]: ISO 8601 파싱 결과 (ms). 파싱 실패 시 0L.
 * - ISRC 는 YouTube Data API v3에서 제공되지 않아 항상 null.
 *   → MatchingEngine은 fuzzy fallback을 사용해야 한다.
 */
data class YouTubeTrackItem(
    val videoId: String,
    val title: String,
    val artistHint: String?,
    val durationMs: Long
)

// ─── ISO 8601 duration util ───────────────────────────────────────────────────

/**
 * YouTube contentDetails.duration 에서 오는 ISO 8601 duration(PT형식)을 밀리초로 변환.
 *
 * 지원 패턴 예시:
 *   "PT3M30S"   → 210_000 ms
 *   "PT1H2M3S"  → 3_723_000 ms
 *   "PT45S"     → 45_000 ms
 *   "PT1H"      → 3_600_000 ms
 *   "P0D" / ""  → 0 ms (라이브 스트림/프리미어 등 duration 없는 케이스)
 *
 * @return 밀리초 단위 Long (파싱 불가 시 0L)
 */
fun parseDurationMs(iso8601: String?): Long {
    if (iso8601.isNullOrBlank()) return 0L
    // 패턴: PT[nH][nM][nS] — hours/minutes/seconds 각각 선택적
    val regex = Regex("""^PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?$""")
    val match = regex.matchEntire(iso8601) ?: return 0L
    val hours = match.groupValues[1].toLongOrNull() ?: 0L
    val minutes = match.groupValues[2].toLongOrNull() ?: 0L
    val seconds = match.groupValues[3].toLongOrNull() ?: 0L
    return (hours * 3600 + minutes * 60 + seconds) * 1000L
}

/**
 * YouTube videoId 를 정규화한다.
 *
 * YouTube videoId 는 11문자 Base64url 문자열 (영문대소문자, 숫자, '-', '_').
 * URL 형태로 입력될 수 있으므로 표준 URL 파라미터 "v=" 와 short URL 경로를 처리한다.
 *
 * 예시:
 *   "dQw4w9WgXcQ"                          → "dQw4w9WgXcQ"
 *   "https://www.youtube.com/watch?v=dQw4w9WgXcQ" → "dQw4w9WgXcQ"
 *   "https://youtu.be/dQw4w9WgXcQ"        → "dQw4w9WgXcQ"
 *
 * @return 정규화된 videoId. 인식되지 않는 입력이면 원본 그대로 반환 (오류 미발생).
 */
fun normalizeVideoId(input: String): String {
    val trimmed = input.trim()
    // already bare videoId (11-char base64url)
    if (Regex("""^[A-Za-z0-9_\-]{11}$""").matches(trimmed)) return trimmed
    // ?v= param
    val vParam = Regex("""[?&]v=([A-Za-z0-9_\-]{11})""").find(trimmed)?.groupValues?.get(1)
    if (vParam != null) return vParam
    // youtu.be/ID or /embed/ID or /shorts/ID
    val pathId = Regex("""/(?:embed|shorts|youtu\.be)/([A-Za-z0-9_\-]{11})""").find(trimmed)?.groupValues?.get(1)
    if (pathId != null) return pathId
    return trimmed
}
