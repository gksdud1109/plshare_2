# Claude Prompt for Skill Curation v0.1

아래 프롬프트를 그대로 Claude에 붙여넣으면 된다.

```text
You are working as a `Reviewer / Operator` plus `Product Operations Librarian` for this project.

Your task is to curate a small set of genuinely useful reusable skills for this repository.

Important: do NOT produce only a registry file.
The previous failure mode was creating just `skills/README.md` with links.
This time you must create actual skill packages that can be reused by agents.

## Goal
Collect the highest-value skills for this project from:
- https://github.com/anthropics/skills
- and only if needed, a very small number of other high-quality official/public sources

Then adapt them into this repository's own skill system.

## Project context
This repository is building a product around:
- playlist as emotional asset
- player-agnostic playlist ledger
- cross-platform import/export
- future enterprise-ready human taste data

Current team roles are:
- Product Owner
- Product Designer
- Frontend Engineer
- Backend Engineer
- Reviewer / Operator

The docs already contain:
- PRD
- market research
- functional requirements
- non-functional requirements
- MVP priority
- agent roles
- workflow and prompt docs

## What you must do
1. Read the current docs enough to understand the project direction.
2. Select only 4 to 7 skills that are truly high leverage for this repo.
3. Prefer skills that help with:
   - PRD / strategy sharpening
   - UX spec writing
   - frontend implementation quality
   - webapp testing
   - backend/API integration
   - review / operator workflows
4. Create real skill folders under the top-level `/skills` directory.
5. Each selected skill must have its own folder and `SKILL.md`.
6. Keep `skills/README.md` as a registry only, pointing to the actual skill folders.

## Required output structure
Create this shape:

skills/
  README.md
  <skill-name>/
    SKILL.md
    OPTIONAL: templates/
    OPTIONAL: checklists/
    OPTIONAL: references.md

## Requirements for each SKILL.md
Each skill file must contain:
- skill purpose
- when to use
- when not to use
- required inputs
- exact workflow steps
- expected outputs
- role mapping
- handoff notes to the next role

## Very important constraints
- Do not dump all anthropics/skills entries.
- Do not create a link farm.
- Do not keep skills as one-line summaries only.
- Do not rely on `gh api` if GitHub auth is unavailable.
- If direct GitHub API access is blocked, use webpage fetch and manual extraction.
- Adapt the selected skills to this repository's role system and document structure.

## Selection rule
For each chosen skill, explicitly justify:
- why this skill matters for this project
- which role uses it
- what concrete repo artifact it improves

For each rejected candidate, do not create a folder. Just mention briefly in the registry if needed.

## Deliverables
At minimum, produce:
- `skills/README.md`
- 4 to 7 real skill folders with `SKILL.md`

## Suggested high-value candidates to evaluate first
- doc-coauthoring
- frontend-design
- webapp-testing
- claude-api
- mcp-builder
- brand-guidelines
- skill-creator

## Final output expectations
When finished, report:
- which skills were selected
- which files were created
- how the skills map to Product Owner / Product Designer / Frontend Engineer / Backend Engineer / Reviewer-Operator
```
