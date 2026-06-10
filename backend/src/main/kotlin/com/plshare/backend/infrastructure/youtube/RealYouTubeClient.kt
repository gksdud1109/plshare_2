package com.plshare.backend.infrastructure.youtube

import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * YouTube Data API v3 실 어댑터 (non-demo 프로파일).
 *
 * 기본 URL: https://www.googleapis.com/youtube/v3
 * 인증: Bearer accessToken (파라미터 수신 — 토큰 발급/저장은 이 클래스의 책임 아님).
 *
 * quota 비용 (1일 10,000 unit 기본):
 *   - playlists.list  1 call = 1 unit
 *   - playlistItems.list 1 call = 1 unit (maxResults=50 페이지당)
 *   - videos.list     1 call = 1 unit (ids 최대 50개)
 *
 * 에러 정책:
 *   - 4xx/5xx WebClient 오류 → ApiException(UPSTREAM_ERROR) 변환.
 *   - timeout 10s (list), 15s (전체 플레이리스트 로드).
 */
@Component
@Profile("!demo")
class RealYouTubeClient : YouTubeClient {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val client = WebClient.create(API_BASE)

    // ─── YouTubeClient impl ───────────────────────────────────────────────────

    /**
     * quota: playlists.list 호출 횟수 = ceil(총 플레이리스트 수 / 50) unit.
     * 각 플레이리스트별 items 구성은 추가 호출 없이 snippet 정보만 반환한다
     * (items는 getPlaylistItems 로 지연 조회).
     */
    override fun listUserPlaylists(accessToken: String): Mono<List<YouTubePlaylistSummary>> {
        return fetchAllPlaylistPages(accessToken)
            .map { items ->
                items.map { item ->
                    YouTubePlaylistSummary(
                        id = item.id,
                        title = item.snippet.title,
                        coverUrl = item.snippet.coverUrl,
                        items = emptyList() // 목록 단계에서 tracks 미조회 — getPlaylistItems로 lazy load
                    )
                }
            }
            .timeout(Duration.ofSeconds(15))
            .onErrorMap { it.toUpstreamError("listUserPlaylists") }
    }

    /**
     * quota: playlists.list 1 unit.
     */
    override fun getPlaylist(playlistId: String, accessToken: String): Mono<YouTubePlaylistItem> {
        return client.get()
            .uri { b ->
                b.path("/playlists")
                    .queryParam("part", "snippet")
                    .queryParam("id", playlistId)
                    .queryParam("maxResults", 1)
                    .build()
            }
            .headers { it.setBearerAuth(accessToken) }
            .retrieve()
            .bodyToMono<YouTubePlaylistListResponse>()
            .timeout(Duration.ofSeconds(10))
            .map { resp ->
                resp.items.firstOrNull()
                    ?: throw ApiException(ErrorCode.NOT_FOUND, "YouTube playlist not found: $playlistId")
            }
            .onErrorMap { it.toUpstreamError("getPlaylist($playlistId)") }
    }

    /**
     * quota: playlistItems.list ceil(아이템 수 / 50) unit — nextPageToken 루프 내장.
     */
    override fun getPlaylistItems(playlistId: String, accessToken: String): Mono<List<YouTubePlaylistItemEntry>> {
        return fetchAllPlaylistItemPages(playlistId, accessToken)
            .timeout(Duration.ofSeconds(15))
            .onErrorMap { it.toUpstreamError("getPlaylistItems($playlistId)") }
    }

    /**
     * quota: videos.list ceil(videoIds.size / 50) unit.
     * 50개 초과 시 내부 배치 분할 처리.
     */
    override fun getVideoDetails(videoIds: List<String>, accessToken: String): Mono<List<YouTubeVideoItem>> {
        if (videoIds.isEmpty()) return Mono.just(emptyList())
        val batches = videoIds.chunked(BATCH_SIZE)
        return Flux.fromIterable(batches)
            .flatMapSequential { batch -> fetchVideoBatch(batch, accessToken) }
            .collectList()
            .map { nestedLists -> nestedLists.flatten() }
            .timeout(Duration.ofSeconds(15))
            .onErrorMap { it.toUpstreamError("getVideoDetails(${videoIds.size} ids)") }
    }

