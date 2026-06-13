# Handoff: Backend Core Domain to API Implementation v0.1

## Task
Complete the integration of the Spotify Normalization Engine and expose the API endpoints for the Frontend.

## Receiver Role
Backend Engineer (Next session)

## Recommended Model
Gemini / Codex App

## Input
- `backend/src/main/kotlin/com/plshare/backend/domain/` (Core Entities)
- `backend/src/main/kotlin/com/plshare/backend/application/` (Services with Idempotency)
- `docs/20-product/delivery/implementation/open-issues.md` (Finalized API Contracts)
- `skills/toss-payments-review-skill.md` (Quality Standard)

## Current Progress (Done)
- [x] Backend Scaffolding (Kotlin 1.9, Spring Boot 3.2)
- [x] DB Schema: `ImportJob`, `Asset`, `Track` (with Idempotency Key)
- [x] Spotify Client: OAuth and Playlist Metadata Retrieval (with Timeouts)
- [x] Normalization Engine: ISRC-based matching and Async processing.
- [x] Core Domain Committed to `feat/be/domain-implementation`

## Next To-Do
- [ ] **API Controller Implementation**: Expose `POST /api/v1/imports` and `GET /api/v1/imports/{id}` as defined in `open-issues.md`.
- [ ] **Config Setup**: Add `@EnableAsync` and `@EnableJpaRepositories` to `BackendApplication`.
- [ ] **Mock Test Suite**: Add tests for `ImportService` idempotency and `NormalizationEngine` failure scenarios (using Kotest/Mockk).
- [ ] **Apple Music Adapter**: Start designing the `AppleMusicClient` for the Export loop.

## Fixed Decisions
- Must use `X-Idempotency-Key` for all mutation requests (POST).
- External API calls MUST NOT be inside a DB transaction block.
- Always log external API failures with specific error codes.

## Next Expected Role
Frontend Engineer (for full integration)
