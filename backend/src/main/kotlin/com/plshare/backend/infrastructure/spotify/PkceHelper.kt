package com.plshare.backend.infrastructure.spotify

import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * PKCE (RFC 7636) helper.
 * - Code verifier: 43-128 chars from the unreserved set [A-Z, a-z, 0-9, '-', '.', '_', '~'].
 * - Code challenge: BASE64URL(SHA256(verifier)) without padding.
 */
@Component
class PkceHelper {

    private val random = SecureRandom()
    private val urlEncoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()

    fun generateCodeVerifier(length: Int = DEFAULT_VERIFIER_LENGTH): String {
        require(length in MIN_LENGTH..MAX_LENGTH) {
            "PKCE code verifier length must be between $MIN_LENGTH and $MAX_LENGTH"
        }
        val sb = StringBuilder(length)
        repeat(length) {
            sb.append(UNRESERVED[random.nextInt(UNRESERVED.length)])
        }
        return sb.toString()
    }

    fun codeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        return urlEncoder.encodeToString(digest)
    }

    fun generateState(): String {
        val bytes = ByteArray(32).also { random.nextBytes(it) }
        return urlEncoder.encodeToString(bytes)
    }

    companion object {
        private const val MIN_LENGTH = 43
        private const val MAX_LENGTH = 128
        private const val DEFAULT_VERIFIER_LENGTH = 64
        private const val UNRESERVED =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    }
}
