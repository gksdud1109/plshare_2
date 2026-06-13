package com.plshare.backend.global.exception

import org.springframework.http.HttpStatus

/**
 * 안정적인 API 오류 코드. 클라이언트가 이 enum 이름(code)으로 분기할 수 있으므로
 * 기존 항목의 이름은 바꾸지 않는다(추가만 한다).
 *
 * polyenm_pan 의 global/exception/ErrorCode 컨벤션을 plshare 도메인에 맞춰 차용.
 */
enum class ErrorCode(val status: HttpStatus, val defaultMessage: String) {
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "이 작업을 수행할 권한이 없습니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다"),
    CONFLICT(HttpStatus.CONFLICT, "리소스 상태가 충돌했습니다"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다"),

    /** Spotify / Apple Music 등 외부 어댑터 호출 실패. */
    UPSTREAM_ERROR(HttpStatus.BAD_GATEWAY, "외부 음악 서비스 호출에 실패했습니다"),

    /**
     * YouTube Data API v3 일일 write 쿼터 초과.
     * 기본 10,000u/일 대비 설정된 예산(youtube.daily-write-budget)을 소진했을 때 발생.
     * 클라이언트는 이 코드를 받으면 당일 재시도 대신 사용자에게 안내해야 한다.
     */
    QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "YouTube 일일 쿼터가 초과되었습니다. 내일 다시 시도해 주세요"),

    INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다"),
}
