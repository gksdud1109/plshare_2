# Briefing

## Task ID
be-adapter-arch-001

## Receiver Role
Backend Engineer

## Recommended Model
Codex CLI

## Input Docs
- `docs/20-product/strategy/prd-v0.2.md`
- `docs/20-product/strategy/product-strategy-v0.1.md`
- `docs/20-product/requirements/functional-requirements-v0.2.md`
- `docs/20-product/requirements/non-functional-requirements-v0.2.md`
- `docs/20-product/strategy/platform-direction-review-v0.1.md`

## Output Docs
- `docs/20-product/data/adapter-architecture-v0.1.md`
- `docs/20-product/data/canonical-track-normalization-v0.1.md`

## Fixed Decisions
- This task is document-first only
- Platform adapter direction must follow the PO review result
- Product scope is portability + assetization, not in-app playback

## Do Not Change
- Do not implement backend code in this task
- Do not design B2B export architecture as current scope
- Do not assume strong sensor-based proof collection

## Done Criteria
- Adapter direction is clearly documented according to the approved platform strategy
- Canonical track identity and mapping flow are documented
- Failure, retry, and export job states are defined
- Implementation is deferred until user review approval after the document is completed
