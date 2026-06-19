# Briefing: qa-gift-first-p0-e2e-001

## Objective

Validate the complete Product Baseline v2.1 P0 corridor after YouTube search and import entry are connected.

## Required Scenario

1. Establish a Google-backed application identity in demo mode.
2. Create an asset through YouTube search or import.
3. Attach Emotional Context.
4. Create a gift.
5. Open the gift as an unauthenticated recipient.
6. Start a YouTube embed.
7. Confirm the receiver is not forced to sign in before opening or playback.

## Deliverables

- `frontend/e2e/gift-first-p0.spec.mjs`
- `docs/10-operations/gift-first-p0-validation-report.md`

## Requirements

- Use stable selectors and explicit assertions.
- Cover failure behavior where practical.
- Do not treat fixture fallback as production success.
- Record commands, pass/fail results, and any remaining environment-only validation in the report.

## Validation

```bash
cd backend && ./gradlew test
cd ../frontend && npm run build && npm run lint
```
