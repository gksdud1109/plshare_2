package com.plshare.backend.domain.catalog.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.plshare.backend.domain.catalog.dto.YouTubeCatalogSearchResultDto
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import com.plshare.backend.infrastructure.youtube.YouTubeQuotaGuard
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.HtmlUtils
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class YouTubeCatalogCandidate(
    val videoId: String,
    val title: String,
    val channelTitle: String?,
    val thumbnailUrl: String?,
)

/**
 * API-key catalog search boundary. The production implementation uses only
 * `YOUTUBE_API_KEY`; user OAuth access tokens and YouTube account scopes never
 * enter this path.
 */
interface YouTubeCatalogSearchGateway {
    val quotaCost: Long

    fun validateConfiguration()
    fun search(query: String, maxResults: Int): List<YouTubeCatalogCandidate>
}

@Service
class YouTubeCatalogSearchService(
    private val gateway: YouTubeCatalogSearchGateway,
    private val quotaGuard: YouTubeQuotaGuard,
    private val selectionTokenCodec: YouTubeCatalogSelectionTokenCodec,
) {
    fun search(rawQuery: String): List<YouTubeCatalogSearchResultDto> {
        val query = rawQuery.trim()
        if (query.isEmpty() || query.length > MAX_QUERY_LENGTH) {
            throw ApiException(
                ErrorCode.VALIDATION_FAILED,
                "검색어는 1~${MAX_QUERY_LENGTH}자여야 합니다",
            )
        }

        gateway.validateConfiguration()
        if (gateway.quotaCost > 0) {
            // YouTube search.list is charged 100 units per call. Reserve before
            // making the upstream request so exhausted local budget never leaks
            // an unaccounted call.
            quotaGuard.reserve(gateway.quotaCost)
        }

        return gateway.search(query, MAX_RESULTS)
            .asSequence()
            .distinctBy { it.videoId }
            .take(MAX_RESULTS)
            .map { candidate ->
                validateUpstreamCandidate(candidate)
                YouTubeCatalogSearchResultDto(
                    selectionId = selectionTokenCodec.issue(candidate),
                    videoId = candidate.videoId,
                    title = candidate.title,
                    channelTitle = candidate.channelTitle,
                    thumbnailUrl = candidate.thumbnailUrl,
                )
            }
            .toList()
    }

    private fun validateUpstreamCandidate(candidate: YouTubeCatalogCandidate) {
        if (!VIDEO_ID_PATTERN.matches(candidate.videoId) ||
            candidate.title.isBlank() ||
            candidate.title.length > MAX_TITLE_LENGTH ||
            (candidate.channelTitle?.length ?: 0) > MAX_CHANNEL_LENGTH ||
            (candidate.thumbnailUrl?.length ?: 0) > MAX_THUMBNAIL_URL_LENGTH
        ) {
            throw ApiException(ErrorCode.UPSTREAM_ERROR, "YouTube 검색 응답 형식이 올바르지 않습니다")
        }
    }

    companion object {
        const val MAX_QUERY_LENGTH = 120
        const val MAX_RESULTS = 10
        const val MAX_TITLE_LENGTH = 300
        const val MAX_CHANNEL_LENGTH = 200
        const val MAX_THUMBNAIL_URL_LENGTH = 1_024
        val VIDEO_ID_PATTERN = Regex("""^[A-Za-z0-9_-]{11}$""")
    }
}

/**
 * Stable, integrity-protected selection representation used by asset compose.
 *
 * Format: `yt1.<base64url-json>.<hmac-sha256>`.
 *
 * The payload deliberately contains the server-observed video metadata. Compose
 * verifies the HMAC and reconstructs the Track from this payload, so arbitrary
 * client-supplied title/channel/videoId metadata is never trusted. No timestamp
 * is included: the same upstream result produces the same stable selection ID.
 * The token is an integrity mechanism, not an authorization credential.
 */
