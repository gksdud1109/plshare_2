package com.plshare.backend.domain.ranking.repository

import com.plshare.backend.domain.ranking.dto.AssetRankRow
import com.plshare.backend.domain.ranking.dto.UserRankRow
import java.time.LocalDateTime

/**
 * 랭킹 집계 쿼리 인터페이스.
 *
 * v1은 on-read 집계(DB 쿼리로 즉시 계산) — 별도 랭킹 테이블 없음.
 * 트래픽이 늘어 쿼리 비용이 커지면 materialized view / 스케줄 배치로 교체한다.
 *
 * 플레이리스트 점수 = (자산을 첨부한 포스트들의 좋아요 합) + (해당 자산 export 완료 건수)
 * 사용자 점수     = (작성 포스트들이 받은 좋아요 합) + (팔로워 수)
 */
interface RankingQueryRepository {

    /**
     * 자산 랭킹 집계.
     * [since] 이후 생성된 포스트만 카운트 (기간 윈도우). null이면 전체 기간.
     */
    fun findTopAssets(since: LocalDateTime?, limit: Int, offset: Int): List<AssetRankRow>
    fun countRankedAssets(since: LocalDateTime?): Long

    /**
     * 사용자 랭킹 집계.
     * [since] 이후 생성된 포스트만 좋아요 집계. 팔로워 수는 기간 무관.
     */
    fun findTopUsers(since: LocalDateTime?, limit: Int, offset: Int): List<UserRankRow>
    fun countRankedUsers(since: LocalDateTime?): Long
}
