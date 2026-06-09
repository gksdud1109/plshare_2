package com.plshare.backend.global.exception

/**
 * [GlobalExceptionHandler]를 통해 안정적인 HTTP 응답으로 변환되는 도메인 예외.
 * 서비스/컨트롤러는 raw 예외(IllegalState/NoSuchElement 등) 대신 이것을 던진다.
 */
class ApiException(
    val code: ErrorCode,
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message ?: code.defaultMessage, cause)
