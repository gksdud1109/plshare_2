package com.plshare.backend.domain.catalog.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import com.plshare.backend.infrastructure.youtube.QuotaUsageStore
import com.plshare.backend.infrastructure.youtube.YouTubeQuotaGuard
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class YouTubeCatalogSearchServiceTest {

    @Test
    fun `search reserves 100 units before upstream call and caps results`() {
        val quotaStore = InMemoryQuotaUsageStore()
        val quotaGuard = YouTubeQuotaGuard(quotaStore, dailyBudget = 1_000)
        val observedUsageAtUpstreamCall = mutableListOf<Long>()
        val gateway = RecordingSearchGateway(
            candidates = (1..15).map { index ->
                candidate(
                    videoId = "video${index.toString().padStart(6, '0')}",
                    title = "Track $index",
                )
            },
            onSearch = {
                observedUsageAtUpstreamCall += quotaGuard.todayUsed()
            },
        )
        val service = service(gateway, quotaGuard)

        val first = service.search("  track  ")
        val second = service.search("track")

        assertEquals(YouTubeCatalogSearchService.MAX_RESULTS, first.size)
        assertEquals(first.map { it.selectionId }, second.map { it.selectionId })
        assertEquals(YouTubeQuotaGuard.SEARCH_COST * 2, quotaGuard.todayUsed())
        assertEquals(listOf(100L, 200L), observedUsageAtUpstreamCall)
        assertTrue(first.all { it.selectionId.startsWith("yt1.") })
    }

    @Test
    fun `blank and oversized queries are rejected without spending quota`() {
        val quotaStore = InMemoryQuotaUsageStore()
        val quotaGuard = YouTubeQuotaGuard(quotaStore, dailyBudget = 1_000)
        val gateway = RecordingSearchGateway(candidates = listOf(candidate()))
        val service = service(gateway, quotaGuard)

        listOf("", "   ", "x".repeat(YouTubeCatalogSearchService.MAX_QUERY_LENGTH + 1)).forEach { query ->
            val exception = assertThrows(ApiException::class.java) { service.search(query) }
            assertEquals(ErrorCode.VALIDATION_FAILED, exception.code)
        }

        assertEquals(0, gateway.searchCalls)
        assertEquals(0, quotaGuard.todayUsed())
    }

    @Test
    fun `missing API configuration remains a configuration error`() {
        val quotaStore = InMemoryQuotaUsageStore()
        val quotaGuard = YouTubeQuotaGuard(quotaStore, dailyBudget = 1_000)
        val gateway = ApiKeyYouTubeCatalogSearchGateway(
            apiKey = "",
            objectMapper = jacksonObjectMapper(),
        )

        val exception = assertThrows(ApiException::class.java) {
            service(gateway, quotaGuard).search("track")
        }

        assertEquals(ErrorCode.CONFIGURATION_ERROR, exception.code)
        assertEquals(0, quotaGuard.todayUsed())
    }

    @Test
    fun `quota exhaustion prevents the upstream call`() {
        val quotaGuard = YouTubeQuotaGuard(InMemoryQuotaUsageStore(), dailyBudget = 99)
        val gateway = RecordingSearchGateway(candidates = listOf(candidate()))

        val exception = assertThrows(ApiException::class.java) {
            service(gateway, quotaGuard).search("track")
        }

        assertEquals(ErrorCode.QUOTA_EXCEEDED, exception.code)
        assertEquals(0, gateway.searchCalls)
    }

    @Test
    fun `upstream errors are not replaced with demo fixtures`() {
        val quotaGuard = YouTubeQuotaGuard(InMemoryQuotaUsageStore(), dailyBudget = 1_000)
        val gateway = object : YouTubeCatalogSearchGateway {
            override val quotaCost = YouTubeQuotaGuard.SEARCH_COST
            override fun validateConfiguration() = Unit
            override fun search(query: String, maxResults: Int): List<YouTubeCatalogCandidate> {
                throw ApiException(ErrorCode.UPSTREAM_ERROR, "provider failed")
            }
        }

        val exception = assertThrows(ApiException::class.java) {
            service(gateway, quotaGuard).search("track")
        }

        assertEquals(ErrorCode.UPSTREAM_ERROR, exception.code)
    }

    @Test
    fun `selection token rejects tampering`() {
        val codec = tokenCodec()
        val token = codec.issue(candidate())
        val replacement = if (token.last() == 'a') 'b' else 'a'
        val tampered = token.dropLast(1) + replacement

        val exception = assertThrows(ApiException::class.java) { codec.verify(tampered) }

        assertEquals(ErrorCode.VALIDATION_FAILED, exception.code)
    }

    @Test
    fun `demo search is deterministic and does not spend upstream quota`() {
        val quotaStore = InMemoryQuotaUsageStore()
        val quotaGuard = YouTubeQuotaGuard(quotaStore, dailyBudget = 1_000)
        val service = service(DemoYouTubeCatalogSearchGateway(), quotaGuard)

        val first = service.search("midnight city")
        val second = service.search("midnight city")

        assertEquals(1, first.size)
        assertEquals(first, second)
        assertEquals(0, quotaGuard.todayUsed())
    }

    private fun service(
        gateway: YouTubeCatalogSearchGateway,
        quotaGuard: YouTubeQuotaGuard,
    ) = YouTubeCatalogSearchService(gateway, quotaGuard, tokenCodec())

    private fun tokenCodec() =
        YouTubeCatalogSelectionTokenCodec(jacksonObjectMapper(), "test-selection-secret")

    private fun candidate(
        videoId: String = "abcdefghijk",
        title: String = "Track",
    ) = YouTubeCatalogCandidate(
        videoId = videoId,
        title = title,
        channelTitle = "Artist - Topic",
        thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
    )
}

private class RecordingSearchGateway(
    private val candidates: List<YouTubeCatalogCandidate>,
    private val onSearch: () -> Unit = {},
) : YouTubeCatalogSearchGateway {
    override val quotaCost = YouTubeQuotaGuard.SEARCH_COST
    var searchCalls: Int = 0
        private set

    override fun validateConfiguration() = Unit

    override fun search(query: String, maxResults: Int): List<YouTubeCatalogCandidate> {
        searchCalls += 1
        onSearch()
        return candidates
    }
}

private class InMemoryQuotaUsageStore : QuotaUsageStore {
    private val usage = mutableMapOf<String, Long>()

    override fun getUsedUnits(dateStr: String): Long? = usage[dateStr]

    override fun addUnits(dateStr: String, delta: Long) {
        usage[dateStr] = (usage[dateStr] ?: 0) + delta
    }
}
