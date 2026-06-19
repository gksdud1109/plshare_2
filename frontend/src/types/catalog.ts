/** 큐레이션 카탈로그 타입 — BE /api/catalog 계약과 일치. */

export interface CatalogTrack {
  id: string;
  title: string;
  artist: string;
  youtubeVideoId: string;
  durationMs?: number | null;
  mood: string;
  coverUrl?: string | null;
}

/** GET /api/catalog/youtube/search 항목 — selectionId는 compose 시 그대로 전송한다. */
export interface YouTubeCatalogSearchResult {
  selectionId: string;
  videoId: string;
  title: string;
  channelTitle?: string | null;
  thumbnailUrl?: string | null;
}

/** POST /api/assets/compose 응답 */
export interface ComposedAsset {
  id: string;
  title: string;
  trackCount: number;
}

/** 무드 키 → 한국어 라벨 (UI 표시용) */
export const MOOD_LABELS: Record<string, string> = {
  latenight: "새벽 감성",
  drive: "밤 드라이브",
  love: "설렘·사랑",
  comfort: "위로·응원",
  focus: "집중·무드",
  energy: "기분전환",
};

export const MOOD_ORDER = [
  "latenight",
  "drive",
  "love",
  "comfort",
  "focus",
  "energy",
] as const;
