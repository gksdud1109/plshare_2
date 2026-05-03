# Briefing

## Task ID
ops-branch-pr-consolidation-001

## Receiver Role
Reviewer / Operator

## Recommended Model
Codex CLI

## Input Docs
- `docs/10-operations/orchestration/operator-manual-v0.1.md`
- `docs/10-operations/orchestration/branch-pr-governance-v0.1.md`
- `docs/10-operations/agent-operating-model.md`

## Output Docs
- `docs/20-product/delivery/implementation/branch-pr-consolidation-v0.1.md`

## Fixed Decisions
- Existing role-oriented branches should be normalized through PRs first
- After merge, new work should return to task-scoped branches
- Orchestrator must gate new implementation work behind this cleanup

## Do Not Change
- Do not start new feature implementation from this task
- Do not merge code blindly without identifying ownership and conflicts
- Do not keep role-based long-lived branches as the steady-state model

## Done Criteria
- Document identifies current role-oriented work branches and expected PR grouping
- Document defines merge order, review owner, and post-merge branch naming rules
- Document explains how orchestrator should reopen task-based branches after merge
