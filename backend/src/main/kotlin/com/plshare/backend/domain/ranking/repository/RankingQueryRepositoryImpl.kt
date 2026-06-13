package com.plshare.backend.domain.ranking.repository

import com.plshare.backend.domain.export.model.ExportJobStatus
import com.plshare.backend.domain.ranking.dto.AssetRankRow
import com.plshare.backend.domain.ranking.dto.UserRankRow
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

/**
 * 랭킹 집계 JPQL 구현체.
 *
 * v1 on-read 전략 — 별도 랭킹 테이블/마이그레이션 없이 즉시 집계.
 * 규모 확장 시 materialized view / 스케줄 배치로 교체 예정.
 *
 * 플레이리스트 점수 = SUM(post_likes per post) + COUNT(completed export_jobs)
 * 사용자 점수     = SUM(post_likes on author's posts) + COUNT(followers)
 *
 * 동점 정렬: score DESC, 최신 포스트 createdAt DESC (자산) / userId ASC (사용자, 안정 정렬).
 */
@Repository
class RankingQueryRepositoryImpl(
    private val em: EntityManager,
) : RankingQueryRepository {

    // ── 자산 랭킹 ─────────────────────────────────────────────────────

    override fun findTopAssets(since: LocalDateTime?, limit: Int, offset: Int): List<AssetRankRow> {
        val sinceClause = if (since != null) "AND p.createdAt >= :since" else ""
        val jpql = """
            SELECT p.assetId,
                   COALESCE(SUM(likeSubq.cnt), 0) + COALESCE(exportSubq.exportCnt, 0) AS score,
                   MAX(p.createdAt) AS latestPost
            FROM Post p
            LEFT JOIN (
                SELECT pl.postId AS pid, COUNT(pl.id) AS cnt
                FROM PostLike pl
                GROUP BY pl.postId
            ) likeSubq ON likeSubq.pid = p.id
            LEFT JOIN (
                SELECT ej.assetId AS eid, COUNT(ej.id) AS exportCnt
                FROM ExportJob ej
                WHERE ej.status = :completedStatus
                GROUP BY ej.assetId
            ) exportSubq ON exportSubq.eid = p.assetId
            WHERE p.deleted = false
              AND p.assetId IS NOT NULL
              $sinceClause
            GROUP BY p.assetId, exportSubq.exportCnt
            ORDER BY score DESC, latestPost DESC
        """.trimIndent()

        val query = em.createQuery(jpql, Array::class.java)
            .setParameter("completedStatus", ExportJobStatus.COMPLETED)
            .setFirstResult(offset)
            .setMaxResults(limit)
        if (since != null) query.setParameter("since", since)

        return query.resultList.map { row ->
            AssetRankRow(
                assetId = row[0] as UUID,
                score = (row[1] as Number).toLong(),
                latestPostCreatedAt = row[2] as? LocalDateTime,
            )
        }
    }

    override fun countRankedAssets(since: LocalDateTime?): Long {
        val sinceClause = if (since != null) "AND p.createdAt >= :since" else ""
        val jpql = """
            SELECT COUNT(DISTINCT p.assetId)
            FROM Post p
            WHERE p.deleted = false
              AND p.assetId IS NOT NULL
              $sinceClause
        """.trimIndent()
        val query = em.createQuery(jpql, Long::class.javaObjectType)
        if (since != null) query.setParameter("since", since)
        return query.singleResult
    }

    // ── 사용자 랭킹 ───────────────────────────────────────────────────

    override fun findTopUsers(since: LocalDateTime?, limit: Int, offset: Int): List<UserRankRow> {
        val sinceClause = if (since != null) "AND p.createdAt >= :since" else ""
        val jpql = """
            SELECT p.authorId,
                   COALESCE(SUM(likeSubq.cnt), 0) + COALESCE(followerSubq.followerCnt, 0) AS score
            FROM Post p
            LEFT JOIN (
                SELECT pl.postId AS pid, COUNT(pl.id) AS cnt
                FROM PostLike pl
                GROUP BY pl.postId
            ) likeSubq ON likeSubq.pid = p.id
            LEFT JOIN (
                SELECT f.followeeId AS fid, COUNT(f.id) AS followerCnt
                FROM Follow f
                GROUP BY f.followeeId
            ) followerSubq ON followerSubq.fid = p.authorId
            WHERE p.deleted = false
              $sinceClause
            GROUP BY p.authorId, followerSubq.followerCnt
            ORDER BY score DESC, p.authorId ASC
        """.trimIndent()

        val query = em.createQuery(jpql, Array::class.java)
            .setFirstResult(offset)
            .setMaxResults(limit)
        if (since != null) query.setParameter("since", since)

        return query.resultList.map { row ->
            UserRankRow(
                userId = row[0] as UUID,
                score = (row[1] as Number).toLong(),
            )
        }
    }

    override fun countRankedUsers(since: LocalDateTime?): Long {
        val sinceClause = if (since != null) "AND p.createdAt >= :since" else ""
        val jpql = """
            SELECT COUNT(DISTINCT p.authorId)
            FROM Post p
            WHERE p.deleted = false
              $sinceClause
        """.trimIndent()
        val query = em.createQuery(jpql, Long::class.javaObjectType)
        if (since != null) query.setParameter("since", since)
        return query.singleResult
    }
}