@Component
class YouTubeCatalogSelectionTokenCodec(
    private val objectMapper: ObjectMapper,
    @Value("\${app.session-secret:demo-session-secret-change-me}")
    private val secret: String,
) {
    fun issue(candidate: YouTubeCatalogCandidate): String {
        val payload = YouTubeCatalogSelection(
            videoId = candidate.videoId,
            title = candidate.title,
            channelTitle = candidate.channelTitle,
            thumbnailUrl = candidate.thumbnailUrl,
        )
        val encodedPayload = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload))
        val signedValue = "$VERSION.$encodedPayload"
        return "$signedValue.${URL_ENCODER.encodeToString(sign(signedValue))}"
    }

    fun verify(selectionId: String): YouTubeCatalogSelection {
        if (selectionId.length > MAX_TOKEN_LENGTH) invalidToken()
        val parts = selectionId.split('.', limit = 3)
        if (parts.size != 3 || parts[0] != VERSION) invalidToken()

        val signedValue = "${parts[0]}.${parts[1]}"
        val suppliedSignature = runCatching { URL_DECODER.decode(parts[2]) }.getOrElse { invalidToken() }
        if (!MessageDigest.isEqual(sign(signedValue), suppliedSignature)) invalidToken()

        val payload = runCatching {
            objectMapper.readValue(URL_DECODER.decode(parts[1]), YouTubeCatalogSelection::class.java)
        }.getOrElse { invalidToken() }

        if (!YouTubeCatalogSearchService.VIDEO_ID_PATTERN.matches(payload.videoId) ||
            payload.title.isBlank() ||
            payload.title.length > YouTubeCatalogSearchService.MAX_TITLE_LENGTH ||
            (payload.channelTitle?.length ?: 0) > YouTubeCatalogSearchService.MAX_CHANNEL_LENGTH ||
            (payload.thumbnailUrl?.length ?: 0) > YouTubeCatalogSearchService.MAX_THUMBNAIL_URL_LENGTH
        ) {
            invalidToken()
        }
        return payload
    }

    private fun sign(value: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(value.toByteArray(StandardCharsets.UTF_8))
    }

    private fun invalidToken(): Nothing =
        throw ApiException(ErrorCode.VALIDATION_FAILED, "유효하지 않은 YouTube 검색 선택 ID입니다")

    companion object {
        private const val VERSION = "yt1"
        private const val MAX_TOKEN_LENGTH = 4_096
        private val URL_ENCODER = Base64.getUrlEncoder().withoutPadding()
        private val URL_DECODER = Base64.getUrlDecoder()
    }
}

data class YouTubeCatalogSelection(
    val videoId: String,
    val title: String,
    val channelTitle: String?,
    val thumbnailUrl: String?,
)

