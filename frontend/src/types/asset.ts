/**
 * Domain types — aligned with the BE contract for the Import -> Assetize -> Export loop.
 */

export type AssetSource = "spotify" | "youtube" | "apple_music";

/**
 * Spotify-side playlist (pre-import). Returned by GET /api/playlists.
 */
export interface SpotifyPlaylist {
  id: string;
  name: string;
  description?: string;
  imageUrl?: string;
  trackCount: number;
}

/**
 * Track inside an asset detail (GET /api/assets/{id}).
 */
export interface AssetTrack {
  id: string;
  name: string;
  artist: string;
  isrc?: string;
  durationMs: number;
  youtubeVideoId?: string | null;
}

/**
 * Asset list item (GET /api/assets).
 */
export interface AssetSummary {
  id: string;
  title: string;
  coverUrl?: string;
  trackCount: number;
  createdAt: string;
  hasEmotionalContext: boolean;
}

/**
 * Asset detail (GET /api/assets/{id}).
 */
export interface AssetDetail {
  id: string;
  title: string;
  description?: string;
  diaryText?: string;
  emotionTags: string[];
  coverUrl?: string;
  tracks: AssetTrack[];
}

export interface ImportJob {
  jobId: string;
  status: "queued" | "matching" | "completed" | "failed";
}

export interface ImportJobStatus {
  status: "queued" | "matching" | "completed" | "failed";
  progress: number;
  assetId?: string;
}

export interface ExportJob {
  jobId: string;
  status: "queued" | "matching" | "completed" | "failed";
}

export type ExportMappingStatus = "matched" | "alternative" | "failed";

export interface ExportMapping {
  trackId: string;
  appleTrackId?: string;
  // YouTube Music corridor: per-track match detail for manual review.
  title?: string;
  artist?: string;
  videoId?: string | null;
  matchedTitle?: string | null;
  confidence?: number | null;
  status: ExportMappingStatus;
  reviewed?: boolean;
}

/** Alternative video offered when reviewing a low-confidence/failed match. */
export interface ExportCandidate {
  videoId: string;
  title: string;
  channelTitle?: string | null;
}

export type ExportJobStatusValue =
  | "queued"
  | "matching"
  | "ready"
  | "executing"
  | "completed"
  | "partial"
  | "failed"
  | "canceled";

export interface ExportJobStatus {
  status: ExportJobStatusValue;
  totalTracks: number;
  matchedTracks: number;
  failedTracks: number;
  mappings: ExportMapping[];
  externalPlaylistId?: string | null;
  externalUrl?: string | null;
}

export interface ExportResult {
  externalPlaylistId: string;
  externalUrl: string;
  matchedTracks: number;
  failedTracks: number;
}

export interface ShareLink {
  shareToken: string;
  url: string;
}

/**
 * Public share payload (GET /api/share/{token}).
 * Mirrors AssetDetail-ish shape, exposed to anonymous viewers.
 */
export interface SharedAsset {
  id: string;
  title: string;
  description?: string;
  diaryText?: string;
  emotionTags: string[];
  coverUrl?: string;
  tracks: AssetTrack[];
}

// Legacy type kept for the existing AssetCard component until we migrate it.
export interface PlaylistAsset {
  id: string;
  title: string;
  subtitle?: string;
  curatorNote?: string;
  coverUrl?: string;
  source: AssetSource;
  createdAt: string;
  tracks: Array<{
    id: string;
    title: string;
    artist: string;
    album?: string;
    durationMs: number;
    coverUrl?: string;
    externalUrls: Partial<Record<AssetSource, string>>;
  }>;
}

export type AsyncState<T> =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "success"; data: T }
  | { status: "error"; error: string };
