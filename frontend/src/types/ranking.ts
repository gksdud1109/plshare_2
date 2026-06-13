/**
 * Ranking domain types — aligned with BE RankingController contracts.
 *
 * BE source of truth:
 *   backend/.../domain/ranking/dto/RankingDtos.kt
 *   backend/.../domain/ranking/controller/RankingController.kt
 *
 * PageResponse<T> is re-exported from social.ts (import only, no redeclaration).
 */

import type { PageResponse } from "@/types/social";

export type { PageResponse };

/** period 윈도우 */
export type RankingPeriod = "realtime" | "weekly" | "all-time";

/** 플레이리스트(자산) 랭킹 항목 — mirrors PlaylistRankItemResponse */
export interface PlaylistRankItem {
  rank: number;
  assetId: string;
  title: string;
  coverUrl: string | null;
  trackCount: number;
  score: number;
}

/** 사용자 랭킹 항목 — mirrors UserRankItemResponse */
export interface UserRankItem {
  rank: number;
  userId: string;
  handle: string;
  displayName: string;
  avatarUrl: string | null;
  score: number;
}

/** 랭킹 탭 */
export type RankingTab = "playlists" | "users";
