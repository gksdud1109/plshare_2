import type { SpotifyPlaylist } from "@/types/asset";
import { apiFetch } from "./client";

export async function listPlaylists(): Promise<SpotifyPlaylist[]> {
  return apiFetch<SpotifyPlaylist[]>("/api/playlists");
}
