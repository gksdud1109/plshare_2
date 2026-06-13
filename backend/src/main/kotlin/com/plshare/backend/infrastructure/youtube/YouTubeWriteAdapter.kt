package com.plshare.backend.infrastructure.youtube

import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.UUID

// ─── DTOs ─────────────────────────────────────────────────────────────────────

/**
 * YouTube Music 플레이리스트 생성 결과.
 * [externalId]: YouTube playlistId (PL…)
 * [externalUrl]: https://music.youtube.com/playlist?list={id}
 */
data class YouTubeMusicPlaylistRef(
    val externalId: String,
    val externalUrl: String
)

/**
 * 단일 트랙 내보내기 결과.
 * [videoId]: 대상 videoId (null이면 failed 처리됨)
 * [status]: "added" | "failed"
 * [reason]: 실패 사유 (videoId 없음, API 오류 등)
 */
data class YouTubeMusicTrackResult(
    val videoId: String?,
    val title: String,
    val status: String, // "added" | "failed"
    val reason: String? = null
)

/**
 * 플레이리스트 전체 내보내기 결과.
 */
data class YouTubeMusicExportResult(
    val playlistRef: YouTubeMusicPlaylistRef,
    val added: Int,
    val failed: Int,
    val results: List<YouTubeMusicTrackResult>
)

// ─── 인터페이스 ────────────────────────────────────────────────────────────────

/**
 * YouTube Data API v3 write 어댑터 인터페이스.
 *
 * 구현체:
 *   - [MockYouTubeWriteAdapter]: @Profile("demo") @Primary — 실 API 없이 데모 동작.
 *   - [RealYouTubeWriteAdapter]: @Profile("!demo") — 실제 YouTube Data API v3 호출.
 *
 * quota 비용 (platform-strategy-v2-research §3 참조):
 *   - playlists.insert  = 50 unit
 *   - playlistItems.insert = 50 unit / 곡
 *   → 30곡 1건 ≈ 1,550u. 기본 10,000u/일 → ~6건/일.
 *   [YouTubeQuotaGuard.reserve]가 사전에 예산을 확인하고 초과 시 QUOTA_EXCEEDED를 던진다.
 */
interface YouTubeWriteAdapter {

    /**
     * YouTube 플레이리스트 생성.
     * quota: playlists.insert = 50 unit
     *
     * @param title 플레이리스트 제목
     * @param description 플레이리스트 설명 (nullable)
     * @param accessToken 사용자 OAuth2 accessToken
     * @return 생성된 플레이리스트 참조
     */
    fun createPlaylist(title: String, description: String?, accessToken: String): YouTubeMusicPlaylistRef

    /**
     * 플레이리스트에 단일 트랙 추가.
     * quota: playlistItems.insert = 50 unit
     *
     * 실패 시 [ApiException](UPSTREAM_ERROR)을 던진다.
     * 429(rateLimitExceeded) / 403(quotaExceeded) 응답은 reason을 구분해 메시지에 포함한다.
     *
     * @param playlistId YouTube playlistId
     * @param videoId YouTube videoId
     * @param accessToken 사용자 OAuth2 accessToken
     */
    fun addItem(playlistId: String, videoId: String, accessToken: String)
}

// ─── Mock 구현체 ───────────────────────────────────────────────────────────────

/**
 * YouTube write 어댑터 데모 구현체.
 *
 * - @Profile("demo") @Primary: 실 API 키 없이 개발/데모 환경에서 동작.
 * - createPlaylist: 가짜 playlistId 반환 (ytm-xxxxxxxx 형식).
 * - addItem: videoId가 null이거나 "fail_" prefix면 실패 시뮬레이션.
 *   실제 테스트용 videoId(kpop_v*, lofi_v*)는 성공.
 * - 약간의 지연(createPlaylist 800ms, addItem 200ms)으로 실제 API 응답 패턴 모사.
 */
@Component
@Profile("demo")
@Primary
class MockYouTubeWriteAdapter : YouTubeWriteAdapter {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun createPlaylist(title: String, description: String?, accessToken: String): YouTubeMusicPlaylistRef {
        Thread.sleep(800)
        val id = "ytm-${UUID.randomUUID().toString().take(8)}"
        log.debug("[Mock] YouTube createPlaylist title={} → id={}", title, id)
        return YouTubeMusicPlaylistRef(
            externalId = id,
            externalUrl = "https://music.youtube.com/playlist?list=$id"
        )
    }

    override fun addItem(playlistId: String, videoId: String, accessToken: String) {
        Thread.sleep(200)
        // videoId가 "fail_" prefix이면 실패 시뮬레이션 (테스트/데모용)
        if (videoId.startsWith("fail_")) {
            log.debug("[Mock] YouTube addItem FAILED playlistId={} videoId={}", playlistId, videoId)
            throw ApiException(ErrorCode.UPSTREAM_ERROR, "Mock: simulated addItem failure for videoId=$videoId")
        }
        log.debug("[Mock] YouTube addItem OK playlistId={} videoId={}", playlistId, videoId)
    }
}

