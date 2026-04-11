# Prompt for Document Restructure Execution v0.1

아래 프롬프트를 그대로 Claude 또는 Codex에 붙여넣으면 된다.

```text
You are acting as a `Reviewer / Operator` and `Documentation Maintainer`.

Your task is to execute the document restructure proposal in this repository.

Primary reference:
- `docs/10-operations/document-restructure-proposal-v0.1.md`

You must first read and understand:
- `docs/README.md`
- `docs/00-foundation/document-architecture.md`
- `docs/10-operations/document-restructure-proposal-v0.1.md`
- `docs/10-operations/agent-operating-model.md`

## Goal
Reorganize the `docs/` directory so that:
- operations / workflow documents are separated from product documents
- file locations reflect actual product development flow
- latest/current documents remain easy to find
- archive rules stay intact
- internal links keep working after migration

## Target structure
Implement the structure proposed in:
- `docs/10-operations/document-restructure-proposal-v0.1.md`

That means introducing at minimum:
- `docs/00-foundation/`
- `docs/10-operations/`
- `docs/20-product/`

And under `20-product/`:
- `strategy/`
- `research/`
- `requirements/`
- `design/`
- `data/`
- `delivery/`

## What you must do
1. Create the new folder structure and README files where needed.
2. Move existing files according to the proposal mapping.
3. Preserve file contents unless a path reference must be updated.
4. Update all internal markdown links and plain-text path references that break because of the move.
5. Preserve version history conventions such as `archive/`.
6. Keep only one current location for each active document.
7. Leave a clear navigation path from the top-level `docs/README.md`.
8. If there are ambiguous files, place them conservatively and note them in a migration summary.

## Important constraints
- Do not rewrite product decisions.
- Do not change the meaning of PRD, research, or requirements docs.
- Do not silently drop files.
- Do not delete archives.
- Do not invent missing documents unless a README/navigation file is necessary.
- Do not create duplicate "current" copies of the same document.

## Expected outputs
At minimum, produce:
- migrated folder structure
- updated README/navigation files
- updated path references
- a migration summary document

Create this migration summary:
- `docs/10-operations/migration-log-v0.1.md`

The migration summary must include:
- files moved
- files whose references were updated
- any unresolved ambiguities
- any follow-up cleanup recommendations

## Migration quality bar
The final structure should make it obvious where to look for:
- team operating rules
- prompts and agent docs
- product strategy
- market research
- requirements
- UX/design docs
- data/technical design
- implementation/delivery notes

## Suggested execution order
1. Create target folders and README files
2. Move foundation docs
3. Move operations docs
4. Move product docs
5. Update references
6. Verify link/path consistency
7. Write migration summary

## Final reporting
When complete, report:
- what moved
- what stayed
- what was updated
- what still needs manual review
```
