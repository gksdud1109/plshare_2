package com.plshare.backend.infrastructure.youtube

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
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

interface YouTubeQuotaJpaRepository : JpaRepository<YouTubeQuotaUsage, LocalDate>

/**
 * JPA 기반 쿼터 저장소. read-modify-write는 [YouTubeQuotaGuard.reserve]의 @Transactional
 * 안에서 원자적으로 수행된다(demo 단일 인스턴스 + @Async 단일 export 스레드 기준 충분).
 * 멀티 인스턴스 prod에서 정밀 동시성이 필요하면 PESSIMISTIC_WRITE 락으로 강화할 수 있다.
 */
@Component
class JpaQuotaUsageStore(
    private val repository: YouTubeQuotaJpaRepository,
) : QuotaUsageStore {

    override fun getUsedUnits(dateStr: String): Long? =
        repository.findById(LocalDate.parse(dateStr)).map { it.usedUnits }.orElse(null)

    override fun addUnits(dateStr: String, delta: Long) {
        val date = LocalDate.parse(dateStr)
        val row = repository.findById(date).orElseGet { YouTubeQuotaUsage(usageDate = date, usedUnits = 0) }
        row.usedUnits += delta
        repository.save(row)
    }
}
