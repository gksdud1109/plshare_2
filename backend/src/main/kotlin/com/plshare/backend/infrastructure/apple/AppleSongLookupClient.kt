package com.plshare.backend.infrastructure.apple

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * Looks up Apple Music catalog song IDs by ISRC.
 *
 * Apple's catalog endpoint accepts a comma-separated list of ISRCs in the
 * `filter[isrc]` query parameter, so [lookupBatch] is implemented as a single HTTP call
 * regardless of input size (callers should still chunk to keep URL length sensible).
 */
@Component
@Profile("!demo")
class AppleSongLookupClient(
    private val tokenProvider: AppleDeveloperTokenProvider,
    private val config: AppleMusicConfig,
    webClientBuilder: WebClient.Builder
) {
    private val webClient: WebClient = webClientBuilder
        .baseUrl(API_BASE_URL)
        .build()

    fun lookup(isrc: String): Mono<String?> =
        lookupBatch(listOf(isrc)).map { it[isrc] }

    fun lookupBatch(isrcs: List<String>): Mono<Map<String, String>> {
        if (isrcs.isEmpty()) return Mono.just(emptyMap())
        val joined = isrcs.joinToString(",")
        return webClient.get()
            .uri { builder ->
                builder.path("/v1/catalog/{storefront}/songs")
                    .queryParam("filter[isrc]", joined)
                    .build(config.storefront)
            }
            .header("Authorization", "Bearer ${tokenProvider.provide()}")
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .map { json -> extractIsrcToId(json) }
    }

    private fun extractIsrcToId(json: JsonNode): Map<String, String> {
        val data = json.path("data")
        if (!data.isArray) return emptyMap()
        val out = mutableMapOf<String, String>()
        data.forEach { node ->
            val id = node.path("id").asText(null) ?: return@forEach
            // Apple returns the isrc inside attributes.isrc
            val isrc = node.path("attributes").path("isrc").asText(null) ?: return@forEach
            out.putIfAbsent(isrc, id)
        }
        return out
    }

    companion object {
        const val API_BASE_URL = "https://api.music.apple.com"
    }
}
