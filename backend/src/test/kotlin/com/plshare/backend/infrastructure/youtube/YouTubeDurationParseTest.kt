package com.plshare.backend.infrastructure.youtube

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * ISO 8601 duration 파싱 단위 테스트 (c 요구사항).
 *
 * 검증 케이스:
 *   - 일반 분+초 조합
 *   - 시+분+초 조합
 *   - 초만 있는 케이스
 *   - 시만 있는 케이스
 *   - 라이브 스트림용 "P0D" (비정상 케이스)
 *   - 빈 문자열 / null
 *   - videoId 정규화 케이스
 */
class YouTubeDurationParseTest {

    @Test
    fun `PT3M30S 는 210_000ms`() {
        assertEquals(210_000L, parseDurationMs("PT3M30S"))
    }

    @Test
    fun `PT3M20S 는 200_000ms`() {
        assertEquals(200_000L, parseDurationMs("PT3M20S"))
    }

    @Test
    fun `PT1H2M3S 는 3_723_000ms`() {
        assertEquals(3_723_000L, parseDurationMs("PT1H2M3S"))
    }

    @Test
    fun `PT45S 는 45_000ms`() {
        assertEquals(45_000L, parseDurationMs("PT45S"))
    }

    @Test
    fun `PT1H 는 3_600_000ms`() {
        assertEquals(3_600_000L, parseDurationMs("PT1H"))
    }

    @Test
    fun `PT10M 은 600_000ms`() {
        assertEquals(600_000L, parseDurationMs("PT10M"))
    }

    @Test
    fun `PT0S 는 0ms`() {
        assertEquals(0L, parseDurationMs("PT0S"))
    }

    @Test
    fun `P0D 는 0ms (라이브스트림 엣지케이스)`() {
        // "P0D" 는 PTH|M|S 패턴이 아니므로 0L 반환
        assertEquals(0L, parseDurationMs("P0D"))
    }

    @Test
    fun `빈 문자열은 0ms`() {
        assertEquals(0L, parseDurationMs(""))
    }

    @Test
    fun `null 은 0ms`() {
        assertEquals(0L, parseDurationMs(null))
    }

    @Test
    fun `PT7M4S 는 424_000ms`() {
        assertEquals(424_000L, parseDurationMs("PT7M4S"))
    }

    // ─── videoId 정규화 ───────────────────────────────────────────────────────

    @Test
    fun `bare videoId 는 그대로 반환`() {
        assertEquals("dQw4w9WgXcQ", normalizeVideoId("dQw4w9WgXcQ"))
    }

    @Test
    fun `watch URL 에서 videoId 추출`() {
        assertEquals(
            "dQw4w9WgXcQ",
            normalizeVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        )
    }

    @Test
    fun `youtu be short URL 에서 videoId 추출`() {
        assertEquals(
            "dQw4w9WgXcQ",
            normalizeVideoId("https://youtu.be/dQw4w9WgXcQ")
        )
    }

    @Test
    fun `watch URL 에 다른 파라미터가 있어도 videoId 추출`() {
        assertEquals(
            "dQw4w9WgXcQ",
            normalizeVideoId("https://www.youtube.com/watch?list=PL123&v=dQw4w9WgXcQ&t=30s")
        )
    }

    @Test
    fun `인식 안 되는 입력은 원본 반환`() {
        val input = "not-a-video-id"
        assertEquals(input, normalizeVideoId(input))
    }
}
