---
name: prd-sharpening
description: Sharpen PRDs, functional/non-functional requirements, and strategy docs through a 3-stage co-authoring loop (context → refinement → reader test). Trigger when revising anything under docs/prd/ or docs/research/.
role: Product Owner
source: adapted from https://github.com/anthropics/skills/tree/main/skills/doc-coauthoring
---

# Skill: PRD Sharpening

## Purpose
PA(Playlist Asset) 제품은 "플레이리스트를 플랫폼 비종속 자산으로 만든다"는 고유 서사를 가진다. 이 서사가 흐려지면 MVP 범위·기능 요구사항·리서치 인사이트가 서로 어긋난다. 이 skill은 `docs/prd/*` 문서를 독자가 오해 없이 읽을 수 있는 수준까지 끌어올리기 위한 3단계 loop를 고정한다.

## When to use
- `docs/prd/prd-v*.md`, `functional-requirements-v*.md`, `non-functional-requirements-v*.md`, `mvp-feature-priority-v*.md`, `product-strategy-v*.md`를 새 버전으로 올릴 때.
- `docs/research/market-research-v*.md`의 insight를 PRD에 반영해야 할 때.
- 엔지니어가 구현을 시작하기 전에 PRD가 "읽고 바로 이슈로 쪼갤 수 있는" 수준인지 검증해야 할 때.

## When NOT to use
- UX 플로우/화면 스펙 작성 → `ux-spec-authoring` 사용.
- 코드 구현/리팩터 → `frontend-implementation` 또는 `claude-api-integration` 사용.
- 초기 아이디어 브레인스토밍 단계(아직 기준 문서 자체가 없는 경우) — 먼저 `docs/life-logging-ledger-plan.md`부터 고정.

## Required inputs
1. 수정 대상 문서 경로 (예: `docs/prd/prd-v0.2.md`).
2. 관련 리서치/근거: `docs/research/market-research-v*.md`, `docs/prd/playlist-first-product-intent.md`.
3. 이번 개정의 목적 한 문장 (예: "YouTube `videoId` 정규화 미결 항목 해소").
4. 결정하면 안 되는 범위 (handoff 규칙 준수).

## Workflow (exact steps)

### Stage 1 — Context Gathering
1. 수정 대상 문서와 직접 링크된 문서(prd-v0.2의 Related Documents 전체)를 읽는다.
2. 다음 5개 meta 질문에 답을 **문서 안에서** 찾아 기록한다:
   - 이 문서의 1차 독자는 누구인가? (PO/엔지니어/투자자/B2B 파트너)
   - 이 문서가 성공적으로 읽혔을 때 독자가 다음에 취할 행동은?
   - 이 문서에서 "결정된 것"과 "미결된 것"은 각각 무엇인가?
   - 플랫폼 독립성·휴먼 컨텍스트·데이터 이식성 3원칙 중 이번 개정이 손대는 것은?
   - 이번 개정으로 어떤 기존 섹션이 무효화되는가?
3. 답이 비는 항목이 있으면 사용자에게 1회만 일괄 질문. 다 채워지기 전에는 Stage 2로 넘어가지 않는다.

### Stage 2 — Refinement & Structure
1. 가장 불확실한 섹션부터 시작한다. 보통 `6. MVP 범위`, `12. Risks and Open Questions`, `10. Data Structure` 중 하나.
2. 섹션별로: 질문 → 선택지 브레인스토밍 → 사용자 선택 → 갭 점검 → 초안 → 수술적 편집. **섹션 전체를 다시 쓰지 말고 `Edit` 도구로 부분만 교체**.
3. 수정이 끝날 때마다 해당 섹션이 PRD의 3원칙(Player-Agnostic / Human-Authenticity / Data Portability)과 충돌하지 않는지 확인한다.
4. 모든 섹션을 마친 뒤 문서 전체를 한 번 통독하며 흐름·일관성·불필요한 필러를 제거한다.

### Stage 3 — Reader Test
1. "Reader test" 질문 6개를 구성한다:
   - 이 제품이 Spotify/Apple Music과 경쟁하는가, 보완하는가?
   - MVP에서 YouTube는 포함되는가?
   - B2B 대상 데이터는 음원인가, 구조화된 선호 벡터인가?
   - 휴먼 컨텍스트는 어떻게 증명되는가?
   - Normalization 실패 시 fallback은?
   - 이번 개정으로 바뀐 것 3가지는?
2. 문서만 보고 6개 질문에 모두 답할 수 있으면 통과. 하나라도 모호하면 Stage 2로 돌아간다.
3. 통과 시 `docs/implementation/decision-log.md`에 "prd-vX.Y: <한 줄 요약>"을 1줄 추가한다.

## Expected outputs
- 갱신된 `docs/prd/*.md` 파일 (최소 diff).
- `docs/implementation/decision-log.md`에 1줄 로그.
- 필요 시 `docs/prd/archive/`로 직전 버전을 이동.

## Role mapping
- **Primary**: Product Owner (Gemini).
- **Reviewer**: Reviewer/Operator가 reader test 6문항을 대신 돌려 독립 검증.

## Handoff to next role
PRD가 Stage 3 통과했을 때:
- → **Product Designer**: `ux-spec-authoring` skill 실행. 입력은 갱신된 PRD 경로 + MVP 범위 섹션.
- → **Backend Engineer**: 새로 확정된 Data Structure·Functional Requirements만 전달. 리서치 근거는 링크로만.
- 금지: Frontend/Backend가 PRD를 자체 수정하는 것. 모든 PRD 수정은 다시 이 skill로 돌아와야 한다.
