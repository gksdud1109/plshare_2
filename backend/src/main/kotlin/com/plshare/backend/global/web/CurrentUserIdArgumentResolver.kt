package com.plshare.backend.global.web

import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import com.plshare.backend.global.security.ApplicationPrincipal
import com.plshare.backend.global.security.CurrentUserId
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.util.UUID

/**
 * `@CurrentUserId` 파라미터 리졸버.
 *
 * SecurityContext 의 [ApplicationPrincipal] 에서 userId(UUID) 를 꺼낸다.
 * - 파라미터가 non-null(`UUID`) 인데 미인증이면 [ErrorCode.UNAUTHORIZED].
 * - nullable(`UUID?`) 이면 미인증 시 null.
 *
 * Kotlin nullable 여부는 [MethodParameter.isOptional] 로 판별한다(Spring KotlinDetector).
 */
class CurrentUserIdArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(CurrentUserId::class.java) &&
            parameter.parameterType == UUID::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any? {
        val principal = SecurityContextHolder.getContext()
            .authentication
            ?.principal as? ApplicationPrincipal
        if (principal != null) return principal.userId
        if (parameter.isOptional) return null
        throw ApiException(ErrorCode.UNAUTHORIZED, "인증이 필요합니다")
    }
}
