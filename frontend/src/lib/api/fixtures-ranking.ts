/**
 * Ranking demo fixtures — used when the BE is unreachable.
 * Pure data, no fetch calls.
 */

import type {
  PlaylistRankItem,
  UserRankItem,
  PageResponse,
} from "@/types/ranking";

export const demoPlaylistRanking: PlaylistRankItem[] = [
  {
    rank: 1,
    assetId: "asset-rank-001",
    title: "새벽 다섯시의 산책",
    coverUrl:
      "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&q=80",
    trackCount: 18,
    score: 142,
  },
  {
    rank: 2,
    assetId: "asset-rank-002",
    title: "해안도로 드라이브",
    coverUrl:
      "https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=600&q=80",
    trackCount: 24,
    score: 98,
  },
  {
    rank: 3,
    assetId: "asset-rank-003",
    title: "오후의 카페",
    coverUrl:
      "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=600&q=80",
    trackCount: 12,
    score: 67,
  },
  {
    rank: 4,
    assetId: "asset-rank-004",
    title: "비오는 창가",
    coverUrl:
      "https://images.unsplash.com/photo-1428591565892-8eb6703eb573?w=600&q=80",
    trackCount: 15,
    score: 45,
  },
  {
    rank: 5,
    assetId: "asset-rank-005",
    title: "늦은 밤 집중 모드",
    coverUrl:
      "https://images.unsplash.com/photo-1519683384663-e3afba39bfbb?w=600&q=80",
    trackCount: 20,
    score: 31,
  },
];

export const demoPlaylistRankingPage: PageResponse<PlaylistRankItem> = {
  content: demoPlaylistRanking,
  page: 0,
  size: 20,
  totalElements: demoPlaylistRanking.length,
  totalPages: 1,
  hasNext: false,
};

export const demoUserRanking: UserRankItem[] = [
  {
    rank: 1,
    userId: "00000000-0000-0000-0000-000000000001",
    handle: "jimin",
    displayName: "지민",
    avatarUrl: "https://picsum.photos/seed/jimin/200/200",
    score: 215,
  },
  {
    rank: 2,
    userId: "00000000-0000-0000-0000-000000000002",
    handle: "soyul",
    displayName: "소율",
    avatarUrl: "https://picsum.photos/seed/soyul/200/200",
    score: 178,
  },
  {
    rank: 3,
    userId: "00000000-0000-0000-0000-000000000003",
    handle: "hyunwoo",
    displayName: "현우",
    avatarUrl: null,
    score: 93,
  },
  {
    rank: 4,
    userId: "00000000-0000-0000-0000-000000000004",
    handle: "dayoung",
    displayName: "다영",
    avatarUrl: "https://picsum.photos/seed/dayoung/200/200",
    score: 61,
  },
  {
    rank: 5,
    userId: "00000000-0000-0000-0000-000000000005",
    handle: "seunghyun",
    displayName: "승현",
    avatarUrl: null,
    score: 44,
  },
];

export const demoUserRankingPage: PageResponse<UserRankItem> = {
  content: demoUserRanking,
  page: 0,
  size: 20,
  totalElements: demoUserRanking.length,
  totalPages: 1,
  hasNext: false,
};
