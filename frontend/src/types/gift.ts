/**
 * Gift domain types — aligned with BE /api/gifts contract.
 */

export type GiftStatus = "CREATED" | "OPENED" | "SAVED";

/** 자산 포맷 — 듀얼 포맷 분기. */
export type AssetKind = "TRACKLIST" | "MOOD_VIDEO";

/** 선물 계기(5값). */
export type Occasion =
  | "BIRTHDAY"
  | "ANNIVERSARY"
  | "COMFORT"
  | "CELEBRATION"
  | "JUST_BECAUSE";

/** 계기 → 한국어 라벨/이모지 (UI). */
export const OCCASION_LABELS: Record<Occasion, string> = {
  BIRTHDAY: "🎂 생일",
  ANNIVERSARY: "💞 기념일",
  COMFORT: "🌙 위로",
  CELEBRATION: "🎉 축하",
  JUST_BECAUSE: "💌 그냥",
};

export interface GiftSender {
  handle: string;
  displayName: string;
  avatarUrl?: string;
}

export interface GiftAsset {
  id: string;
  title: string;
  coverUrl?: string;
  tracks: GiftTrack[];
  /** 듀얼 포맷 — 뷰가 재생 컴포넌트를 이걸로 분기. */
  assetKind: AssetKind;
  moodVideoId?: string | null;
  moodChannelName?: string | null;
  moodTrackListText?: string | null;
}

export interface GiftTrack {
  id: string;
  name: string;
  artist: string;
  durationMs?: number;
  isrc?: string;
  spotifyId?: string;
  appleMusicId?: string;
  youtubeVideoId?: string | null;
}

/** POST /api/gifts response */
export interface GiftCreated {
  token: string;
  url: string;
}

/** GET /api/gifts/{token} response */
export interface GiftView {
  token: string;
  status: GiftStatus;
  message: string;
  wrapSkin: string;
  /** 헌정 — "지영에게". message 와 분리돼 봉투/언박싱/OG 에 단독 노출. */
  dedicationTo?: string | null;
  occasion?: Occasion | null;
  sender: GiftSender;
  asset: GiftAsset;
}

/** GET /api/gifts/received|sent 목록 항목(요약) */
export interface GiftSummary {
  token: string;
  status: GiftStatus;
  message: string;
  wrapSkin: string;
  dedicationTo?: string | null;
  occasion?: Occasion | null;
  sender: GiftSender;
  assetTitle: string;
  assetCoverUrl?: string;
  trackCount: number;
  assetKind: AssetKind;
  createdAt: string;
}

/** Nocturne 포장 스킨 정의 */
export interface WrapSkin {
  key: string;
  label: string;
  /** Tailwind/CSS gradient class or inline style value */
  gradient: string;
  accentColor: string;
}

export const WRAP_SKINS: WrapSkin[] = [
  {
    key: "nocturne-violet",
    label: "미드나잇 바이올렛",
    gradient: "linear-gradient(135deg, #2d1b69 0%, #11074a 100%)",
    accentColor: "#a78bfa",
  },
  {
    key: "nocturne-rose",
    label: "딥 로즈",
    gradient: "linear-gradient(135deg, #4a1128 0%, #1a0a1a 100%)",
    accentColor: "#f472b6",
  },
  {
    key: "nocturne-teal",
    label: "오션 틸",
    gradient: "linear-gradient(135deg, #0d3d3d 0%, #071a1a 100%)",
    accentColor: "#2dd4bf",
  },
  {
    key: "nocturne-gold",
    label: "앰버 골드",
    gradient: "linear-gradient(135deg, #3d2a00 0%, #1a1000 100%)",
    accentColor: "#fbbf24",
  },
];
