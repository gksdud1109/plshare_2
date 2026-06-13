package com.plshare.backend.domain.auth.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "google_access_grants",
    indexes = [Index(name = "idx_google_grant_user", columnList = "user_id")],
)
class GoogleAccessGrant(
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

    @Column(name = "scope", length = 1024)
    var scope: String?,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
