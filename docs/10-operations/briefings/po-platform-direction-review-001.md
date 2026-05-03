# Briefing

## Task ID
po-platform-direction-review-001

## Receiver Role
Product Owner

## Recommended Model
Gemini

## Input Docs
- `docs/20-product/strategy/prd-v0.2.md`
- `docs/20-product/strategy/product-strategy-v0.1.md`
- `docs/20-product/strategy/mvp-feature-priority-v0.1.md`
- `docs/20-product/research/market-research-v0.3.md`

## Output Docs
- `docs/20-product/strategy/platform-direction-review-v0.1.md`

## Fixed Decisions
- The term `Emotional Context` is approved as the canonical naming for the human-written context layer
- Current frozen direction must be explicitly re-evaluated, not silently assumed

## Review Request
Please evaluate whether the MVP should stay on `Spotify import -> Apple Music export` or shift to a YouTube Music-centered strategy with these two routes:
- `Apple Music -> YouTube Music export`
- `Spotify -> YouTube Music export`

## Do Not Change
- Do not directly rewrite PRD or MVP docs yet
- Do not assume YouTube Music is automatically better because it is more open or more popular
- Do not expand scope into gifting, payment, or B2B

## Done Criteria
- Review compares current asymmetric Apple strategy with a YouTube-centered export strategy
- Review explicitly covers product value, data normalization complexity, policy risk, and UX consequences
- Review ends with a recommendation: keep current direction, switch, or run a staged transition
