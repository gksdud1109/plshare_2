package com.plshare.backend.infrastructure.youtube

import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * YouTubeQuotaGuard 단위 테스트 — fake repository (no Spring context, no DB).
 *
 * 검증 불변식:
 *  (a) 예산 잔여 >= 견적 → reserve 성공, 정확한 누적
 *  (b) 예산 잔여 < 견적 → QUOTA_EXCEEDED
 *  (c) 경계값: 잔여 == 견적 → 성공 (정확히 소진)
 *  (d) 여러 번 reserve → 누적 정확성
 */
class YouTubeQuotaGuardTest {

    private lateinit var fakeRepo: FakeQuotaRepository
    private lateinit var guard: YouTubeQuotaGuard

    @BeforeEach
    fun setUp() {
        fakeRepo = FakeQuotaRepository()
        guard = YouTubeQuotaGuard(fakeRepo, dailyBudget = 1000L)
    }

    // ─── (a) 예산 충분 → 성공 ────────────────────────────────────────────────

    @Test
    fun `예산 충분한 경우 reserve 성공`() {
        guard.reserve(500L)
        assertEquals(500L, guard.todayUsed())
    }

    @Test
    fun `첫 번째 reserve 후 두 번째 reserve도 예산 내이면 성공`() {
        guard.reserve(400L)
        guard.reserve(400L)
        assertEquals(800L, guard.todayUsed())
    }

    // ─── (b) 예산 초과 → QUOTA_EXCEEDED ─────────────────────────────────────

    @Test
    fun `견적이 전체 예산 초과 시 QUOTA_EXCEEDED`() {
        val ex = assertThrows(ApiException::class.java) { guard.reserve(1001L) }
        assertEquals(ErrorCode.QUOTA_EXCEEDED, ex.code)
    }

    @Test
    fun `이전 소모 후 잔여 예산 부족하면 QUOTA_EXCEEDED`() {
        guard.reserve(700L) // used=700, remaining=300
        val ex = assertThrows(ApiException::class.java) { guard.reserve(400L) } // 300 < 400
        assertEquals(ErrorCode.QUOTA_EXCEEDED, ex.code)
        // 실패 후 카운터는 증가하지 않음
        assertEquals(700L, guard.todayUsed())
    }

    // ─── (c) 경계값: 잔여 == 견적 → 성공 ────────────────────────────────────

    @Test
    fun `잔여 예산과 견적이 정확히 같으면 성공`() {
        guard.reserve(600L) // used=600, remaining=400
        guard.reserve(400L) // remaining == requested → OK
        assertEquals(1000L, guard.todayUsed())
    }

    @Test
    fun `예산 전체를 한 번에 소진 가능`() {
        guard.reserve(1000L)
        assertEquals(1000L, guard.todayUsed())
    }

    // ─── (d) 누적 정확성 ─────────────────────────────────────────────────────

    @Test
    fun `estimatedCost - 30트랙 1건 비용 계산`() {
        val cost = YouTubeQuotaGuard.estimatedCost(30)
        // 50 + 50*30 = 1550
        assertEquals(1550L, cost)
    }

    @Test
    fun `estimatedCost - 0트랙이면 플레이리스트 생성 비용만`() {
        val cost = YouTubeQuotaGuard.estimatedCost(0)
        assertEquals(50L, cost)
    }

    @Test
    fun `여러 건 reserve 누적이 정확함`() {
        // 3건 × 150u = 450u
        repeat(3) { guard.reserve(150L) }
        assertEquals(450L, guard.todayUsed())
    }

    @Test
    fun `QUOTA_EXCEEDED 예외 이후에도 카운터는 이전 값 유지`() {
        guard.reserve(900L) // used=900
        assertThrows(ApiException::class.java) { guard.reserve(200L) } // 100 < 200 → fail
        assertEquals(900L, guard.todayUsed()) // 카운터 불변
    }
}

// ─── Fake 구현체 ─────────────────────────────────────────────────────────────

/**
 * [QuotaUsageStore]의 in-memory fake. DB 없이 쿼터 가드 로직만 검증한다.
 */
class FakeQuotaRepository : QuotaUsageStore {
    private val store = mutableMapOf<String, Long>()

    override fun getUsedUnits(dateStr: String): Long? = store[dateStr]

    override fun addUnits(dateStr: String, delta: Long) {
        store[dateStr] = (store[dateStr] ?: 0L) + delta
    }
}
