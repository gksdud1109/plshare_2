import type { PlaylistSummary } from "@/types/asset";
import { ApiError, apiFetch } from "./client";

interface PlaylistSummaryResponse {
  id: string;
  name: string;
  description?: string;
  coverUrl?: string;
  trackCount: number;
}

function toPlaylistSummary(playlist: PlaylistSummaryResponse): PlaylistSummary {
  return {
    id: playlist.id,
    name: playlist.name,
    description: playlist.description,
    imageUrl: playlist.coverUrl,
    trackCount: playlist.trackCount,
  };
}

async function listSourcePlaylists(path: string): Promise<PlaylistSummary[]> {
  const playlists = await apiFetch<PlaylistSummaryResponse[]>(path);
  return playlists.map(toPlaylistSummary);
}

export function listPlaylists(): Promise<PlaylistSummary[]> {
  return listSourcePlaylists("/api/playlists");
}

export function listYoutubePlaylists(): Promise<PlaylistSummary[]> {
  return listSourcePlaylists("/api/youtube/playlists");
}

function errorDetails(value: unknown): { code: string; message: string } {
  const body =
    value instanceof ApiError
      ? value.body
      : value;
  if (body === null || typeof body !== "object") {
    return { code: "", message: "" };
  }
  const { code, errorCode, message, errorMessage } = body as {
    code?: unknown;
    errorCode?: unknown;
    message?: unknown;
    errorMessage?: unknown;
  };
  return {
    code:
      typeof code === "string"
        ? code
        : typeof errorCode === "string"
          ? errorCode
          : "",
    message:
      typeof message === "string"
        ? message
        : typeof errorMessage === "string"
          ? errorMessage
          : "",
  };
}

export function indicatesMissingYoutubeScope(value: unknown): boolean {
  if (
    value instanceof ApiError &&
    (value.status === 401 || value.status === 403)
  ) {
    return true;
  }

  const { code, message } = errorDetails(value);
  const normalizedCode = code.toUpperCase();
  if (
    normalizedCode.includes("YOUTUBE") &&
    /(AUTH|CONSENT|GRANT|PERMISSION|SCOPE)/.test(normalizedCode)
  ) {
    return true;
  }

  const normalizedMessage = message.toLowerCase();
  return (
    normalizedMessage.includes("youtube") &&
    /(authorization|consent|grant|permission|scope|권한|동의|인증|연결)/i.test(
      normalizedMessage,
    )
  );
}
