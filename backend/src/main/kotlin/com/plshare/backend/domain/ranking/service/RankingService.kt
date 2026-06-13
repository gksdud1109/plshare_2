package com.plshare.backend.domain.ranking.service

import com.plshare.backend.domain.asset.repository.AssetRepository
import com.plshare.backend.domain.ranking.dto.PlaylistRankItemResponse
import com.plshare.backend.domain.ranking.dto.UserRankItemResponse
import com.plshare.backend.domain.ranking.repository.RankingQueryRepository
import com.plshare.backend.domain.user.repository.UserRepository
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import com.plshare.backend.global.response.PageResponse
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 랭킹 도메인 서비스.
 *
 * v1 on-read 집계 전략: 요청마다 DB 집계 쿼리를 실행한다.
 * 트래픽이 증가하면 [RankingQueryRepository] 구현체를 materialized 뷰/배치 테이블로 교체 예정.
 *
 * 기간 윈도우 (post.createdAt 기준):
 * - realtime : 제한 없음 (전체 기간)
 * - weekly   : 최근 7일
 * - all-time : 제한 없음 (realtime과 동일 — 의미적 레이블 차이)
 *
 * 점수 공식:
 * - 플레이리스트: (자산 첨부 포스트들의 좋아요 합) + (자산 export 완료 건수)
 * - 사용자:     (작성 포스트들의 좋아요 합) + (팔로워 수)
 */
@Service
@Transactional(readOnly = true)
class RankingService(
    private val rankingRepo: RankingQueryRepository,
    private val assetRepo: AssetRepository,
    private val userRepo: UserRepository,
) {

    companion object {
        /** 주간 윈도우 일수. */
        const val WEEKLY_DAYS = 7L
    }

    fun rankPlaylists(period: String, pageable: Pageable): PageResponse<PlaylistRankItemResponse> {
        val since = periodToSince(period)
        val offset = (pageable.pageNumber * pageable.pageSize)
        val rows = rankingRepo.findTopAssets(since, pageable.pageSize, offset)
        val total = rankingRepo.countRankedAssets(since)

        val assetIds = rows.map { it.assetId }
        val assetMap = assetRepo.findAllById(assetIds).associateBy { it.id }

        val items = rows.mapIndexedNotNull { idx, row ->
            val asset = assetMap[row.assetId] ?: return@mapIndexedNotNull null
            PlaylistRankItemResponse(
                rank = offset + idx + 1,
                assetId = asset.id.toString(),
                title = asset.title,
                coverUrl = asset.coverUrl,
                trackCount = asset.tracks.size,
                score = row.score,
            )
        }

        val page = PageImpl(items, pageable, total)
        return PageResponse.from(page) { it }
    }

    fun rankUsers(period: String, pageable: Pageable): PageResponse<UserRankItemResponse> {
        val since = periodToSince(period)
        val offset = (pageable.pageNumber * pageable.pageSize)
        val rows = rankingRepo.findTopUsers(since, pageable.pageSize, offset)
        val total = rankingRepo.countRankedUsers(since)

        val userIds = rows.map { it.userId }
        val userMap = userRepo.findAllById(userIds).associateBy { it.id }

        val items = rows.mapIndexedNotNull { idx, row ->
            val user = userMap[row.userId] ?: return@mapIndexedNotNull null
            UserRankItemResponse(
                rank = offset + idx + 1,
                userId = user.id.toString(),
                handle = user.handle,
                displayName = user.displayName,
                avatarUrl = user.avatarUrl,
                score = row.score,
            )
        }

        val page = PageImpl(items, pageable, total)
        return PageResponse.from(page) { it }
    }

    // ── helpers ───────────────────────────────────────────────────────

    private fun periodToSince(period: String): LocalDateTime? = when (period.lowercase()) {
        "weekly" -> LocalDateTime.now().minusDays(WEEKLY_DAYS)
        "realtime", "all-time", "alltime" -> null
        else -> throw ApiException(
            ErrorCode.VALIDATION_FAILED,
            "period 값이 올바르지 않습니다: $period (realtime | weekly | all-time)",
        )
    }
}
