package com.plshare.backend.global.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class ApplicationSessionFilter(
    private val sessions: ApplicationSessionService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = request.getHeader("Authorization")
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")
            ?.trim()

        val principal = token?.let(sessions::verify)
        if (principal != null) {
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(principal, token, emptyList())
        }
        filterChain.doFilter(request, response)
    }
}
