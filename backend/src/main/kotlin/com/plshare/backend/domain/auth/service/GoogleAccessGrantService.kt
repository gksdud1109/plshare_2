package com.plshare.backend.domain.auth.service

import com.plshare.backend.domain.auth.model.GoogleAccessGrant
import com.plshare.backend.domain.auth.repository.GoogleAccessGrantRepository
import com.plshare.backend.infrastructure.google.GoogleOAuthClient
import com.plshare.backend.infrastructure.google.GoogleTokenSet
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class GoogleAccessGrantService(
    private val grants: GoogleAccessGrantRepository,
    private val googleOAuthClient: GoogleOAuthClient,
) {
    @Transactional
    fun save(userId: UUID, tokens: GoogleTokenSet): GoogleAccessGrant {
        val existing = grants.findFirstByUserIdOrderByUpdatedAtDesc(userId)
        if (existing != null) {
            existing.accessToken = tokens.accessToken
            existing.refreshToken = tokens.refreshToken ?: existing.refreshToken
            existing.expiresAt = LocalDateTime.now().plusSeconds(tokens.expiresInSeconds)
            existing.scope = tokens.scope ?: existing.scope
            existing.updatedAt = LocalDateTime.now()
            return grants.save(existing)
        }
        return grants.save(
            GoogleAccessGrant(
                userId = userId,
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
                expiresAt = LocalDateTime.now().plusSeconds(tokens.expiresInSeconds),
                scope = tokens.scope,
            )
        )
    }

    @Transactional
    fun getValidYouTubeToken(userId: UUID): String {
        val grant = grants.findFirstByUserIdOrderByUpdatedAtDesc(userId)
            ?: throw IllegalStateException("Google access grant not found for user $userId")
        val scopes = grant.scope.orEmpty()
        if (!scopes.contains("youtube")) {
            throw IllegalStateException("YouTube permission has not been granted")
        }
        if (grant.expiresAt.isAfter(LocalDateTime.now().plusMinutes(5))) {
            return grant.accessToken
        }
        val refreshToken = grant.refreshToken
            ?: throw IllegalStateException("Google refresh token not available")
        val refreshed = googleOAuthClient.refreshAccessToken(refreshToken).block()
            ?: throw IllegalStateException("Google token refresh returned no response")
        return save(userId, refreshed).accessToken
    }
}
