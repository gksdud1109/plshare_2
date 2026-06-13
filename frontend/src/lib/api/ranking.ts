/**
 * Ranking API client — wraps GET /api/ranking/* endpoints.
 *
 * BE endpoints:
 *   GET /api/ranking/playlists?period=&page=&size= → PageResponse<PlaylistRankItem>
 *   GET /api/ranking/users?period=&page=&size=     → PageResponse<UserRankItem>
 */

import { apiFetch } from "./client";
import type {
  PlaylistRankItem,
  UserRankItem,
  RankingPeriod,
  PageResponse,
} from "@/types/ranking";

export async function listPlaylistRanking(
  period: RankingPeriod,
  page: number,
  size: number,
): Promise<PageResponse<PlaylistRankItem>> {
  const params = new URLSearchParams({
    period,
    page: String(page),
    size: String(size),
  });
  return apiFetch<PageResponse<PlaylistRankItem>>(`/api/ranking/playlists?${params}`);
}

export async function listUserRanking(
  period: RankingPeriod,
  page: number,
  size: number,
): Promise<PageResponse<UserRankItem>> {
  const params = new URLSearchParams({
    period,
    page: String(page),
    size: String(size),
  });
  return apiFetch<PageResponse<UserRankItem>>(`/api/ranking/users?${params}`);
}
