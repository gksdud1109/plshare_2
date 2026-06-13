package com.plshare.backend.global.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class ApplicationPrincipal(
    val userId: UUID,
    val spotifyGrantId: UUID? = null,
)

@Service
class ApplicationSessionService(
    @Value("\${app.session-secret:demo-session-secret-change-me}")
    private val secret: String,
    @Value("\${app.session-ttl-seconds:2592000}")
    private val ttlSeconds: Long,
    private val environment: Environment? = null,
) {
    init {
        if (environment != null && !environment.acceptsProfiles(Profiles.of("demo"))) {
            require(secret != DEFAULT_DEMO_SECRET && secret.length >= 32) {
                "APP_SESSION_SECRET must be set to at least 32 characters outside the demo profile"
            }
        }
    }

    fun issue(userId: UUID, spotifyGrantId: UUID? = null): String {
        val expiresAt = Instant.now().epochSecond + ttlSeconds
        val payload = listOf(userId, spotifyGrantId ?: "", expiresAt).joinToString("|")
        val encoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.toByteArray(StandardCharsets.UTF_8))
        return "$encoded.${sign(encoded)}"
    }

    fun verify(token: String): ApplicationPrincipal? {
        val parts = token.split('.')
        if (parts.size != 2 || !constantTimeEquals(sign(parts[0]), parts[1])) return null

        val payload = runCatching {
            String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8)
        }.getOrNull() ?: return null
        val fields = payload.split('|')
        if (fields.size != 3) return null

        val userId = runCatching { UUID.fromString(fields[0]) }.getOrNull() ?: return null
        val grantId = fields[1].takeIf { it.isNotBlank() }
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() ?: return null }
        val expiresAt = fields[2].toLongOrNull() ?: return null
        if (expiresAt <= Instant.now().epochSecond) return null

        return ApplicationPrincipal(userId, grantId)
    }

    private fun sign(value: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(mac.doFinal(value.toByteArray(StandardCharsets.UTF_8)))
    }

    private fun constantTimeEquals(left: String, right: String): Boolean {
        return MessageDigest.isEqual(
            left.toByteArray(StandardCharsets.UTF_8),
            right.toByteArray(StandardCharsets.UTF_8),
        )
    }

    companion object {
        private const val DEFAULT_DEMO_SECRET = "demo-session-secret-change-me"
    }
}
