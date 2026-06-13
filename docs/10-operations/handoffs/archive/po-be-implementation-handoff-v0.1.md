# Handoff: Backend Implementation (Normalization & Adapters) v0.1

## Task
Establish the core backend architecture using Kotlin/Spring Boot to handle Spotify-to-Apple music normalization and export.

## Receiver Role
Backend Engineer

## Recommended Model
Codex App

## Input
- `docs/20-product/strategy/prd-v0.2.md`
- `docs/20-product/delivery/implementation/tech-stack-decision-v0.1.md` (Tech Stack & Responsibility Mapping)
- `docs/20-product/delivery/implementation/open-issues.md` (Initial API Contracts)

## Output
- Kotlin/Spring Boot project structure.
- Spotify OAuth and Metadata retrieval service.
- ISRC-based Normalization Engine.
- Apple Music Playlist Export Job (Async).
- Database Schema for the Ledger.

## Current Fixed Decisions
- **Core Loop:** Spotify Import -> Normalize -> Save (Ledger) -> Apple Music Export.
- **Primary Identifier:** ISRC. Use platform IDs as fallback.
- **Asymmetric Bridge:** MVP focuses on Spotify (Read) and Apple Music (Write).
- **Managed DB:** Use Supabase PostgreSQL.

## Do Not Change
- Do not use a Node.js backend for core business logic (Normalization/Adapters).
- Do not build an in-app streaming engine.
- Do not implement B2B features yet.

## Done When
- A user can successfully import a Spotify playlist and see a normalized track list in the DB.
- A background job can successfully create a playlist in Apple Music using the normalized data.
- API endpoints for the Frontend (Next.js) are documented and functional.

## Next Role
Frontend Engineer (for full integration)
