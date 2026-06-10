package com.plshare.backend.global.response

import org.springframework.data.domain.Page

/**
 * 목록 엔드포인트에서 공유하는 최소한의 페이지 래퍼.
 *
 * polyenm_pan의 `global/response/PageResponse.kt`를 차용·각색.
 * - Spring 기본 Page 직렬화는 버전마다 와이어 포맷이 달라질 수 있어 직접 안정적인 DTO로 래핑한다.
 * - 컨트롤러는 `PageResponse.from(page) { transform }` 한 줄로 변환한다.
 * - plshare 변경: polyenm Long id 대신 UUID 기반 도메인을 그대로 쓴다(타입 파라미터 S는 무제한).
 */
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
) {
    companion object {
        fun <S : Any, T : Any> from(page: Page<S>, transform: (S) -> T): PageResponse<T> = PageResponse(
            content = page.content.map(transform),
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            hasNext = page.hasNext(),
        )
    }
}
