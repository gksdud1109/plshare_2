# Product Baseline v2

- Effective date: `2026-06-10`
- Status: `CURRENT`
- Supersedes: Spotify-to-Apple-only MVP baseline

## Product

plshare2 is a social taste-asset service that turns a playlist into a
platform-independent asset with Emotional Context and lets people share,
rank, and gift that asset.

## P0

1. Google sign-in and a persistent plshare user identity
2. Spotify or YouTube Music playlist import
3. Emotional Context assetization
4. Social feed sharing, likes, comments, and follows
5. Spotify to YouTube Music conversion as the primary corridor

YouTube Music export is quota-gated. Import and social sharing must remain
usable when write quota is unavailable.

## P1

- Playlist and user ranking
- Emotional gift flow
- Apple Music export
- Manual review for low-confidence track matches

## Out Of Scope

- In-app full music playback
- On-chain minting
- B2B training-data packaging
- Direct messages and realtime listening rooms

## Product Rules

- Google is the service identity. Spotify and YouTube are connected providers.
- A user may only mutate assets and jobs they own.
- Production failures must never be replaced by demo success data.
- Demo fixtures are allowed only when explicit demo mode is enabled.
- Product scope is controlled by this document and
  `platform-strategy-v2-research-v0.1.md`.

## Technical Baseline

| Layer | Current choice |
|---|---|
| Frontend | Next.js App Router, TypeScript |
| Backend | Kotlin, Spring Boot |
| Database | PostgreSQL with Flyway; H2 only in demo |
| Authentication | Google OAuth, backend-issued application session |
| Provider auth | Spotify OAuth and Google incremental YouTube scopes |
| Storage | S3-compatible storage; local filesystem only in demo |
| Deployment target | Vercel frontend, Cloud Run backend |

## Delivery Gate

A task is not complete merely because a demo screen exists. Completion
requires its validation command to pass, production failures to remain
visible, and the current API contract to be satisfied.
