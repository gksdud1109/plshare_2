package com.plshare.backend.application.service

import com.plshare.backend.domain.entity.SpotifyAccessGrant
import com.plshare.backend.domain.repository.SpotifyAccessGrantRepository
import com.plshare.backend.infrastructure.spotify.SpotifyClient
import com.plshare.backend.infrastructure.spotify.SpotifyTokenSet
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.UUID

/**
 * Wraps `SpotifyAccessGrant` lifecycle: lookup, transparent refresh of soon-to-expire
 * tokens, and persistence of refreshed values.
 *
 * Existing import flow keeps using `SpotifyClient.getAccessToken()` (client_credentials),
 * so this service only kicks in when an authenticated user-bound token is required.
 */
@Service
class SpotifyAccessGrantService(
    private val grantRepository: SpotifyAccessGrantRepository,
    private val spotifyClient: SpotifyClient
) {

    @Transactional
    fun saveNewGrant(userId: UUID, tokens: SpotifyTokenSet): SpotifyAccessGrant {
        val grant = SpotifyAccessGrant(
            userId = userId,
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
            expiresAt = LocalDateTime.now().plusSeconds(tokens.expiresInSeconds),
            scope = tokens.scope
        )
        return grantRepository.save(grant)
    }

    fun findGrant(grantId: UUID): SpotifyAccessGrant? =
        grantRepository.findById(grantId).orElse(null)

    fun isExpiringSoon(grant: SpotifyAccessGrant, now: LocalDateTime = LocalDateTime.now()): Boolean =
        grant.expiresAt.isBefore(now.plusMinutes(REFRESH_THRESHOLD_MINUTES))

    /**
     * Returns a valid access token for the grant, refreshing transparently if it
     * expires within `REFRESH_THRESHOLD_MINUTES`.
     */
    fun getValidAccessToken(grantId: UUID): Mono<String> {
        val grant = findGrant(grantId)
            ?: return Mono.error(IllegalArgumentException("Grant not found: $grantId"))

        if (!isExpiringSoon(grant)) {
            return Mono.just(grant.accessToken)
        }
        val refreshToken = grant.refreshToken
            ?: return Mono.error(IllegalStateException("No refresh_token on grant $grantId"))

        return spotifyClient.refreshAccessToken(refreshToken)
            .map { tokens -> persistRefresh(grantId, tokens).accessToken }
    }

    @Transactional
    fun persistRefresh(grantId: UUID, tokens: SpotifyTokenSet): SpotifyAccessGrant {
        val grant = grantRepository.findById(grantId).orElseThrow {
            IllegalArgumentException("Grant not found: $grantId")
        }
        grant.accessToken = tokens.accessToken
        if (tokens.refreshToken != null) grant.refreshToken = tokens.refreshToken
        grant.expiresAt = LocalDateTime.now().plusSeconds(tokens.expiresInSeconds)
        if (tokens.scope != null) grant.scope = tokens.scope
        grant.updatedAt = LocalDateTime.now()
        return grantRepository.save(grant)
    }

    companion object {
        private const val REFRESH_THRESHOLD_MINUTES = 5L
    }
}
