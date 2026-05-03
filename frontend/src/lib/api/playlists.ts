/**
 * Playlist asset endpoints. Paths are placeholders until the BE schema lands —
 * keep this file as the single surface the UI imports from, so the swap later
 * is mechanical.
 */

import type { PlaylistAsset } from "@/types/asset";
import { apiFetch } from "./client";

export async function importFromSpotify(
  spotifyUrl: string,
): Promise<PlaylistAsset> {
  return apiFetch<PlaylistAsset>("/api/imports/spotify", {
    method: "POST",
    body: JSON.stringify({ url: spotifyUrl }),
  });
}

export async function getAsset(id: string): Promise<PlaylistAsset> {
  return apiFetch<PlaylistAsset>(`/api/assets/${id}`);
}

export async function exportToAppleMusic(
  id: string,
): Promise<{ deepLink: string }> {
  return apiFetch<{ deepLink: string }>(`/api/assets/${id}/export/apple`, {
    method: "POST",
  });
}
