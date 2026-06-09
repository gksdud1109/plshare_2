package com.plshare.backend.global.response

/**
 * 표준 성공 응답 envelope. polyenm_pan 컨벤션 차용.
 *  - code:    HTTP 상태 문자열("200")
 *  - message: 사람이 읽는 메시지
 *  - data:    실제 페이로드
 *
 * 오류는 [com.plshare.backend.global.exception.ErrorResponse]로 내려가며 이 envelope를 쓰지 않는다.
 */
data class ApiResponse<T>(
    val code: String,
    val message: String,
    val data: T? = null,
) {
    companion object {
        fun <T> ok(data: T?): ApiResponse<T> = ApiResponse(code = "200", message = "OK", data = data)
    }
}
