# SQLite Queue Design v0.1

## 목적
기존 YAML queue는 동시 수정과 상태 전이에 취약했다.

이 문서는 현재 프로젝트의 오케스트레이션 source of truth를 `SQLite queue`로 전환하는 설계를 정의한다.

---

## 1. 설계 원칙
- queue는 deterministic 해야 한다
- worker는 queue에서 자기 작업만 가져간다
- 문서와 코드는 worker가 만들고, queue는 상태만 관리한다
- artifact와 log는 분리한다
- file lock은 task 단위가 아니라 `path scope` 단위로 잡는다

---

## 2. 핵심 상태값
- `draft`
- `awaiting_approval`
- `pending`
- `blocked`
- `running`
- `review`
- `done`
- `failed`

---

## 3. 핵심 테이블

### tasks
태스크 메타데이터와 현재 상태를 저장한다.

핵심 필드:
- `id`
- `title`
- `role`
- `agent`
- `status`
- `priority`
- `requires_approval`
- `review_lane`
- `briefing_path`
- `report_path`
- `log_path`
- `validate_cmd`
- `next_role`
- `attempt_count`

### task_dependencies
선행 작업 관계를 저장한다.

### task_inputs
입력 문서/경로를 저장한다.

### task_outputs
worker가 실제로 바꿔야 하는 산출물 경로를 저장한다.

### task_locks
동시에 수정하면 안 되는 path scope를 저장한다.

### task_done_criteria
완료 기준을 저장한다.

### task_events
자동화 시스템이 남기는 실행 이벤트 로그다.

---

## 4. claim 규칙
worker는 아래 조건을 만족하는 task만 claim한다.
- 자기 agent와 일치
- 상태가 `pending`
- 모든 dependency가 `done`
- 현재 `running` task와 lock path가 겹치지 않음

claim은 SQLite transaction으로 수행한다.

---

## 5. review lane 규칙
review lane은 `review` 상태 task를 읽고 아래를 검사한다.
- report가 존재하고 비어 있지 않은가
- log에 명백한 error signal이 없는가
- managed output이 실제로 변경되었는가
- 필요하면 validate_cmd가 통과하는가

통과 시:
- `done`

실패 시:
- `failed`

---

## 6. 현재 프로젝트 기준 lane 매핑
- `gemini-cli` -> Product Owner lane
- `claude-code` -> Product Designer lane
- `codex-cli` -> Frontend / Backend lane
- `review-loop` -> deterministic review lane

---

## 7. 왜 YAML보다 나은가
- 동시 접근 안전성
- dependency 해제 자동화
- status transition 안정성
- 이벤트 추적 가능
- lock scope 검사 가능

---

## 8. 현재 프로젝트의 actual task schema

### Gate A. 전략 재검토
- `po-platform-direction-review-001`
  - 목적: `Spotify -> Apple` 유지 여부와 `YouTube Music 중심 export` 전환안을 비교 검토
  - agent: `gemini-cli`
  - output: `docs/20-product/strategy/platform-direction-review-v0.1.md`

### Gate B. branch/PR 정리
- `ops-branch-pr-consolidation-001`
  - 목적: role 중심 브랜치를 PR로 정리하고 병합 후 `task branch`로 복귀하는 계획 수립
  - agent: `codex-cli`
  - output: `docs/20-product/delivery/implementation/branch-pr-consolidation-v0.1.md`

### Gate C. 후속 제품/설계 작업
- `po-validation-plan-001`
  - dependency: `po-platform-direction-review-001`
- `pd-ux-flow-001`
  - dependency: `po-platform-direction-review-001`, `ops-branch-pr-consolidation-001`
- `be-adapter-arch-001`
  - dependency: `po-platform-direction-review-001`, `ops-branch-pr-consolidation-001`
  - special rule: 문서 전용 task, 구현 금지
- `fe-implementation-plan-001`
  - dependency: `pd-ux-flow-001`, `be-adapter-arch-001`, `ops-branch-pr-consolidation-001`

### Gate D. human approval
- `be-adapter-arch-001` 완료 후 backend 구현은 별도 task로 다시 승인받아야 한다
- branch/PR 정리 완료 전에는 새 구현 브랜치를 열지 않는다
