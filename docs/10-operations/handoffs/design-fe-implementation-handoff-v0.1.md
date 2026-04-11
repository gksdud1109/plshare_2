# Handoff: Frontend Implementation for Spotify-to-Apple Bridge v0.1

## Task
Implement the core user interface and interaction flows for the `Spotify Import -> Assetize -> Apple Music Export` loop based on the locked PRD and UX specifications.

## Receiver Role
Frontend Engineer

## Recommended Model
Codex App

## Input
- `docs/20-product/strategy/prd-v0.2.md`
- `docs/20-product/design/ux/design-direction-v0.1.md`
- `docs/20-product/design/ux/user-flows-v0.1.md`
- `docs/20-product/design/ux/screen-specs-v0.1.md`
- `docs/20-product/design/ux/copy-guide-v0.1.md`
- `docs/20-product/delivery/implementation/open-issues.md` (for API contracts)

## Output
- Working frontend components and state management logic.
- Storybook or equivalent UI component library (if applicable).

## Current Fixed Decisions
- Aesthetic: **Luxury / Refined**. Use card-based layouts and elegant typography.
- Flow: Follow the defined happy paths and error states in `user-flows-v0.1.md`.
- Copy: Adhere strictly to the `copy-guide-v0.1.md`.
- Connectivity: Use external deep links for actual music playback.

## Do Not Change
- Do not simplify the 'Asset Card' to a plain list.
- Do not implement music player controls.
- Do not change the refined/poetic tone of the copy.

## Done When
- All screens from `screen-specs-v0.1.md` are implemented with all states (default/loading/error/success).
- The transition between Import, Assetize, and Export feels "weighted and smooth" as per design direction.
- Responsive web view for public sharing is functional.

## Next Role
Reviewer / QA

## Next Expected Handoff
- Feature testing and validation handoff.
