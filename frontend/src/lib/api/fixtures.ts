/**
 * Demo fixtures used when the BE is unreachable, so the UI is always inspectable.
 * Pure data only — no fetch calls here.
 */

import type {
  AssetDetail,
  AssetSummary,
  ExportJobStatus,
  ExportMapping,
  ExportResult,
  ImportJobStatus,
  SharedAsset,
  SpotifyPlaylist,
} from "@/types/asset";

export const demoPlaylists: SpotifyPlaylist[] = [
  {
    id: "pl_morning",
    name: "새벽 다섯시의 산책",
    description: "혼자 걷던 새벽들을 모아둔 플레이리스트",
    imageUrl:
      "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&q=80",
    trackCount: 18,
  },
  {
    id: "pl_drive",
    name: "해안도로 드라이브",
    description: "창문 내리고 듣고 싶은 노래들",
    imageUrl:
      "https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=600&q=80",
    trackCount: 24,
  },
  {
    id: "pl_cafe",
    name: "오후의 카페",
    description: "햇살이 길게 드는 오후를 위한",
    imageUrl:
      "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=600&q=80",
    trackCount: 12,
  },
  {
    id: "pl_rain",
    name: "비 오는 창가",
    description: "조용히 흘러가는 시간",
    imageUrl:
      "https://images.unsplash.com/photo-1438449805896-28a666819a20?w=600&q=80",
    trackCount: 16,
  },
];

export const demoAssets: AssetSummary[] = [
  {
    id: "a_morning",
    title: "새벽 다섯시의 산책",
    coverUrl:
      "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&q=80",
    trackCount: 18,
    createdAt: "2026-04-21T05:13:00Z",
    hasEmotionalContext: true,
  },
  {
    id: "a_drive",
    title: "해안도로 드라이브",
    coverUrl:
      "https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=600&q=80",
    trackCount: 24,
    createdAt: "2026-04-15T18:02:00Z",
    hasEmotionalContext: false,
  },
  {
    id: "a_cafe",
    title: "오후의 카페",
    coverUrl:
      "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=600&q=80",
    trackCount: 12,
    createdAt: "2026-03-30T15:40:00Z",
    hasEmotionalContext: true,
  },
];

const demoTrackList = [
  { id: "t1", name: "Holocene", artist: "Bon Iver", isrc: "USJAY1100013", durationMs: 337000 },
  { id: "t2", name: "Re: Stacks", artist: "Bon Iver", isrc: "USJAY0800014", durationMs: 397000 },
  { id: "t3", name: "Saturn", artist: "Sleeping at Last", isrc: "TCAFG1454017", durationMs: 275000 },
  { id: "t4", name: "Avril 14th", artist: "Aphex Twin", isrc: "GBBPW0100177", durationMs: 124000 },
  { id: "t5", name: "Spiegel im Spiegel", artist: "Arvo Pärt", isrc: "DEH849600028", durationMs: 481000 },
  { id: "t6", name: "An Ending (Ascent)", artist: "Brian Eno", isrc: "GBAYE8300013", durationMs: 264000 },
];

export function buildDemoAssetDetail(id: string): AssetDetail {
  return {
    id,
    title: "새벽 다섯시의 산책",
    description: "혼자 걷던 새벽들을 모아둔 기록",
    diaryText:
      "처음에는 잠이 오지 않아서 걷기 시작했다. 그러다 새벽 공기에서만 들리는 소리가 있다는 걸 알게 됐다. 이 노래들은 그 시간을 함께 걸어준 친구들.",
    emotionTags: ["새벽", "그리움", "산책"],
    coverUrl:
      "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&q=80",
    tracks: demoTrackList,
  };
}

export function buildDemoSharedAsset(token: string): SharedAsset {
  const detail = buildDemoAssetDetail(token);
  return { ...detail };
}

export const demoImportProgression: ImportJobStatus[] = [
  { status: "queued", progress: 5 },
  { status: "matching", progress: 28 },
  { status: "matching", progress: 56 },
  { status: "matching", progress: 82 },
  { status: "completed", progress: 100, assetId: "a_morning" },
];

export function buildDemoExportStatus(step: number): ExportJobStatus {
  const total = demoTrackList.length;
  const completed = Math.min(total, Math.max(0, step));
  const mappings: ExportMapping[] = demoTrackList.map((t, i) => {
    if (i >= completed) return { trackId: t.id, status: "matched" };
    const r = (i * 7) % 10;
    const s: ExportMapping["status"] =
      r < 7 ? "matched" : r < 9 ? "alternative" : "failed";
    return {
      trackId: t.id,
      appleTrackId: s === "failed" ? undefined : `apple_${t.id}`,
      status: s,
    };
  });
  const matched = mappings.filter((m) => m.status === "matched").length;
  const failed = mappings.filter((m) => m.status === "failed").length;
  return {
    status: completed >= total ? "completed" : "matching",
    totalTracks: total,
    matchedTracks: matched,
    failedTracks: failed,
    mappings,
  };
}

export const demoExportResult: ExportResult = {
  externalPlaylistId: "apl_demo_001",
  externalUrl: "https://music.apple.com/us/playlist/demo",
  matchedTracks: 5,
  failedTracks: 1,
};

export const EMOTION_TAGS = [
  "그리움",
  "새벽",
  "여행",
  "햇빛",
  "비",
  "카페",
  "운전",
  "추억",
] as const;