    // ─── private helpers ──────────────────────────────────────────────────────

    /** playlists.list 전체 페이지를 수집해 YouTubePlaylistItem 목록으로 반환. */
    private fun fetchAllPlaylistPages(accessToken: String): Mono<List<YouTubePlaylistItem>> {
        fun fetchPage(pageToken: String?): Mono<YouTubePlaylistListResponse> =
            client.get()
                .uri { b ->
                    b.path("/playlists").also { ub ->
                        ub.queryParam("part", "snippet")
                        ub.queryParam("mine", true)
                        ub.queryParam("maxResults", 50)
                        if (pageToken != null) ub.queryParam("pageToken", pageToken)
                    }.build()
                }
                .headers { it.setBearerAuth(accessToken) }
                .retrieve()
                .bodyToMono<YouTubePlaylistListResponse>()
                .timeout(Duration.ofSeconds(10))

        return fetchPage(null).expand { resp ->
            if (resp.nextPageToken != null) fetchPage(resp.nextPageToken) else Mono.empty()
        }.flatMap { resp ->
            Flux.fromIterable(resp.items)
        }.collectList()
    }

    /** playlistItems.list 전체 페이지를 수집해 YouTubePlaylistItemEntry 목록으로 반환. */
    private fun fetchAllPlaylistItemPages(
        playlistId: String,
        accessToken: String
    ): Mono<List<YouTubePlaylistItemEntry>> {
        fun fetchPage(pageToken: String?): Mono<YouTubePlaylistItemListResponse> =
            client.get()
                .uri { b ->
                    b.path("/playlistItems").also { ub ->
                        ub.queryParam("part", "snippet")
                        ub.queryParam("playlistId", playlistId)
                        ub.queryParam("maxResults", 50)
                        if (pageToken != null) ub.queryParam("pageToken", pageToken)
                    }.build()
                }
                .headers { it.setBearerAuth(accessToken) }
                .retrieve()
                .bodyToMono<YouTubePlaylistItemListResponse>()
                .timeout(Duration.ofSeconds(10))

        return fetchPage(null).expand { resp ->
            if (resp.nextPageToken != null) fetchPage(resp.nextPageToken) else Mono.empty()
        }.flatMap { resp ->
            Flux.fromIterable(resp.items)
        }.collectList()
    }

    /** videos.list 단일 배치(최대 50개) 조회. */
    private fun fetchVideoBatch(videoIds: List<String>, accessToken: String): Mono<List<YouTubeVideoItem>> =
        client.get()
            .uri { b ->
                b.path("/videos")
                    .queryParam("part", "snippet,contentDetails")
                    .queryParam("id", videoIds.joinToString(","))
                    .queryParam("maxResults", 50)
                    .build()
            }
            .headers { it.setBearerAuth(accessToken) }
            .retrieve()
            .bodyToMono<YouTubeVideoListResponse>()
            .timeout(Duration.ofSeconds(10))
            .map { it.items }

    /**
     * WebClient 예외를 ApiException(UPSTREAM_ERROR)로 변환.
     * 이미 ApiException이면 그대로 재전파한다.
     */
    private fun Throwable.toUpstreamError(context: String): Throwable {
        if (this is ApiException) return this
        log.error("YouTube API error during {}: {}", context, message, this)
        return ApiException(ErrorCode.UPSTREAM_ERROR, "YouTube API 호출 실패: $context — ${message}")
    }

    companion object {
        private const val API_BASE = "https://www.googleapis.com/youtube/v3"
        private const val BATCH_SIZE = 50
    }
}
