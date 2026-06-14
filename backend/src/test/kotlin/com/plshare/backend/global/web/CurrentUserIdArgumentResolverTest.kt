package com.plshare.backend.global.web

import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import com.plshare.backend.global.security.ApplicationPrincipal
import com.plshare.backend.global.security.CurrentUserId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.ServletWebRequest
import java.util.UUID

/**
 * [CurrentUserIdArgumentResolver] 단위 테스트.
 *
 * 계약:
 * - `@CurrentUserId` + UUID(또는 UUID?) 파라미터만 지원한다.
 * - principal 있으면 userId 반환.
 * - non-null 파라미터인데 미인증이면 UNAUTHORIZED, nullable 이면 null.
 */
class CurrentUserIdArgumentResolverTest {

    private val resolver = CurrentUserIdArgumentResolver()
    private val webRequest = ServletWebRequest(MockHttpServletRequest())

    @Suppress("unused", "UNUSED_PARAMETER")
    fun requiredActor(@CurrentUserId actorId: UUID) {}

    @Suppress("unused", "UNUSED_PARAMETER")
    fun optionalViewer(@CurrentUserId viewerId: UUID?) {}

    @Suppress("unused", "UNUSED_PARAMETER")
    fun unannotated(id: UUID) {}

    private fun param(methodName: String): MethodParameter {
        val method = this::class.java.methods.first { it.name == methodName }
        return MethodParameter(method, 0)
    }

    @AfterEach
    fun clearContext() = SecurityContextHolder.clearContext()

    private fun authenticateAs(userId: UUID) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(ApplicationPrincipal(userId), "token", emptyList())
    }

    @Test
    fun `supportsParameter - CurrentUserId UUID는 지원`() {
        assertTrue(resolver.supportsParameter(param("requiredActor")))
        assertTrue(resolver.supportsParameter(param("optionalViewer")))
    }

    @Test
    fun `supportsParameter - 어노테이션 없으면 미지원`() {
        assertFalse(resolver.supportsParameter(param("unannotated")))
    }

    @Test
    fun `principal 있으면 userId를 반환한다`() {
        val userId = UUID.randomUUID()
        authenticateAs(userId)
        assertEquals(userId, resolver.resolveArgument(param("requiredActor"), null, webRequest, null))
        assertEquals(userId, resolver.resolveArgument(param("optionalViewer"), null, webRequest, null))
    }

    @Test
    fun `nullable 파라미터는 미인증 시 null을 반환한다`() {
        assertNull(resolver.resolveArgument(param("optionalViewer"), null, webRequest, null))
    }

    @Test
    fun `non-null 파라미터는 미인증 시 UNAUTHORIZED를 던진다`() {
        val ex = assertThrows(ApiException::class.java) {
            resolver.resolveArgument(param("requiredActor"), null, webRequest, null)
        }
        assertEquals(ErrorCode.UNAUTHORIZED, ex.code)
    }
}
