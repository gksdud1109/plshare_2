/**
 * Domain types for the Import -> Assetize -> Export loop.
 * These are a frontend-side best guess to unblock UI work while the BE contract
 * is being finalized. Replace with generated types once the OpenAPI/handoff lands.
 *
 * Source of truth (once ready):
 *   docs/20-product/delivery/implementation/open-issues.md
 */

export type AssetSource = "spotify" | "apple_music";

export interface Track {
  id: string;
  title: string;
  artist: string;
  album?: string;
  durationMs: number;
  coverUrl?: string;
  externalUrls: Partial<Record<AssetSource, string>>;
}

export interface PlaylistAsset {
  id: string;
  title: string;
  subtitle?: string;
  curatorNote?: string;
  coverUrl?: string;
  source: AssetSource;
  createdAt: string;
  tracks: Track[];
}

export type AsyncState<T> =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "success"; data: T }
  | { status: "error"; error: string };