@Component
@Profile("!demo")
class ApiKeyYouTubeCatalogSearchGateway(
    @Value("\${youtube.api-key:}") private val apiKey: String,
    private val objectMapper: ObjectMapper,
) : YouTubeCatalogSearchGateway {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val client = WebClient.create(API_BASE)

    override val quotaCost: Long = YouTubeQuotaGuard.SEARCH_COST

    override fun validateConfiguration() {
        if (apiKey.isBlank()) {
            throw ApiException(
                ErrorCode.CONFIGURATION_ERROR,
                "YouTube 카탈로그 검색 설정이 없습니다: YOUTUBE_API_KEY",
            )
        }
    }

    override fun search(query: String, maxResults: Int): List<YouTubeCatalogCandidate> {
        validateConfiguration()
        return try {
            client.get()
                .uri { builder ->
                    builder.path("/search")
                        .queryParam("part", "snippet")
                        .queryParam("type", "video")
                        .queryParam("videoCategoryId", MUSIC_CATEGORY_ID)
                        .queryParam("maxResults", maxResults)
                        .queryParam("q", query)
                        .queryParam("key", apiKey)
                        .build()
                }
                .retrieve()
                .bodyToMono<YouTubeCatalogSearchApiResponse>()
                .timeout(Duration.ofSeconds(10))
                .block()
                ?.items
                .orEmpty()
                .mapNotNull { item ->
                    val videoId = item.id.videoId ?: return@mapNotNull null
                    val snippet = item.snippet ?: return@mapNotNull null
                    YouTubeCatalogCandidate(
                        videoId = videoId,
                        title = HtmlUtils.htmlUnescape(snippet.title.orEmpty()),
                        channelTitle = snippet.channelTitle?.let(HtmlUtils::htmlUnescape),
                        thumbnailUrl = snippet.thumbnailUrl,
                    )
                }
        } catch (exception: ApiException) {
            throw exception
        } catch (exception: WebClientResponseException) {
            throw mapWebClientException(exception)
        } catch (exception: Exception) {
            log.error("YouTube API-key catalog search failed", exception)
            throw ApiException(
                ErrorCode.UPSTREAM_ERROR,
                "YouTube 카탈로그 검색 호출에 실패했습니다",
                exception,
            )
        }
    }

    private fun mapWebClientException(exception: WebClientResponseException): ApiException {
        val reason = runCatching {
            objectMapper.readTree(exception.responseBodyAsString)
                .path("error")
                .path("errors")
                .path(0)
                .path("reason")
                .asText("")
        }.getOrDefault("")
        val quotaExhausted = exception.statusCode == HttpStatus.TOO_MANY_REQUESTS ||
            reason in QUOTA_REASONS

        log.warn(
            "YouTube API-key catalog search failed: status={}, reason={}",
            exception.statusCode.value(),
            reason.ifBlank { "unknown" },
        )
        return if (quotaExhausted) {
            ApiException(ErrorCode.QUOTA_EXCEEDED, "YouTube 검색 쿼터가 소진되었습니다", exception)
        } else {
            ApiException(
                ErrorCode.UPSTREAM_ERROR,
                "YouTube 카탈로그 검색 호출에 실패했습니다",
                exception,
            )
        }
    }

    companion object {
        private const val API_BASE = "https://www.googleapis.com/youtube/v3"
        private const val MUSIC_CATEGORY_ID = "10"
        private val QUOTA_REASONS = setOf("quotaExceeded", "dailyLimitExceeded", "rateLimitExceeded")
    }
}

@Component
@Profile("demo")
class DemoYouTubeCatalogSearchGateway : YouTubeCatalogSearchGateway {
    override val quotaCost: Long = 0

    override fun validateConfiguration() = Unit

    override fun search(query: String, maxResults: Int): List<YouTubeCatalogCandidate> {
        val terms = query.lowercase().split(Regex("""\s+""")).filter { it.isNotBlank() }
        return DEMO_CATALOG
            .filter { candidate ->
                val searchable = "${candidate.title} ${candidate.channelTitle.orEmpty()}".lowercase()
                terms.all(searchable::contains)
            }
            .take(maxResults)
    }

    companion object {
        private val DEMO_CATALOG = listOf(
            demoCandidate("em0MknB6wFo", "Something About Us", "Daft Punk - Topic"),
            demoCandidate("dX3k_QDnzHE", "Midnight City", "M83"),
            demoCandidate("sBzrzS1Ag_g", "The Less I Know The Better", "Tame Impala"),
            demoCandidate("cFElidiwxYU", "1901", "Phoenix"),
            demoCandidate("aygY5OqMuKE", "All My Friends", "LCD Soundsystem"),
        )

        private fun demoCandidate(videoId: String, title: String, channelTitle: String) =
            YouTubeCatalogCandidate(
                videoId = videoId,
                title = title,
                channelTitle = channelTitle,
                thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
            )
    }
}

data class YouTubeCatalogSearchApiResponse(
    val items: List<YouTubeCatalogSearchApiItem> = emptyList(),
)

data class YouTubeCatalogSearchApiItem(
    val id: YouTubeCatalogSearchApiId,
    val snippet: YouTubeCatalogSearchApiSnippet? = null,
)

data class YouTubeCatalogSearchApiId(
    val videoId: String? = null,
)

data class YouTubeCatalogSearchApiSnippet(
    val title: String? = null,
    val channelTitle: String? = null,
    val thumbnails: Map<String, YouTubeCatalogSearchApiThumbnail>? = null,
) {
    val thumbnailUrl: String?
        get() = thumbnails?.let { values ->
            values["high"]?.url ?: values["medium"]?.url ?: values["default"]?.url
        }
}

data class YouTubeCatalogSearchApiThumbnail(
    val url: String,
)
