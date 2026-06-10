package com.plshare.backend.domain.user.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents a plshare user identity.
 *
 * Identity resolution strategy:
 * - Primary key: [id] (UUID, stable across all auth methods).
 * - Google login is resolved via [googleSubject] (immutable "sub" claim from Google).
 * - [email] is nullable because anonymous/legacy paths may not have one yet.
 * - [handle] is a unique URL-safe slug used for public profile links.
 *
 * Invariant: if [googleSubject] is set it must be unique across the table (enforced
 * via DB unique index). Upsert logic in [com.plshare.backend.domain.auth.service.GoogleAuthService]
 * must always look up by googleSubject before inserting to prevent duplicates.
 */
@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_users_email", columnList = "email", unique = true),
        Index(name = "idx_users_handle", columnList = "handle", unique = true),
        Index(name = "idx_users_google_subject", columnList = "google_subject", unique = true)
    ]
)
class User(
    @Id
    val id: UUID = UUID.randomUUID(),

    /** The user's email address. Nullable because not every auth path supplies one. */
    @Column(nullable = true, unique = true, length = 255)
    var email: String? = null,

    /** Display name shown in the UI. Defaults to the name from the OAuth provider. */
    @Column(name = "display_name", nullable = false, length = 255)
    var displayName: String,

    /**
     * URL-safe unique handle for public profiles, e.g. "jane.doe".
     * Auto-generated from displayName if not explicitly set.
     * Unique constraint ensures no collisions.
     */
    @Column(nullable = false, unique = true, length = 64)
    var handle: String,

    /** Avatar image URL sourced from the OAuth provider's profile picture. */
    @Column(name = "avatar_url", length = 1024)
    var avatarUrl: String? = null,

    /**
     * Google account "sub" claim — immutable identifier issued by Google.
     * Nullable: set only once Google login is performed.
     * Used as the stable join key for Google OAuth upsert.
     */
    @Column(name = "google_subject", unique = true, length = 255)
    var googleSubject: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
