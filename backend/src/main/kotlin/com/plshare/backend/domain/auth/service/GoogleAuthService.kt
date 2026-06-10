package com.plshare.backend.domain.auth.service

import com.plshare.backend.domain.user.model.User
import com.plshare.backend.domain.user.repository.UserRepository
import com.plshare.backend.infrastructure.google.GoogleUserInfo
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Handles the User identity lifecycle for Google OAuth.
 *
 * Upsert invariant: a given [GoogleUserInfo.sub] must map to exactly one [User] row.
 * The first login creates the record; subsequent logins update mutable fields (name,
 * avatar) but never create a duplicate. This is enforced by the unique index on
 * `users.google_subject` in addition to the application-level check here.
 *
 * Handle generation: derived from the email local-part (before '@') or from the
 * display name if no email is available. Collisions are resolved by appending a
 * short random suffix — keeping handles human-readable while guaranteeing uniqueness.
 */
@Service
class GoogleAuthService(
    private val userRepository: UserRepository
) {

    /**
     * Upserts a [User] from Google's userinfo.
     * - If [googleSubject] already exists → update mutable fields and return existing user.
     * - If not found → create a new user.
     *
     * @return the persisted [User] (either existing or newly created).
     */
    @Transactional
    fun upsertUser(info: GoogleUserInfo): User {
        val existing = userRepository.findByGoogleSubject(info.sub)
        if (existing != null) {
            // Update mutable profile fields that may change on the Google side.
            existing.displayName = info.name ?: existing.displayName
            existing.avatarUrl = info.picture ?: existing.avatarUrl
            existing.updatedAt = java.time.LocalDateTime.now()
            return userRepository.save(existing)
        }

        val handle = generateUniqueHandle(info)
        val user = User(
            email = info.email,
            displayName = info.name ?: handle,
            handle = handle,
            avatarUrl = info.picture,
            googleSubject = info.sub
        )
        return userRepository.save(user)
    }

    /**
     * Derives a URL-safe handle from the user's email or display name,
     * appending a 4-char suffix when the base handle is already taken.
     */
    private fun generateUniqueHandle(info: GoogleUserInfo): String {
        val base = when {
            !info.email.isNullOrBlank() -> info.email.substringBefore('@')
            !info.name.isNullOrBlank()  -> info.name.lowercase().replace(Regex("[^a-z0-9]"), ".")
            else                        -> "user"
        }.take(48).trimEnd('.')

        val sanitized = base.replace(Regex("[^a-z0-9._-]"), ".").ifEmpty { "user" }

        if (userRepository.findByHandle(sanitized) == null) return sanitized

        // Collision — append random suffix.
        repeat(10) {
            val candidate = "$sanitized-${randomSuffix()}"
            if (userRepository.findByHandle(candidate) == null) return candidate
        }
        // Extremely unlikely fallback.
        return "$sanitized-${System.currentTimeMillis()}"
    }

    private fun randomSuffix(): String =
        (1..4).map { ('a'..'z').random() }.joinToString("")
}
