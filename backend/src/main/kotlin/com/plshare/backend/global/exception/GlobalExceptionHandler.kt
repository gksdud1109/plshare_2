package com.plshare.backend.global.exception

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * 전역 예외 → 표준 [ErrorResponse] 변환. polyenm_pan 컨벤션 차용.
 * 컨트롤러/서비스의 요청 경로에서 던져진 [ApiException]을 안정적인 HTTP 상태로 매핑한다.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ApiException::class)
    fun handleApi(ex: ApiException): ResponseEntity<ErrorResponse> {
        // 4xx는 평범한 흐름이므로 WARN, 5xx 성격(UPSTREAM/INTERNAL)은 ERROR로 남긴다.
        if (ex.code.status.is5xxServerError) {
            log.error("ApiException ${ex.code}", ex)
        } else {
            log.warn("ApiException {}: {}", ex.code, ex.message)
        }
        return ResponseEntity
            .status(ex.code.status)
            .body(ErrorResponse(code = ex.code.name, message = ex.message ?: ex.code.defaultMessage))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.bindingResult.fieldErrors.map {
            ErrorResponse.FieldError(field = it.field, message = it.defaultMessage ?: "invalid")
        }
        return ResponseEntity
            .status(ErrorCode.VALIDATION_FAILED.status)
            .body(
                ErrorResponse(
                    code = ErrorCode.VALIDATION_FAILED.name,
                    message = ErrorCode.VALIDATION_FAILED.defaultMessage,
                    details = fieldErrors,
                ),
            )
    }

    /** 매칭 핸들러/정적 리소스 없음(잘못된 경로 등). 일상적 404이므로 ERROR 로그 없이 깔끔한 404. */
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResource(ex: NoResourceFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(ErrorCode.NOT_FOUND.status)
            .body(ErrorResponse(code = ErrorCode.NOT_FOUND.name, message = ErrorCode.NOT_FOUND.defaultMessage))

    @ExceptionHandler(Exception::class)
    fun handleUnknown(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception", ex)
        return ResponseEntity
            .status(ErrorCode.INTERNAL.status)
            .body(ErrorResponse(code = ErrorCode.INTERNAL.name, message = ErrorCode.INTERNAL.defaultMessage))
    }
}
