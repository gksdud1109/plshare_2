# Decision Log

이 문서는 확정된 제품/구현 결정을 기록한다.

기록 규칙:
- 각 항목은 `date | role | model | decision | rationale | affected` 형식을 따른다
- `model` 필드는 필수다
- 허용 모델 표기: `Gemini`, `Claude`, `Codex CLI`, `Codex App`

- `2026-04-11 | Backend Engineer | Gemini | Finalized API contracts with Idempotency (X-Idempotency-Key) | Ensure data integrity and prevent duplicate processing of assets | docs/20-product/delivery/implementation/open-issues.md`
- `2026-04-11 | Backend Engineer | Gemini | Adopted Async + Transaction Isolation for Normalization Engine | Prevent external API latency from exhausting DB connections and isolate failures | backend/src/main/kotlin/com/plshare/backend/application/service/NormalizationEngine.kt`
- `2026-04-11 | Backend Engineer | Gemini | Locked ISRC as the primary normalization identifier | Ensure cross-platform asset matching consistency | backend/src/main/kotlin/com/plshare/backend/domain/entity/Asset.kt`
- `2026-04-11 | Product Designer | Gemini | Locked "Luxury / Refined" aesthetic direction for PA products | Enhance 'Asset' value perception | docs/20-product/design/ux/design-direction-v0.1.md`
- `2026-04-11 | Product Designer | Gemini | Defined core UX flows for Spotify-to-Apple bridge | Guide frontend implementation from locked PRD | docs/20-product/design/ux/user-flows-v0.1.md`
- `2026-04-11 | Product Owner | Gemini | Sync PRD v0.2, Strategy v0.1, Priority v0.1 with frozen MVP scope (Option A) | Eliminate strategic tensions and focus on Spotify -> Apple asymmetric bridge | docs/20-product/strategy/`
- `2026-04-11 | Product Owner | Gemini | Freeze MVP to Spotify import + Apple export | Narrow scope and reduce adapter risk | docs/20-product/strategy/prd-v0.2.md`


- `2026-04-13 | Reviewer / Operator | claude-sonnet-4-6 | Reviewed and approved po-strategy-sync-001 (Sync strategy docs to frozen MVP decisions) | All done criteria met: B2B removed from MVP framing, Human-authenticity framed as P1, MVP frozen to Spotify import + Apple export | docs/20-product/strategy/prd-v0.2.md`

기존 결정:
- `2026-04-11 | Product Owner | Gemini | Sharpened prd-v0.2 MVP scope and moved B2B to roadmap | Align PRD with Product Strategy v0.1 | docs/20-product/strategy/prd-v0.2.md`
- `2026-04-11 | Product Owner | Gemini | Aligned product-strategy-v0.1 handoff paths with the new directory structure | Keep strategy docs consistent with the restructured docs tree | docs/20-product/strategy/product-strategy-v0.1.md`
- `2026-04-11 | Product Owner | Gemini | Synced mvp-feature-priority-v0.1 Tier 0/1/2/3 with P0/P1/P2 framework | Make release prioritization match the strategy framework | docs/20-product/strategy/mvp-feature-priority-v0.1.md`
- `2026-04-11 | Reviewer / Operator | Codex CLI | Propagated frozen MVP baseline across docs and agent rules | Keep all current docs and roles aligned to one execution baseline | docs/README.md, docs/10-operations/, docs/20-product/`
- `2026-04-14 | Reviewer / Operator | Claude | Auto-approved task po-strategy-sync-001 (Sync strategy docs to frozen MVP decisions) | Artifacts exist, are non-empty, and log has no error signal; next role=product-designer | docs/20-product/strategy/prd-v0.2.md,docs/20-product/strategy/product-strategy-v0.1.md docs/20-product/strategy/mvp-feature-priority-v0.1.md`
- `2026-04-14 | Product Owner | Codex App | Approved "Emotional Context" as the canonical PRD naming | Keep emotional logger/context terminology consistent across strategy, UX, and requirements docs | docs/20-product/strategy/prd-v0.2.md`
- `2026-04-14 | Product Owner | Gemini | Maintained the asymmetric Apple strategy after reviewing a YouTube Music-centered export alternative | YouTube Music increases normalization and policy risk while weakening the Emotional Context asset thesis for MVP | docs/20-product/strategy/platform-direction-review-v0.1.md`
- `2026-05-03 | Product Designer | Claude | Locked 9-route information architecture for MVP UX (/, /auth/spotify, /import, /import/[id]/progress, /assets, /assets/[id], /assets/[id]/export, /assets/[id]/export/result, /share/[token]) | One-to-one mapping with Next.js App Router; covers import → assetize → export → share with Refined ProgressNarrative tone | docs/20-product/design/ux/user-flows-v0.1.md, docs/20-product/design/ux/screen-specs-v0.1.md`
- `2026-05-03 | Backend Engineer | Claude | Adopted asymmetric adapter contracts (PlaylistReadAdapter / PlaylistWriteAdapter) with Spotify Read + Apple Write split, ExportJob 7-state machine, ISRC primary + fuzzy(0.5/0.3/0.2) fallback | Enforce Apple-strategy at code level, isolate adapter risk per platform | docs/20-product/data/adapter-architecture-v0.1.md, docs/20-product/data/canonical-track-normalization-v0.1.md`
- `2026-05-03 | Backend Engineer | Claude | Demo profile via H2 in-memory + MockSpotifyClient + MockAppleMusicWriteAdapter; SpotifyClient extracted to interface with @Profile("demo")/@Profile("!demo") split | Enable single-command demo (./gradlew bootRun) without external credentials, preserve real adapter for prod | backend/src/main/kotlin/com/plshare/backend/, backend/src/main/resources/application-demo.yml`
- `2026-05-03 | Frontend Engineer | Claude | Implemented 9-route Next.js demo with fixture fallback and idempotency-aware API client; ProgressNarrative replaces spinners per Refined design direction | Make the full Spotify→Emotional Context→Apple→Share loop runnable end-to-end, BE-optional via fixture mode | frontend/src/app/, frontend/src/lib/api/, frontend/src/components/ui/`
