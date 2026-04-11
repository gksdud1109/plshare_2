# Gemini Prompt for PO Strategy Consistency Pass v0.1

아래 프롬프트를 그대로 Gemini에 붙여넣으면 된다.

```text
You are working as the `Product Owner` for this project.

Your task is to perform a strategy consistency pass across three existing strategy documents.

Documents to review and align:
- `docs/20-product/strategy/prd-v0.2.md`
- `docs/20-product/strategy/product-strategy-v0.1.md`
- `docs/20-product/strategy/mvp-feature-priority-v0.1.md`

Your job is NOT to expand the product.
Your job is to remove contradictions and freeze a consistent MVP direction.

## Why this task exists
The current strategy docs are much sharper than before, but they still contain tensions that could cause different team members to build different products.

In particular, you must resolve inconsistencies around:
1. whether B2B / enterprise demand is a current product concern or only a future expansion
2. whether human-authenticity / proof-of-human is core MVP scope or post-launch strengthening
3. whether MVP launch requires full Spotify + Apple Music import/export, or a narrower asymmetric v1 release

## Required output
Produce a single decision memo:
- Output target: `docs/20-product/strategy/archive/strategy-consistency-pass-v0.1.md`

## What the memo must do
1. Identify the exact conflicting statements across the three docs.
2. Decide which statement becomes the source of truth.
3. Explain the decision briefly.
4. List exact edits that should be applied to each of the three docs.
5. Define a single frozen MVP release scope.
6. Define what is explicitly not part of MVP.

## Important operating rules
1. Be opinionated. Do not leave contradictions unresolved.
2. Prefer narrowing scope over broadening scope.
3. Distinguish clearly between:
   - source of truth for current MVP
   - near-term post-launch additions
   - future strategic options
4. Write in Korean.
5. Keep it practical and actionable.

## Specific questions you must answer

### A. B2B Positioning
Should B2B remain:
- a core problem statement now
- a design constraint only
- or a future expansion that should mostly disappear from MVP-facing docs?

### B. Human Authenticity
Should human-authenticity remain:
- a flagship core feature in PRD
- a basic optional proof signal in early product
- or a later differentiator after MVP loop validation?

### C. Release Scope
For actual v1 launch, which of these should become the frozen release definition?
- Option A: Spotify import + Apple Music export
- Option B: Spotify/Apple Music import/export all supported
- Option C: some other narrower cut

You must pick one and justify it.

## The memo structure must include
- Executive decision summary
- Conflict 1 / decision / required doc edits
- Conflict 2 / decision / required doc edits
- Conflict 3 / decision / required doc edits
- Frozen MVP release definition
- Explicit non-MVP list
- Next step recommendation

## Final instruction
Return only the completed content for:
- `docs/20-product/strategy/archive/strategy-consistency-pass-v0.1.md`
```
