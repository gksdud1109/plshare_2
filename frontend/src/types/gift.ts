/**
 * Gift domain types — aligned with BE /api/gifts contract.
 */

export type GiftStatus = "CREATED" | "OPENED" | "SAVED";

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
  sender: GiftSender;
  asset: GiftAsset;
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
