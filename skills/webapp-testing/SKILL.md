---
name: webapp-testing
description: Run Playwright-based smoke and regression tests on local dev server for PA flows (Import, PA create, Export, Share). Use before any merge that touches UI or platform adapter code.
role: Frontend Engineer + Reviewer/Operator
source: adapted from https://github.com/anthropics/skills/tree/main/skills/webapp-testing
---

# Skill: Webapp Testing

## Purpose
PA 제품의 핵심 가치(플랫폼 이동, 자산 보존)는 플로우가 끝까지 완주되어야 증명된다. 단위 테스트만으로는 Import→Normalize→Log→Export 루프가 깨지는 회귀를 잡을 수 없다. 이 skill은 로컬 dev server 위에서 Playwright로 golden path와 주요 실패 경로를 반복 가능하게 검증한다.

## When to use
- Frontend PR 머지 직전.
- Platform adapter(Spotify/Apple Music) 관련 코드 변경 후.
- UX 회귀 버그 재현 시.
- Release 전 smoke test.

## When NOT to use
- 백엔드 단위 테스트(이건 Jest/Vitest 등으로).
- 스펙 자체가 불확실한 단계(먼저 PRD/UX 고정).
- 외부 실 API를 호출하는 계약 테스트 — 별도 integration test로 분리.

## Required inputs
1. 검증 대상 플로우 명 (예: "Spotify Import → PA Create → Apple Music Export").
2. 각 플로우의 성공 기준 (screen spec의 "성공 기준" 필드).
3. 로컬 dev server 기동 명령 (예: `pnpm dev`).
4. 테스트 계정/fixture 데이터 경로.

## Workflow (exact steps)

### Step 1 — Decision tree
1. 대상 화면이 **정적 HTML**이면: 파일을 직접 읽고 DOM 구조만 검증.
2. **동적 앱**이면: dev server 기동 후 Playwright로 진행.
3. 이미 서버가 떠 있으면 재기동하지 말 것. 포트 충돌 방지.

### Step 2 — Test 스크립트 작성
각 플로우당 한 스크립트. 파일 경로: `tests/e2e/<flow-name>.spec.ts`.

필수 단계:
1. `await page.goto(url)` 직후 **반드시** `await page.waitForLoadState('networkidle')`. 이 전에는 DOM inspect 금지.
2. selector는 렌더된 상태에서 추출. 소스 JSX만 보고 추측하지 말 것.
3. 액션마다 assertion 1개 이상.
4. 스크린샷을 `test-results/`에 저장.

### Step 3 — 핵심 플로우 커버리지
최소한 다음 golden path 3개는 항상 유지:
- **Import**: Spotify 플레이리스트 URL 입력 → 정규화 완료 → PA 생성 화면 진입.
- **Log**: PA에 텍스트 일기 + 감정 태그 추가 → 저장 후 리스트에 노출.
- **Export**: 저장된 PA를 Apple Music으로 내보내기 → 성공 상태 표시.

각 플로우에 empty·error 케이스 최소 1개씩 추가.

### Step 4 — 실행 및 보고
1. `pnpm test:e2e` 또는 상응 명령 실행.
2. 실패한 스텝의 스크린샷·콘솔 로그를 PR 코멘트에 첨부.
3. 2회 연속 flaky한 테스트는 격리(`test.skip` 금지, `test.fixme`로 표시)하고 `docs/implementation/open-issues.md`에 원인 조사 이슈로 등록.

### Step 5 — 리포트
- 통과: PR에 "webapp-testing: 3 golden + 2 error cases passed"라고 한 줄.
- 실패: 실패 케이스, 재현 스텝, 관련 커밋 SHA를 `docs/implementation/open-issues.md`에 등록.

## Critical rules
- `networkidle` 이전 DOM inspect 금지 (원본 anthropics/skills의 강조 규칙).
- Mock을 넣지 말고 실제 dev server를 띄운다.
- 테스트는 UI를 검증한다. 비즈니스 로직 검증은 유닛 테스트의 몫.

## Expected outputs
- `tests/e2e/*.spec.ts`
- `test-results/` 스크린샷
- PR 코멘트 한 줄 요약
- 필요 시 `docs/implementation/open-issues.md` 업데이트

## Role mapping
- **Primary**: Frontend Engineer (구현 직후 smoke test).
- **Co-primary**: Reviewer/Operator (release 전 전체 스위트).

## Handoff to next role
- 통과 시 → Reviewer가 머지 승인.
- 실패 시 → Frontend Engineer에게 재할당. 3회 이상 같은 플로우가 실패하면 Product Designer까지 호출해 스펙 정합성 재점검.
