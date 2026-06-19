# Briefing: fe-youtube-import-entry-001

## Objective

Connect the existing YouTube playlist APIs to the import UI and remove Spotify-first copy from the active P0 path.

## Existing Backend

- `GET /api/youtube/playlists`
- `POST /api/imports` with `{ playlistId, sourcePlatform: "youtube" }`
- incremental Google consent entry: `/api/auth/google/start?scope=youtube`

## Requirements

- Make YouTube/YouTube Music the primary import source.
- Load YouTube playlists using a typed API client.
- If the backend indicates missing YouTube scope, show an explicit consent CTA using incremental Google OAuth.
- Preserve the selected source into the progress route and `startImport`.
- Keep explicit loading, empty, error, and retry states.
- Remove active Spotify-first language from this path.
- Keep production errors visible; use fixtures only when demo mode is explicitly enabled.

## Constraints

- Do not delete dormant Spotify code.
- Do not change backend contracts.
- Do not edit create or gift pages.
- Preserve current visual system and accessibility.

## Validation

```bash
cd frontend
npm run build
npm run lint
```

