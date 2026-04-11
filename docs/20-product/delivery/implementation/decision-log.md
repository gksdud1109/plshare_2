# Decision Log

이 문서는 확정된 제품/구현 결정을 기록한다.

기록 규칙:
- 각 항목은 `date | role | model | decision | rationale | affected` 형식을 따른다
- `model` 필드는 필수다
- 허용 모델 표기: `Gemini`, `Claude`, `Codex CLI`, `Codex App`

기존 결정:
- `2026-04-11 | Product Owner | Gemini | Finalized tech stack: Next.js + Kotlin/Spring Boot + Supabase | Ensure scalability for normalization engine and platform adapters | docs/20-product/delivery/implementation/tech-stack-decision-v0.1.md`
- `2026-04-11 | Product Designer | Gemini | Locked "Luxury / Refined" aesthetic direction for PA products | Enhance 'Asset' value perception | docs/20-product/design/ux/design-direction-v0.1.md`
- `2026-04-11 | Product Designer | Gemini | Defined core UX flows for Spotify-to-Apple bridge | Guide frontend implementation from locked PRD | docs/20-product/design/ux/user-flows-v0.1.md`
- `2026-04-11 | Product Owner | Gemini | Sync PRD v0.2, Strategy v0.1, Priority v0.1 with frozen MVP scope (Option A) | Eliminate strategic tensions and focus on Spotify -> Apple asymmetric bridge | docs/20-product/strategy/`
- `2026-04-11 | Product Owner | Gemini | Freeze MVP to Spotify import + Apple export | Narrow scope and reduce adapter risk | docs/20-product/strategy/prd-v0.2.md`


기존 결정:
- `2026-04-11 | Product Owner | Gemini | Sharpened prd-v0.2 MVP scope and moved B2B to roadmap | Align PRD with Product Strategy v0.1 | docs/20-product/strategy/prd-v0.2.md`
- `2026-04-11 | Product Owner | Gemini | Aligned product-strategy-v0.1 handoff paths with the new directory structure | Keep strategy docs consistent with the restructured docs tree | docs/20-product/strategy/product-strategy-v0.1.md`
- `2026-04-11 | Product Owner | Gemini | Synced mvp-feature-priority-v0.1 Tier 0/1/2/3 with P0/P1/P2 framework | Make release prioritization match the strategy framework | docs/20-product/strategy/mvp-feature-priority-v0.1.md`
- `2026-04-11 | Reviewer / Operator | Codex CLI | Propagated frozen MVP baseline across docs and agent rules | Keep all current docs and roles aligned to one execution baseline | docs/README.md, docs/10-operations/, docs/20-product/`
