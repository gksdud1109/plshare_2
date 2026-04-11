---
name: ux-spec-authoring
description: Author UX flows, screen specs, empty/error/loading states, and copy under docs/ux/ from a locked PRD. Commits to a bold aesthetic direction before specifying screens.
role: Product Designer
source: adapted from https://github.com/anthropics/skills/tree/main/skills/frontend-design and doc-coauthoring
---

# Skill: UX Spec Authoring

## Purpose
`docs/ux/`는 현재 README만 있고 비어 있다. 이 skill은 PRD에서 확정된 MVP 범위를 받아, Frontend Engineer가 바로 구현할 수 있는 수준의 UX 산출물을 생산한다. 특히 PA 제품은 "감성 자산"이라는 정체성을 가지므로, 일반적인 SaaS 템플릿 UI로 수렴하지 않도록 **aesthetic direction을 먼저 고정**한다.

## When to use
- PRD가 Stage 3(reader test)를 통과한 직후.
- `docs/ux/user-flows-v*.md`, `screen-specs-v*.md`, `copy-guide-v*.md`를 신규 작성하거나 개정할 때.
- 기존 화면에 빈/에러/로딩 상태가 빠져 있어 구현이 막힐 때.

## When NOT to use
- PRD가 아직 정리되지 않은 상태 → 먼저 `prd-sharpening`.
- 실제 코드 구현 → `frontend-implementation`.
- 브랜드 시각 아이덴티티(로고/컬러 시스템)의 최초 결정 — 이건 별도 세션에서 PO와 합의 필요.

## Required inputs
1. 최신 PRD 경로와 버전 (예: `docs/prd/prd-v0.2.md`).
2. 이번에 다룰 플로우 범위 (예: "Spotify 플레이리스트 Import → PA 생성 → Apple Music Export").
3. 이미 결정된 디자인 제약 (타겟 디바이스, 다국어 여부 등).
4. Out-of-scope 화면 명시.

## Workflow (exact steps)

### Step 1 — Aesthetic direction lock
다음 축 중 하나를 골라 한 문장으로 commit한다. 중간값 금지.
- editorial / magazine-like (긴 글·일기와 어울림)
- brutalist / raw (인간성·물성 강조)
- luxury / refined (취향 자산의 가치 강조)
- organic / natural (감정 레이어)

선택한 방향을 `docs/ux/design-direction-v0.1.md` 파일 상단에 1문단으로 기록. 이후 모든 screen spec은 이 방향과 충돌하면 reject.

### Step 2 — User flow 작성
1. PRD의 "Core User Scenarios"를 받아 각 scenario를 **entry → happy path → empty → error → success**로 분해.
2. `docs/ux/user-flows-v0.1.md`에 scenario별 섹션으로 기록. 각 step에 `[시스템 입력]`, `[사용자 액션]`, `[시스템 피드백]` 3줄을 명시.
3. 매 step마다 "이 단계에서 플랫폼 종속성이 복원되는가?"를 체크. 복원되면 flow를 다시 설계.

### Step 3 — Screen spec 작성
화면별로 다음 필드를 모두 채운다. 하나라도 빠지면 구현 불가로 간주.
- 화면 목적 (1문장)
- 주 정보 블록과 우선순위
- 상호작용 (탭/스와이프/키보드)
- 상태: `default`, `empty`, `loading`, `error`, `success`
- 접근성 고려 (대체 텍스트, 초점 순서)
- 성공 기준 (이 화면이 "끝났다"는 조건)

출력: `docs/ux/screen-specs-v0.1.md` — 스크린마다 한 섹션.

### Step 4 — Copy guide
1. 카피는 aesthetic direction에 맞춰 tone·문장 길이·금지어를 정의.
2. Empty/error 카피는 일반 SaaS 문구(`Oops! Something went wrong.` 등) 금지. PA 제품 정체성에 맞는 표현만 허용.
3. `docs/ux/copy-guide-v0.1.md`에 표로 정리: `context | tone | example | avoid`.

### Step 5 — Handoff package 검증
다음 질문에 모두 Yes여야 한다.
- Frontend Engineer가 이 문서만 읽고 컴포넌트 트리를 그릴 수 있는가?
- 모든 상태(default/empty/loading/error/success)가 명시됐는가?
- PRD의 MVP 범위를 넘어선 화면이 끼어 있지 않은가?

## Expected outputs
- `docs/ux/design-direction-v0.1.md`
- `docs/ux/user-flows-v0.1.md`
- `docs/ux/screen-specs-v0.1.md`
- `docs/ux/copy-guide-v0.1.md`

## Role mapping
- **Primary**: Product Designer (Claude).
- **Backup**: PO가 tone/message 검증.

## Handoff to next role
- → **Frontend Engineer**: `frontend-implementation` skill 실행. 입력은 위 4개 파일 전부 + PRD 경로.
- → **Backend Engineer**: 각 screen에서 필요한 데이터 계약을 `docs/implementation/open-issues.md`에 "UX→API required fields" 섹션으로 추출.
- 금지: 구현 중 임의 카피 생성. 카피 변경이 필요하면 `copy-guide`부터 갱신.
