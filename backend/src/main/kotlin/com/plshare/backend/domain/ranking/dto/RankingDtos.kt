package com.plshare.backend.domain.ranking.dto

import java.util.UUID

// ── 내부 집계 행 (repository → service) ──────────────────────────────

/**
 * 자산 랭킹 집계 행. repository 쿼리 결과를 담는 내부 DTO.
 * score = 포스트 좋아요 합 + export 완료 건수.
 */
data class AssetRankRow(
    val assetId: UUID,
    val score: Long,
    val latestPostCreatedAt: java.time.LocalDateTime?,
)

/**
 * 사용자 랭킹 집계 행. repository 쿼리 결과를 담는 내부 DTO.
 * score = 포스트 좋아요 합 + 팔로워 수.
 */
data class UserRankRow(
    val userId: UUID,
    val score: Long,
)

// ── API 응답 DTO ──────────────────────────────────────────────────────

/**
 * 플레이리스트(자산) 랭킹 항목 응답.
 * 클라이언트가 /assets/[id] 로 이동하기 위한 최소 정보를 포함.
 */
data class PlaylistRankItemResponse(
    /** 1-based 순위 */
    val rank: Int,
    val assetId: String,
    val title: String,
    val coverUrl: String?,
    val trackCount: Int,
    val score: Long,
)

/**
 * 사용자 랭킹 항목 응답.
 * 클라이언트가 /u/[handle] 로 이동하기 위한 최소 정보를 포함.
 */
data class UserRankItemResponse(
    /** 1-based 순위 */
    val rank: Int,
    val userId: String,
    val handle: String,
    val displayName: String,
    val avatarUrl: String?,
    val score: Long,
)
