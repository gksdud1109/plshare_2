package com.plshare.backend.global.openapi

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Swagger UI: `/swagger-ui.html`, OpenAPI JSON: `/v3/api-docs`.
 * polyenm_pan 컨벤션 차용(springdoc). 컨트롤러별 API 인터페이스 분리(@Operation/@ApiErrorCode)는
 * 선택적 후속 작업 — 우선 의존성 + 기본 메타데이터로 자동 문서화를 켠다.
 */
@Configuration
class OpenApiConfig {

    @Bean
    fun plshareOpenApi(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("plshare2 API")
                .description("Spotify import → Emotional Context → Apple Music export. 응답은 ApiResponse<T> envelope, 오류는 ErrorResponse.")
                .version("v0.2"),
        )
}
