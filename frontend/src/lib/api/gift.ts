import type { GiftCreated, GiftView, GiftSummary } from "@/types/gift";
import { apiFetch } from "./client";

/** GET /api/gifts/received — 받은(저장한) 선물 목록 (인증 필요) */
export async function getReceivedGifts(): Promise<GiftSummary[]> {
  return apiFetch<GiftSummary[]>("/api/gifts/received");
}

/** GET /api/gifts/sent — 보낸 선물 목록 (인증 필요) */
export async function getSentGifts(): Promise<GiftSummary[]> {
  return apiFetch<GiftSummary[]>("/api/gifts/sent");
}

export interface CreateGiftInput {
  assetId: string;
  message: string;
  wrapSkin: string;
  /** 헌정 — 받는 사람 이름(선택, ≤40). */
  dedicationTo?: string;
  /** 계기(선택). */
  occasion?: string;
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

/** POST /api/gifts/{token}/save — 라이브러리 저장 (인증 필수, 수신자는 세션 principal로 해석) */
export async function saveGift(token: string): Promise<GiftView> {
  return apiFetch<GiftView>(`/api/gifts/${token}/save`, { method: "POST" });
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

/**
 * 서버사이드 gift 조회 (generateMetadata / OG 이미지 라우트용). 읽기 전용 —
 * 선물 상태를 바꾸지 않는다. 내부 base URL 우선, 실패 시 throw하여 호출측 폴백.
 */
export async function fetchGiftServer(token: string): Promise<GiftView> {
  const base =
    process.env.API_BASE_INTERNAL_URL?.trim() ||
    process.env.NEXT_PUBLIC_API_BASE_URL?.trim() ||
    "http://localhost:8080";

  const res = await fetch(`${base}/api/gifts/${token}`, {
    headers: { Accept: "application/json" },
    cache: "force-cache",
    next: { revalidate: 60, tags: [`gift-${token}`] },
  });
  if (!res.ok) {
    throw new Error(`fetchGiftServer: ${res.status} ${res.statusText}`);
  }
  const json: unknown = await res.json();
  const payload =
    json !== null && typeof json === "object" && "data" in json
      ? (json as { data: unknown }).data
      : json;
  return payload as GiftView;
}
