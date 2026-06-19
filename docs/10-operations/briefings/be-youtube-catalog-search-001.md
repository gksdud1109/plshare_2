# Briefing: be-youtube-catalog-search-001

## Objective

Implement the missing P0 capability to search-resolve tracks to YouTube without requiring YouTube account OAuth.

## Context

The existing `/api/catalog/tracks` endpoint exposes a curated, preverified catalog. Keep it. Add a separate authenticated search path backed by the YouTube Data API key tier described in Product Baseline v2.1.

## Contract

Add:

```text
GET /api/catalog/youtube/search?q=<query>
```

Return a bounded result list containing:

- stable selection id
- `videoId`
- title
- channel/artist hint
- thumbnail URL

The selection id must be accepted by asset composition without trusting arbitrary client metadata. A server-side signed token or another deterministic, validated representation is acceptable. Document the choice in code.

## Requirements

- Use `YOUTUBE_API_KEY` in non-demo mode; do not request a user OAuth scope for search.
- Keep explicit demo behavior deterministic.
- Reject blank and oversized queries.
- Cap result count.
- Account for `search.list` quota cost before the upstream call.
- Surface missing configuration, quota exhaustion, and upstream errors.
- Preserve existing curated-track composition and mood-video behavior.
- Add focused unit tests.

## Constraints

- No unofficial YouTube Music APIs.
- No silent fixture fallback outside explicit demo profile.
- No changes to Spotify or Apple integrations.
- Do not edit frontend files.

## Validation

Run:

```bash
cd backend
./gradlew test --tests '*YouTubeCatalogSearchServiceTest' --tests '*CatalogServiceTest'
```

