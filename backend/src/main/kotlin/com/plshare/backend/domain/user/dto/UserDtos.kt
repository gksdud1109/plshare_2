package com.plshare.backend.domain.user.dto

import com.plshare.backend.domain.user.model.User
import java.util.UUID

/**
 * Public profile DTO — safe to expose via unauthenticated endpoints.
 * Excludes sensitive fields (email, googleSubject).
 */
data class UserProfileDto(
    val id: UUID,
    val displayName: String,
    val handle: String,
    val avatarUrl: String?
) {
    companion object {
        fun from(user: User) = UserProfileDto(
            id = user.id,
            displayName = user.displayName,
            handle = user.handle,
            avatarUrl = user.avatarUrl
        )
    }
}

/**
 * Private "me" DTO — includes email; should only be served to the authenticated user.
 */
data class UserMeDto(
    val id: UUID,
    val displayName: String,
    val handle: String,
    val avatarUrl: String?,
    val email: String?
) {
    companion object {
        fun from(user: User) = UserMeDto(
            id = user.id,
            displayName = user.displayName,
            handle = user.handle,
            avatarUrl = user.avatarUrl,
            email = user.email
        )
    }
}
