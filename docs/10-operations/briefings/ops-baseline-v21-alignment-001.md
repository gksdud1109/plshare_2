# Briefing: ops-baseline-v21-alignment-001

## Objective

Align active repository guidance with `docs/20-product/strategy/product-baseline-v2.1.md`.

## Read First

- `docs/20-product/strategy/product-baseline-v2.1.md`
- `docs/10-operations/feedback-fix-completion-report.md`
- `AGENTS.md`
- `docs/20-product/strategy/README.md`

## Required Changes

1. Update `AGENTS.md` so the current product baseline is v2.1.
2. Describe P0 as Google identity, YouTube-backed taste asset creation, Emotional Context, gift/unboxing, social, and share surfaces.
3. State that Spotify and Apple account integrations are dormant and feature-flagged.
4. State that YouTube account export is P1.
5. Update the strategy README's current-document list, fixed decisions, and reading order.

## Constraints

- Do not edit product-baseline-v2.1.md.
- Do not change code.
- Do not present direct messages, B2B data packaging, on-chain minting, Spotify, or Apple as MVP scope.
- Preserve concise repository-instruction style.

## Validation

Run the task `validate_cmd` and inspect `git diff -- AGENTS.md docs/20-product/strategy/README.md`.

