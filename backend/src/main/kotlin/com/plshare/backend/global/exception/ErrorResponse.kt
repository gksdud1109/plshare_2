package com.plshare.backend.global.exception

/**
 * 표준 오류 응답 바디. 성공 응답 형태는 (현재) 건드리지 않고, 오류만 일관된 모양으로 내려준다.
 *  - code:    [ErrorCode] 이름 (클라이언트 분기용 안정 식별자)
 *  - message: 사람이 읽는 메시지
 *  - details: 검증 실패 시 필드별 메시지 (없으면 생략)
 */
data class ErrorResponse(
    val code: String,
    val message: String,
    val details: List<FieldError>? = null,
) {
    data class FieldError(
        val field: String,
        val message: String,
    )
}