// ─── Real 구현체 ───────────────────────────────────────────────────────────────

/**
 * YouTube Data API v3 write 어댑터 실제 구현체 (prod profile).
 *
 * 인증: Authorization: Bearer {accessToken} (사용자 OAuth2 토큰, 파라미터로 전달)
 *
 * 사용 endpoint:
 *   - POST /youtube/v3/playlists?part=snippet,status  (playlists.insert)
 *   - POST /youtube/v3/playlistItems?part=snippet      (playlistItems.insert)
 *
 * 오류 처리:
 *   - 429 (rateLimitExceeded) → ApiException(UPSTREAM_ERROR, "rate_limit_exceeded")
 *   - 403 with reason=quotaExceeded → ApiException(UPSTREAM_ERROR, "quota_exceeded_upstream")
 *     ※ 이는 YouTube API 측 quota 오류이며 [YouTubeQuotaGuard]의 사전 차단과는 별개.
 *   - 기타 4xx/5xx → ApiException(UPSTREAM_ERROR)
 *
 * TODO(후속): search.list(100u) 기반 videoId 매칭은 현재 범위 외.
 *   소스가 YouTube가 아닌 트랙은 ExportService에서 failed 처리된다.
 */
@Component
@Profile("!demo")
class RealYouTubeWriteAdapter(
    webClientBuilder: WebClient.Builder
) : YouTubeWriteAdapter {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val webClient: WebClient = webClientBuilder
        .baseUrl(API_BASE_URL)
        .build()

    override fun createPlaylist(title: String, description: String?, accessToken: String): YouTubeMusicPlaylistRef {
        val body = mapOf(
            "snippet" to mapOf(
                "title" to title,
                "description" to (description ?: "")
            ),
            "status" to mapOf(
                "privacyStatus" to "private"
            )
        )
        return try {
            val json = webClient.post()
                .uri("/youtube/v3/playlists?part=snippet,status")
                .header("Authorization", "Bearer $accessToken")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode::class.java)
                .block(Duration.ofSeconds(10))
                ?: throw ApiException(ErrorCode.UPSTREAM_ERROR, "YouTube playlists.insert returned empty response")

            val id = json.path("id").asText("")
            if (id.isBlank()) throw ApiException(ErrorCode.UPSTREAM_ERROR, "YouTube playlists.insert returned no id")
            YouTubeMusicPlaylistRef(
                externalId = id,
                externalUrl = "https://music.youtube.com/playlist?list=$id"
            )
        } catch (e: WebClientResponseException) {
            throw mapWebClientException(e, "playlists.insert")
        }
    }

    override fun addItem(playlistId: String, videoId: String, accessToken: String) {
        val body = mapOf(
            "snippet" to mapOf(
                "playlistId" to playlistId,
                "resourceId" to mapOf(
                    "kind" to "youtube#video",
                    "videoId" to videoId
                )
            )
        )
        try {
            webClient.post()
                .uri("/youtube/v3/playlistItems?part=snippet")
                .header("Authorization", "Bearer $accessToken")
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block(Duration.ofSeconds(10))
        } catch (e: WebClientResponseException) {
            throw mapWebClientException(e, "playlistItems.insert")
        }
    }

    /**
     * WebClientResponseException을 ApiException(UPSTREAM_ERROR)으로 변환.
     * 429(rateLimitExceeded)와 403(quotaExceeded reason)을 구분해 메시지에 포함한다.
     */
    private fun mapWebClientException(e: WebClientResponseException, operation: String): ApiException {
        val reason = extractErrorReason(e)
        val message = when {
            e.statusCode == HttpStatus.TOO_MANY_REQUESTS -> "YouTube $operation rate_limit_exceeded (reason=$reason)"
            e.statusCode == HttpStatus.FORBIDDEN && reason == "quotaExceeded" ->
                "YouTube $operation quota_exceeded_upstream — API side quota exhausted"
            else -> "YouTube $operation failed: HTTP ${e.statusCode.value()} reason=$reason"
        }
        log.warn("[Real] YouTube {} error: {}", operation, message)
        return ApiException(ErrorCode.UPSTREAM_ERROR, message, e)
    }

    /** YouTube 오류 응답 JSON에서 errors[0].reason 추출. 없으면 "unknown". */
    private fun extractErrorReason(e: WebClientResponseException): String {
        return try {
            val json = com.fasterxml.jackson.databind.ObjectMapper().readTree(e.responseBodyAsString)
            json.path("error").path("errors").path(0).path("reason").asText("unknown")
        } catch (_: Exception) {
            "unknown"
        }
    }

    companion object {
        const val API_BASE_URL = "https://www.googleapis.com"
    }
}
