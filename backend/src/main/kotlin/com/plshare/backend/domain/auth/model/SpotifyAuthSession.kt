package com.plshare.backend.domain.auth.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * In-flight OAuth handshake. Created on `/api/auth/spotify/start`,
 * consumed on `/api/auth/spotify/callback`. TTL ~10 minutes.
 */
@Entity
@Table(
    name = "oauth_handshakes",
    indexes = [Index(name = "idx_oauth_handshake_state", columnList = "state", unique = true)]
)
class OauthHandshake(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true, length = 128)
    val state: String,

    @Column(name = "code_verifier", nullable = false, length = 256)
    val codeVerifier: String,

    @Column(name = "redirect_uri", nullable = false, length = 512)
    val redirectUri: String,

    @Column(name = "user_id")
    val userId: UUID? = null,

    @Column(name = "return_path", length = 1024)
    val returnPath: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "consumed_at")
    var consumedAt: LocalDateTime? = null
)

/**
 * Persisted Spotify access grant for a (currently anonymous) user.
 * Used by `SpotifyAccessGrantService` for transparent refresh.
 */
@Entity
@Table(
    name = "spotify_access_grants",
    indexes = [Index(name = "idx_spotify_grant_user", columnList = "user_id")]
)
class SpotifyAccessGrant(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "access_token", nullable = false, length = 2048)
    var accessToken: String,

    @Column(name = "refresh_token", length = 2048)
    var refreshToken: String?,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,

    @Column(name = "scope", length = 512)
    var scope: String?,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
