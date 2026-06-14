import { apiFetch } from "./client";
import type { CatalogTrack, ComposedAsset } from "@/types/catalog";

/** GET /api/catalog/tracks — 공개. mood 로 필터. */
export async function getCatalog(mood?: string): Promise<CatalogTrack[]> {
  const qs = mood ? `?mood=${encodeURIComponent(mood)}` : "";
  return apiFetch<CatalogTrack[]>(`/api/catalog/tracks${qs}`);
}

export interface ComposeInput {
  title: string;
  trackIds: string[];
  emotionTags?: string[];
  coverUrl?: string;
  description?: string;
}

/** POST /api/assets/compose — 선택 트랙으로 내 플레이리스트 조립(인증 필요). */
export async function composeAsset(input: ComposeInput): Promise<ComposedAsset> {
  return apiFetch<ComposedAsset>("/api/assets/compose", {
    method: "POST",
    body: input,
  });
}
