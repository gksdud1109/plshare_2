import { apiFetch } from "./client";
import type {
  CatalogTrack,
  ComposedAsset,
  YouTubeCatalogSearchResult,
} from "@/types/catalog";

/** GET /api/catalog/tracks — 공개. mood 로 필터. */
export async function getCatalog(mood?: string): Promise<CatalogTrack[]> {
  const qs = mood ? `?mood=${encodeURIComponent(mood)}` : "";
  return apiFetch<CatalogTrack[]>(`/api/catalog/tracks${qs}`);
}

/** GET /api/catalog/youtube/search — API key 기반 검색이며 사용자 YouTube OAuth는 필요 없다. */
export async function searchYouTubeCatalog(
  query: string,
): Promise<YouTubeCatalogSearchResult[]> {
  return apiFetch<YouTubeCatalogSearchResult[]>(
    `/api/catalog/youtube/search?q=${encodeURIComponent(query)}`,
  );
}

interface ComposeInputBase {
  title: string;
  emotionTags?: string[];
  coverUrl?: string;
  description?: string;
}

export type ComposeInput = ComposeInputBase &
  (
    | { trackIds: string[]; selectionIds?: never }
    | { selectionIds: string[]; trackIds?: never }
  );

/** POST /api/assets/compose — 선택 트랙으로 내 플레이리스트 조립(인증 필요). */
export async function composeAsset(
  input: ComposeInput,
  idempotencyKey: string,
): Promise<ComposedAsset> {
  return apiFetch<ComposedAsset>("/api/assets/compose", {
    method: "POST",
    body: input,
    idempotencyKey,
  });
}

export interface MoodVideoInput {
  title: string;
  videoUrlOrId: string;
  channelName?: string;
  trackListText?: string;
  coverUrl?: string;
  emotionTags?: string[];
}

/** POST /api/assets/mood-video — 단일 유튜브 무드영상을 한 단위 자산으로(인증 필요). */
export async function composeMoodVideo(
  input: MoodVideoInput,
  idempotencyKey: string,
): Promise<ComposedAsset> {
  return apiFetch<ComposedAsset>("/api/assets/mood-video", {
    method: "POST",
    body: input,
    idempotencyKey,
  });
}
