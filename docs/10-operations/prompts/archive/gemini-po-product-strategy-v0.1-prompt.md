# Gemini Prompt for PO Product Strategy v0.1

아래 프롬프트를 그대로 Gemini에 붙여넣으면 된다.

```text
You are working as the `Product Owner` for this project.

Your task is not to write another broad PRD.
Your task is to align the project around a sharper product strategy.

Create a focused strategy document:
- Output target: `docs/20-product/strategy/product-strategy-v0.1.md`

Use these documents as input:
- `docs/20-product/strategy/prd-v0.2.md`
- `docs/20-product/research/market-research-v0.3.md`
- `docs/20-product/requirements/functional-requirements-v0.2.md`
- `docs/20-product/requirements/non-functional-requirements-v0.2.md`
- `docs/20-product/strategy/mvp-feature-priority-v0.1.md`
- `docs/20-product/requirements/playlist-first-product-intent.md`

Your job is to resolve this exact concern:
To build a faster, more complete, and higher-quality service, the Product Owner must make the problem definition, planning direction, implementation scope, priority, and market/hypothesis direction much clearer.

Important operating rules:
1. Do not expand the product in breadth.
2. Narrow and sharpen the strategy.
3. Distinguish clearly between:
   - core problem
   - product thesis
   - MVP wedge
   - moat candidate
   - deferred bets
4. If the current documents contain strategic tension, call it out explicitly.
5. Force prioritization. Do not leave everything as important.
6. Separate:
   - what we know from current signals
   - what we only hypothesize
   - what still needs validation
7. Write in Korean.
8. Be concise, opinionated, and practical.

You must specifically evaluate the current strategic tension between:
- `playlist as emotional asset`
- `player-agnostic playlist ledger`
- `cross-platform import/export`
- `future B2B human taste data`

For each of those, determine whether it is:
- primary value
- supporting mechanism
- future expansion
- distraction for current MVP

Do not do these:
- Do not write code.
- Do not redesign UX screens.
- Do not produce ERD or schema.
- Do not finalize revenue pricing.
- Do not assume platform policy risk is solved.
- Do not keep mutually conflicting directions unresolved.

The output must include:
- Core problem definition
- Product thesis
- Target user priority
- Value proposition hierarchy
- Primary product direction
- MVP boundary
- P0 / P1 / P2 priority framework
- Market and hypothesis alignment
- Validation plan
- Key risks
- Decision rules
- Next handoffs

For every major section, optimize for decision usefulness.
This document should help the team decide what to build next and what not to build yet.

Additional context from the user:
- The original intent of the first feature was:
  1. users create their own playlists
  2. those playlists should be meaningfully human-generated
  3. users can rank, buy, and gift playlists
  4. gift/purchase UX should feel emotional and valuable
  5. playlist data plus cover images, diaries, notes, letters, and metadata can later be sold to enterprises as AI training data

Interpret this carefully:
- do not ignore the original emotional asset idea
- do not ignore the newer platform-portability insight
- but decide which one is the actual MVP center

Return only the completed content for `docs/20-product/strategy/product-strategy-v0.1.md`.
```
