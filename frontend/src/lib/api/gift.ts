import type { GiftCreated, GiftView } from "@/types/gift";
import { apiFetch } from "./client";

export interface CreateGiftInput {
  senderId: string;
  assetId: string;
  message: string;
  wrapSkin: string;
}

/** POST /api/gifts */
export async function createGift(input: CreateGiftInput): Promise<GiftCreated> {
  return apiFetch<GiftCreated>("/api/gifts", {
    method: "POST",
    body: input,
  });
}

/** GET /api/gifts/{token} — 공개, 인증 불필요 */
export async function getGift(token: string): Promise<GiftView> {
  return apiFetch<GiftView>(`/api/gifts/${token}`);
}

/** POST /api/gifts/{token}/open — 언박싱 진입 (멱등) */
export async function openGift(token: string): Promise<GiftView> {
  return apiFetch<GiftView>(`/api/gifts/${token}/open`, { method: "POST" });
}

/** POST /api/gifts/{token}/save — 라이브러리 저장 */
export async function saveGift(token: string, userId: string): Promise<GiftView> {
  return apiFetch<GiftView>(`/api/gifts/${token}/save`, {
    method: "POST",
    body: { userId },
  });
}

/**
 * GET /api/tracks/{trackId}/youtube — 트랙을 재생 가능한 YouTube videoId로 resolve.
 * 공개(인증 불필요). videoId 가 null 이면 재생 후보를 못 찾은 것.
 */
export async function resolveTrackYouTube(
  trackId: string,
): Promise<{ videoId: string | null }> {
  return apiFetch<{ videoId: string | null }>(`/api/tracks/${trackId}/youtube`);
}
