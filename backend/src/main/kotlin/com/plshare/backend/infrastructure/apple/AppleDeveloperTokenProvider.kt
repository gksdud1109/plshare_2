package com.plshare.backend.infrastructure.apple

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference

/**
 * Issues and caches Apple MusicKit developer tokens (ES256 JWT) signed with the team's
 * private key. Implementation uses only the JDK `java.security` stack to avoid pulling in
 * extra dependencies — see ADR / adapter-architecture-v0.1.md (Apple Write Adapter section).
 *
 * Token lifetime: 6 months (Apple's stated maximum). Cached tokens are reused until 30
 * minutes before expiry, after which `provide()` re-signs.
 *
 * Credentials are read from [AppleMusicConfig]. If any of teamId / keyId / privateKeyPem
 * is blank, [provide] throws [IllegalStateException] — the failure is intentionally
 * deferred to call time so the application still boots in environments without secrets.
 */
@Component
@Profile("!demo")
class AppleDeveloperTokenProvider(
    private val config: AppleMusicConfig
) {

    private data class CachedToken(val token: String, val expiresAt: Instant)

    private val cache = AtomicReference<CachedToken?>(null)

    fun provide(): String {
        val now = Instant.now()
        val cached = cache.get()
        if (cached != null && now.isBefore(cached.expiresAt.minus(REFRESH_BEFORE))) {
            return cached.token
        }

        val token = sign(now)
        cache.set(CachedToken(token, now.plus(TOKEN_TTL)))
        return token
    }

    private fun sign(now: Instant): String {
        check(config.teamId.isNotBlank() && config.keyId.isNotBlank() && config.privateKeyPem.isNotBlank()) {
            "Apple credentials not configured"
        }

        val header = """{"alg":"ES256","kid":"${config.keyId}","typ":"JWT"}"""
        val payload = """{"iss":"${config.teamId}","iat":${now.epochSecond},"exp":${now.plus(TOKEN_TTL).epochSecond}}"""

        val signingInput = b64Url(header.toByteArray(StandardCharsets.UTF_8)) +
            "." +
            b64Url(payload.toByteArray(StandardCharsets.UTF_8))

        val privateKey = parseEcPrivateKey(config.privateKeyPem)
        val derSignature = Signature.getInstance("SHA256withECDSA").run {
            initSign(privateKey)
            update(signingInput.toByteArray(StandardCharsets.UTF_8))
            sign()
        }
        val joseSignature = derToJose(derSignature)
        return signingInput + "." + b64Url(joseSignature)
    }

    private fun parseEcPrivateKey(pem: String): PrivateKey {
        val normalized = pem.replace("\\n", "\n").trim()
        val body = normalized
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN EC PRIVATE KEY-----", "")
            .replace("-----END EC PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val der = Base64.getDecoder().decode(body)
        val spec = PKCS8EncodedKeySpec(der)
        return KeyFactory.getInstance("EC").generatePrivate(spec) as ECPrivateKey
    }

    /**
     * Convert a DER-encoded ECDSA signature (as produced by `SHA256withECDSA`) to the
     * fixed-length JOSE/IEEE-P1363 form required by JWS ES256: r || s, each padded to 32
     * bytes.
     */
    private fun derToJose(der: ByteArray): ByteArray {
        require(der.size >= 8 && der[0].toInt() == 0x30) { "invalid DER signature" }
        var offset = 2
        if (der[1].toInt() and 0x80 != 0) {
            offset += der[1].toInt() and 0x7f
        }
        require(der[offset].toInt() == 0x02) { "expected INTEGER for r" }
        val rLen = der[offset + 1].toInt()
        val r = BigInteger(1, der.copyOfRange(offset + 2, offset + 2 + rLen))
        offset += 2 + rLen
        require(der[offset].toInt() == 0x02) { "expected INTEGER for s" }
        val sLen = der[offset + 1].toInt()
        val s = BigInteger(1, der.copyOfRange(offset + 2, offset + 2 + sLen))

        val out = ByteArray(64)
        copyToFixed(r.toByteArray(), out, 0, 32)
        copyToFixed(s.toByteArray(), out, 32, 32)
        return out
    }

    private fun copyToFixed(src: ByteArray, dest: ByteArray, destOffset: Int, len: Int) {
        // BigInteger.toByteArray() may include a leading sign byte; trim/pad to `len`.
        val start = if (src.size > len) src.size - len else 0
        val effective = src.size - start
        val padding = len - effective
        for (i in 0 until padding) dest[destOffset + i] = 0
        System.arraycopy(src, start, dest, destOffset + padding, effective)
    }

    private fun b64Url(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    companion object {
        private val TOKEN_TTL: Duration = Duration.ofDays(180)
        private val REFRESH_BEFORE: Duration = Duration.ofMinutes(30)
    }
}
