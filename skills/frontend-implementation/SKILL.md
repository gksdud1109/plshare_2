---
name: frontend-implementation
description: Build production-grade frontend components for the PA product from locked UX specs. Enforces aesthetic direction, full state coverage, and no generic AI-slop defaults.
role: Frontend Engineer
source: adapted from https://github.com/anthropics/skills/tree/main/skills/frontend-design
---

# Skill: Frontend Implementation

## Purpose
PA 제품의 UI는 "감성 자산" 정체성을 시각적으로 표현해야 한다. 일반 SaaS 템플릿에 수렴하면 경쟁 우위(휴먼 컨텍스트)가 시각적으로 증발한다. 이 skill은 UX 스펙을 실제 코드로 옮길 때 aesthetic·상태·접근성·성능을 고정한다.

## When to use
- `docs/ux/`의 스펙이 확정된 상태에서 컴포넌트/페이지를 신규 구현.
- 기존 UI의 빈/에러/로딩 상태 보강.
- UX 회귀(regression)가 보고된 이슈의 수정.

## When NOT to use
- 스펙이 없는 화면을 추측해서 만들 때 → 먼저 `ux-spec-authoring`.
- 백엔드 API 스키마 변경 → `claude-api-integration` 또는 backend decision log.
- 디자인 방향 자체를 바꾸고 싶을 때 → `ux-spec-authoring`의 Step 1로 되돌아가기.

## Required inputs
1. `docs/ux/design-direction-v*.md`
2. 대상 화면의 `screen-specs` 섹션
3. `copy-guide-v*.md`
4. API 계약 (`docs/implementation/open-issues.md` 또는 backend decision log)
5. 대상 브랜치 이름 (예: `feat/playlist-import-flow`)

## Workflow (exact steps)

### Step 1 — Aesthetic commitment 확인
1. `design-direction` 문서를 읽고 현재 브랜치의 컴포넌트가 그 방향을 따르는지 확인.
2. 충돌 발견 시 구현을 멈추고 `docs/implementation/ui-decision-log.md`에 충돌 내용을 기록, Designer에게 돌려보낸다.

### Step 2 — Component tree 설계
1. screen spec의 "주 정보 블록과 우선순위"를 그대로 컴포넌트 트리로 옮긴다.
2. 컴포넌트 단위는 **상태를 공유하는 최소 블록**. 5개 이상의 prop이 필요하면 분해.
3. 공통 컴포넌트는 `components/ui/`에, 화면 전용은 `app/<route>/_components/`에.

### Step 3 — 상태 5종 모두 구현
`default`, `empty`, `loading`, `error`, `success` — 하나라도 빠지면 PR 불가.
- `loading`: skeleton 또는 progressive reveal. 스피너 남발 금지.
- `empty`: copy-guide의 PA-tone 카피를 사용. 일반 "No data" 문구 금지.
- `error`: 사용자가 다음 행동을 취할 수 있는 액션을 반드시 포함.

### Step 4 — Aesthetic 가드레일
다음은 모두 금지:
- Inter / Roboto / 기본 system font를 본문에 사용
- 보라색 그라데이션을 흰 배경에 기본값으로 사용
- 대칭·정렬만 있는 밋밋한 grid 레이아웃
- 의미 없는 micro-interaction 남발

대신 다음을 우선:
- Display font와 body font 분리, 최소 한쪽은 개성 있는 선택
- 한 번의 인상적인 진입 애니메이션(staggered reveal) > 흩뿌린 micro-interaction
- 배경에 noise·gradient mesh·geometric texture 등 질감 한 겹

### Step 5 — 접근성·성능 체크
- 모든 상호작용 요소에 focus style 명시.
- 이미지에 의미 있는 alt 또는 `role="presentation"`.
- LCP 요소에 `priority` 또는 eager load.
- Contrast ratio AA 이상.

### Step 6 — 검증
1. 로컬에서 dev server 띄우고 브라우저에서 golden path + 각 상태를 직접 확인.
2. `webapp-testing` skill을 호출해 Playwright로 최소 smoke test.
3. `docs/implementation/ui-decision-log.md`에 이번 PR의 주요 선택(font, layout break, animation)을 1~3줄로 기록.

## Expected outputs
- 새/수정된 컴포넌트 코드
- `docs/implementation/ui-decision-log.md` 업데이트
- PR 설명에 screen spec 경로, 상태 5종 스크린샷, 성능/접근성 체크 결과

## Role mapping
- **Primary**: Frontend Engineer (Codex CLI).
- **Design support**: Claude가 aesthetic 충돌 검토.
- **QA**: Reviewer/Operator가 webapp-testing으로 회귀 검증.

## Handoff to next role
- → **Reviewer/Operator**: PR 리뷰 + `webapp-testing` skill.
- → **Backend Engineer**: API 계약 불일치 발견 시 `docs/implementation/open-issues.md`에 이슈로 등록.
- 금지: 스펙에 없는 화면/상호작용을 "더 좋아 보여서" 추가하는 것. 추가하려면 `ux-spec-authoring`으로 돌아간다.
