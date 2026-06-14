package com.plshare.backend.global.web

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    // prod 에선 ALLOWED_ORIGINS=https://app.도메인 으로 주입(CSV). 미설정 시 로컬 기본값.
    // 하드코딩 localhost 만 두면 Vercel 프론트의 모든 호출이 CORS 차단된다.
    @Value("\${ALLOWED_ORIGINS:http://localhost:3000,http://127.0.0.1:3000}")
    private val allowedOriginsCsv: String,
) : WebMvcConfigurer {
    private val allowedOrigins: Array<String>
        get() = allowedOriginsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toTypedArray()

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(CurrentUserIdArgumentResolver())
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins(*allowedOrigins)
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders(
                "Content-Type",
                "Authorization",
                "X-Idempotency-Key",
                "Accept",
                "Origin"
            )
            .exposedHeaders("X-Idempotency-Key")
            .allowCredentials(true)
            .maxAge(3600)
    }
}
