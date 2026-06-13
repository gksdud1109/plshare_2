/**
 * YouTube playlist API client.
 * GET /api/youtube/playlists — returns mock fixture of 2 playlists.
 * Falls back to demoYoutubePlaylists fixture when the BE is unreachable.
 */

import type { SpotifyPlaylist } from "@/types/asset";
import { apiFetch } from "./client";

export async function listYoutubePlaylists(): Promise<SpotifyPlaylist[]> {
  return apiFetch<SpotifyPlaylist[]>("/api/youtube/playlists");
}
