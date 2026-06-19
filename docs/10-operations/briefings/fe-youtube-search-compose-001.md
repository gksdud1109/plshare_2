# Briefing: fe-youtube-search-compose-001

## Objective

Expose backend YouTube catalog search in `/create` so users can build a tracklist beyond the curated starter catalog.

## Requirements

- Add a debounced or explicit-submit search input.
- Render loading, no-result, quota/configuration, and generic upstream-error states.
- Let users select search results and curated catalog tracks in one composition.
- Preserve result identity exactly as defined by the backend contract.
- Keep selected order stable.
- Keep existing idempotent asset composition and mood-video creation working.
- Avoid triggering an upstream search on every keystroke.

## Constraints

- Do not add React Bits or unrelated visual redesign.
- Do not add YouTube account OAuth for catalog search.
- Do not hide production errors behind demo fixtures.
- Do not edit import, gift, or profile pages.

## Validation

```bash
cd frontend
npm run build
npm run lint
```

