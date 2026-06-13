/**
 * parsePlaylistLink — pure function, no side-effects, no fetch calls.
 *
 * Supported patterns
 * ──────────────────
 * Spotify:
 *   https://open.spotify.com/playlist/{id}
 *   https://open.spotify.com/playlist/{id}?si=anything&utm_source=…
 *   (mobile share URL is same domain, just adds query params — handled below)
 *
 * YouTube (standard):
 *   https://www.youtube.com/playlist?list={id}
 *   https://youtube.com/playlist?list={id}
 *   https://m.youtube.com/playlist?list={id}   ← mobile
 *
 * YouTube Music:
 *   https://music.youtube.com/playlist?list={id}
 *
 * Edge cases handled:
 *   - Extra query params (si=, utm_*, feature=, app=, cbrd=…) — ignored
 *   - Query param ordering: list= may appear after other params
 *   - Trailing slashes
 *   - http:// variants
 *   - Malformed URL or missing playlist ID → returns null
 *   - Blank / non-URL string → returns null
 */

export type ParsedPlaylistLink =
  | { platform: "spotify"; playlistId: string }
  | { platform: "youtube"; playlistId: string };

const SPOTIFY_RE =
  /open\.spotify\.com\/playlist\/([A-Za-z0-9]{10,})/;

const YOUTUBE_DOMAINS = new Set([
  "www.youtube.com",
  "youtube.com",
  "m.youtube.com",
  "music.youtube.com",
]);

export function parsePlaylistLink(
  raw: string,
): ParsedPlaylistLink | null {
  const trimmed = raw.trim();
  if (!trimmed) return null;

  // ── Spotify ──────────────────────────────────────────────────────────
  const spotifyMatch = SPOTIFY_RE.exec(trimmed);
  if (spotifyMatch) {
    return { platform: "spotify", playlistId: spotifyMatch[1] };
  }

  // ── YouTube / YouTube Music ───────────────────────────────────────────
  // Parse via URL constructor so query-param ordering doesn't matter.
  let url: URL;
  try {
    // Prepend scheme if missing so URL() can parse bare hostnames.
    const candidate = /^https?:\/\//i.test(trimmed)
      ? trimmed
      : `https://${trimmed}`;
    url = new URL(candidate);
  } catch {
    return null; // Not a valid URL at all.
  }

  if (YOUTUBE_DOMAINS.has(url.hostname)) {
    const listId = url.searchParams.get("list");
    if (listId && listId.length > 0) {
      return { platform: "youtube", playlistId: listId };
    }
  }

  return null;
}
