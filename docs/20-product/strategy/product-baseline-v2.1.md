# Product Baseline v2.1

- Effective date: `2026-06-13`
- Status: `CURRENT`
- Supersedes: `product-baseline-v2.md` (gift-first pivot)

## Why v2.1 (forcing function)

v2 named "Spotify to YouTube Music conversion as the primary corridor". Credential
verification (2026-06-13) showed that corridor is not viable for a solo / pre-business
build:

- **Spotify** — since 2024-11-27 a newly created app **cannot read public playlists**
  via client-credentials (404 in development mode); user-auth is capped at **5 users +
  the app owner must have Premium**; extended access requires a **registered business +
  250k MAU**. → Spotify is unusable as a source.
- **Apple Music** — needs the **$99/yr** Apple Developer Program; the developer token
  only reaches the catalog. User library read/write needs a **Music-User-Token** via a
  MusicKit JS browser flow (not built).
- **YouTube** — the only platform fully feasible solo: an **API key resolves any track
  to a playable video (no OAuth)**, anyone plays a YouTube link **without an account**,
  and account export works via OAuth (≤100 test users unverified).

→ Pivot: stop centering cross-platform *conversion*; center the **emotional taste
asset + gift**, with **YouTube as the universal playback substrate**.

## Product

plshare2 is a layer for capturing your music taste with emotional context and
**gifting / sharing it so anyone can listen instantly — no app, no account** (YouTube
playback). Taste becomes a shareable identity and an emotional gift.

## Two integration tiers (the design constraint)

| Tier | User OAuth | Solo-feasible | Role |
|---|---|---|---|
| **Catalog / link** — resolve a track to a YouTube link, embed-play | No (API key) | Yes | the playback + share/gift backbone |
| **Account** — read my library / write to my account | Yes | YouTube only | optional enhancement |

The product centers on the catalog/link tier. The account tier is additive and, for
Spotify/Apple, dormant (see below).

## P0 (build now — no external blockers)

1. Google sign-in and a persistent plshare identity
2. Build a taste asset: add tracks (search-resolve to YouTube) or import a YouTube/YTM
   playlist (OAuth); attach Emotional Context (diary, mood tags, photos)
3. **Emotional gift** — wrap an asset + story, recipient unboxes track-by-track and
   **plays via YouTube embeds without logging in** (headline / wedge)
4. Social layer: feed, like, comment, follow, ranking
5. Beautiful share surfaces (gift page, share page, OG preview card)

## P1 (account tier — YouTube only)

- "Save this to my YouTube/YTM" (OAuth export). Works ≤100 users unverified; needs
  OAuth consent-screen verification to scale.

## Dormant (code preserved, feature-flag OFF — activate on business/membership)

- Spotify import/export, Apple import/export, cross-platform conversion. The
  conversion engine, matching, low-confidence review, and adapters are **kept** and
  gated by a feature flag. Activate when: a registered business / Spotify extended
  access exists, or the Apple Developer Program ($99) + MusicKit JS user-token flow is
  built.

## Parked ideas (backlog — not yet scheduled)

Gift-native: premium wrap/unboxing themes, advent reveal, voice-note attach, physical
mixtape card (QR/NFC print-on-demand), scheduled/timed gifts. Identity: Taste Card
(취향 명함), Taste Wrapped recap. Loop: taste reply / correspondence. Monetization is
designed now, charged once a business entity + payment processor exist.

## Out Of Scope

- In-app full music playback (use YouTube embeds)
- On-chain minting · B2B training-data packaging
- Direct messages and realtime listening rooms

## Product Rules

- Google is the service identity. YouTube is the playback + resolve substrate.
- A user may only mutate assets and jobs they own.
- Production failures must never be replaced by demo success data.
- Demo fixtures are allowed only when explicit demo mode is enabled.
- **Spotify/Apple account integration stays feature-flag OFF until business/membership
  is secured; the code is preserved, not deleted.**
- The shared/gift artifact is a first-class design surface — it must be beautiful
  enough to share unprompted.

## Technical Baseline

| Layer | Current choice |
|---|---|
| Frontend | Next.js App Router, TypeScript, Nocturne design system |
| Backend | Kotlin, Spring Boot |
| Database | PostgreSQL with Flyway; H2 only in demo |
| Authentication | Google OAuth, backend-issued application session |
| Resolve / playback | YouTube Data API (API key resolve + OAuth write); YouTube embeds |
| Storage | S3-compatible storage; local filesystem only in demo |
| Deployment target | Local docker-compose now; Vercel frontend + Cloud Run backend later |

## Delivery Gate

A task is not complete merely because a demo screen exists. Completion requires its
validation command to pass, production failures to remain visible, and the current API
contract to be satisfied.
