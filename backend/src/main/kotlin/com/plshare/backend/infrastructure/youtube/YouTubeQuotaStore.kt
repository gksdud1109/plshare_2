package com.plshare.backend.infrastructure.youtube

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.LockModeType
import jakarta.persistence.Table
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 일일 write 쿼터 누적 카운터 저장소 추상화.
 *
 * 인터페이스로 분리한 이유:
 *  - [YouTubeQuotaGuard]가 영속 구현(JPA)과 분리되어 단위테스트에서 fake로 대체 가능.
 *  - demo(H2 ddl-auto)와 prod(PostgreSQL + Flyway) 모두 [JpaQuotaUsageStore]가 동작
 *    (Hibernate가 방언 처리 → 이전 JDBC `ON CONFLICT` 방식의 H2 비호환 문제 제거).
 */
interface QuotaUsageStore {
    /** 해당 날짜(ISO yyyy-MM-dd)의 누적 소모 unit. 행이 없으면 null. */
    fun getUsedUnits(dateStr: String): Long?

    /** 해당 날짜의 used_units를 [delta]만큼 증가(없으면 생성). */
    fun addUnits(dateStr: String, delta: Long)
}

/**
 * youtube_quota_usage 테이블 엔티티.
 *
 * 이 엔티티가 **demo 스키마의 소스**다(ddl-auto=create-drop가 자동 생성).
 * prod는 `V6__youtube_quota.sql`(Flyway)이 동일 스키마를 만든다 — 컬럼명/타입 일치 필수.
 *  - usage_date DATE PK (하루 1행)
 *  - used_units BIGINT NOT NULL
 */
@Entity
@Table(name = "youtube_quota_usage")
class YouTubeQuotaUsage(
    @Id
    @Column(name = "usage_date")
    val usageDate: LocalDate,

    @Column(name = "used_units", nullable = false)
    var usedUnits: Long = 0,
)

interface YouTubeQuotaJpaRepository : JpaRepository<YouTubeQuotaUsage, LocalDate> {
    /** 행 단위 쓰기 락(SELECT … FOR UPDATE)으로 동시 누적을 직렬화한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from YouTubeQuotaUsage q where q.usageDate = :date")
    fun findByDateForUpdate(@Param("date") date: LocalDate): YouTubeQuotaUsage?
}

/**
 * JPA 기반 쿼터 저장소. read-modify-write를 PESSIMISTIC_WRITE 락으로 직렬화한다 —
 * 단일 인스턴스에서도 export(@Async 스레드)와 트랙 resolve(요청 스레드)가 동시에 reserve하면
 * lost-update로 예산이 초과 소모될 수 있으므로, 누적은 [YouTubeQuotaGuard.reserve]의
 * @Transactional 안에서 행 락을 잡고 수행한다.
 */
@Component
class JpaQuotaUsageStore(
    private val repository: YouTubeQuotaJpaRepository,
) : QuotaUsageStore {

    override fun getUsedUnits(dateStr: String): Long? =
        repository.findById(LocalDate.parse(dateStr)).map { it.usedUnits }.orElse(null)

    override fun addUnits(dateStr: String, delta: Long) {
        val date = LocalDate.parse(dateStr)
        val locked = repository.findByDateForUpdate(date)
        if (locked != null) {
            locked.usedUnits += delta
            repository.save(locked)
            return
        }
        // 그날의 첫 행 — 동시 생성 시 PK(usage_date) 충돌은 락 경로로 재시도해 직렬화.
        try {
            repository.saveAndFlush(YouTubeQuotaUsage(usageDate = date, usedUnits = delta))
        } catch (e: DataIntegrityViolationException) {
            val row = repository.findByDateForUpdate(date) ?: throw e
            row.usedUnits += delta
            repository.save(row)
        }
    }
}
