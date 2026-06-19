package com.plshare.backend.infrastructure.youtube

import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * YouTube Data API v3 일일 예약 쿼터 가드.
 *
 * ## 역할
 * export 시작 전에 예상 unit을 예약([reserve])하고, 실제 소모량을 기록([record])한다.
 * 예산 초과 시 [ApiException](QUOTA_EXCEEDED)을 던져 export job을 FAILED 처리한다.
 *
 * ## 비용 모델
 *   - [PLAYLIST_INSERT_COST] = 50u  (playlists.insert 1회)
 *   - [ITEM_INSERT_COST]     = 50u  (playlistItems.insert 1곡)
 *   → N곡 1건 ≈ 50 + 50*N unit. 30곡 ≈ 1,550u.
 *
 * ## 예산 설정
 *   `youtube.daily-write-budget` (기본 8,000u, 기존 설정명 유지):
 *   공식 한도 10,000u 대비 2,000u 헤드룸을 둔다.
 *   이유: read 쿼터(import/matching) + 예상치 못한 재시도가 동일 할당량을 공유하므로
 *   export write와 고비용 search.list가 함께 사용하는 예약 예산이다.
 *
 * ## 영속 방식: DB (재시작 안전)
 *   in-memory 대신 `youtube_quota_usage` 테이블(V6 마이그레이션)을 사용한다.
 *   - 서버 재시작/배포 후에도 당일 누적 소모량이 유지된다.
 *   - ~6건/일 정책에서 배포 사이 카운터 리셋은 예산을 실제보다 넓게 열어줄 위험이 있다.
 *   - 단일 행 UPDATE는 성능 영향 무시할 수준이며 트랜잭션 격리로 동시성을 처리한다.
 *
 * ## 트랜잭션
 *   [reserve]는 @Transactional로 선언되어 예약과 확인이 원자적으로 처리된다.
 *   호출 스레드(ExportService.runExport)가 @Async이므로 새 트랜잭션이 시작된다.
 */
@Component
class YouTubeQuotaGuard(
    private val quotaRepository: QuotaUsageStore,
    @Value("\${youtube.daily-write-budget:8000}") private val dailyBudget: Long
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        /** playlists.insert 1회 비용 */
        const val PLAYLIST_INSERT_COST = 50L
        /** playlistItems.insert 1곡 비용 */
        const val ITEM_INSERT_COST = 50L
        const val SEARCH_COST = 100L

        /**
         * N곡 export 예상 비용 계산.
         * = PLAYLIST_INSERT_COST + ITEM_INSERT_COST * trackCount
         */
        fun estimatedCost(trackCount: Int): Long = PLAYLIST_INSERT_COST + ITEM_INSERT_COST * trackCount

        fun estimatedCost(trackCount: Int, searchCount: Int): Long =
            estimatedCost(trackCount) + SEARCH_COST * searchCount
    }

    /**
     * [estimatedUnits]만큼의 예산을 예약한다.
     *
     * 현재 잔여 예산이 [estimatedUnits]보다 적으면 [ApiException](QUOTA_EXCEEDED)을 던진다.
     * 예산이 충분하면 [estimatedUnits]를 즉시 소모 기록(낙관적 예약)하고 반환한다.
     *
     * 낙관적 예약 선택 이유: export가 성공하면 실제 소모량 ≈ 예상량이므로 차이가 미미하다.
     * 실패(일부 트랙 skipped)하면 실제 소모가 더 적지만, 보수적 예산 정책과 일치한다.
     *
     * @throws ApiException QUOTA_EXCEEDED — 예산 초과
     */
    @Transactional
    fun reserve(estimatedUnits: Long) {
        val today = LocalDate.now()
        val used = quotaRepository.getUsedUnits(today.format(DateTimeFormatter.ISO_LOCAL_DATE)) ?: 0L
        val remaining = dailyBudget - used

        log.debug("YouTube quota check: budget={}, used={}, remaining={}, requested={}", dailyBudget, used, remaining, estimatedUnits)

        if (remaining < estimatedUnits) {
            log.warn("YouTube quota exceeded: budget={}, used={}, requested={}", dailyBudget, used, estimatedUnits)
            throw ApiException(
                ErrorCode.QUOTA_EXCEEDED,
                "YouTube 일일 예약 예산 초과: 예산=${dailyBudget}u, 소모=${used}u, 잔여=${remaining}u, 요청=${estimatedUnits}u"
            )
        }

        quotaRepository.addUnits(today.format(DateTimeFormatter.ISO_LOCAL_DATE), estimatedUnits)
        log.info("YouTube quota reserved {}u (total used today: {}u / {}u)", estimatedUnits, used + estimatedUnits, dailyBudget)
    }

    /**
     * 실제 소모 unit을 추가 기록한다.
     *
     * reserve() 이후 실제 소모가 확정되었을 때(각 API 호출 성공 후) 호출.
     * 현재 구현은 낙관적 예약(reserve에서 예상량 선불)이므로
     * 별도 record() 호출 없이도 예산 관리가 동작한다.
     * 향후 정밀 추적이 필요하면 이 메서드를 확장한다.
     */
    @Transactional
    fun record(actualUnits: Long) {
        val today = LocalDate.now()
        quotaRepository.addUnits(today.format(DateTimeFormatter.ISO_LOCAL_DATE), actualUnits)
        log.debug("YouTube quota recorded {}u actual", actualUnits)
    }

    /** 오늘 사용한 unit 조회 (모니터링/테스트 용도). */
    @Transactional(readOnly = true)
    fun todayUsed(): Long {
        val today = LocalDate.now()
        return quotaRepository.getUsedUnits(today.format(DateTimeFormatter.ISO_LOCAL_DATE)) ?: 0L
    }
}
