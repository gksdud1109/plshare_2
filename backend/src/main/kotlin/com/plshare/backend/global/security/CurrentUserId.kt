package com.plshare.backend.global.security

/**
 * 컨트롤러 파라미터에 현재 인증 사용자의 userId(UUID)를 주입한다.
 *
 * - `@CurrentUserId actorId: UUID`   : 인증 필수. principal 이 없으면 401(UNAUTHORIZED).
 * - `@CurrentUserId viewerId: UUID?` : 선택적. 미인증이면 null — 공개 GET 의 개인화(likedByMe 등)용.
 *
 * 신원은 항상 [ApplicationSessionFilter] 가 세운 SecurityContext 의 [ApplicationPrincipal] 에서만 온다.
 * 요청 본문/쿼리스트링의 actor 값은 신뢰하지 않는다(위조 차단). demo·prod 동일 경로.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class CurrentUserId
