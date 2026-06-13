package com.plshare.backend.global.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.UUID

class ApplicationSessionServiceTest {

    @Test
    fun `issued session preserves user and spotify grant`() {
        val userId = UUID.randomUUID()
        val grantId = UUID.randomUUID()
        val service = ApplicationSessionService("test-secret", 60)

        val principal = service.verify(service.issue(userId, grantId))

        assertNotNull(principal)
        assertEquals(userId, principal?.userId)
        assertEquals(grantId, principal?.spotifyGrantId)
    }

    @Test
    fun `tampered session is rejected`() {
        val service = ApplicationSessionService("test-secret", 60)
        val token = service.issue(UUID.randomUUID())
        val tampered = token.dropLast(1) + if (token.last() == 'a') "b" else "a"

        assertNull(service.verify(tampered))
    }

    @Test
    fun `session signed by another secret is rejected`() {
        val issuer = ApplicationSessionService("issuer-secret", 60)
        val verifier = ApplicationSessionService("verifier-secret", 60)

        assertNull(verifier.verify(issuer.issue(UUID.randomUUID())))
    }

    @Test
    fun `expired session is rejected`() {
        val service = ApplicationSessionService("test-secret", -1)

        assertNull(service.verify(service.issue(UUID.randomUUID())))
    }
}
