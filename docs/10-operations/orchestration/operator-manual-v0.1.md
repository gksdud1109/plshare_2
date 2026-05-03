# Operator Manual v0.1

## 목적
이 문서는 현재 프로젝트에서 SQLite queue 기반 협업 시스템을 실제로 어떻게 운영하는지 설명한다.

---

## 1. 구조 요약
- queue source of truth: `.orchestrator/state/queue.db`
- schema: `.orchestrator/sql/schema.sql`
- workers:
  - `gemini-cli`
  - `claude-code`
  - `codex-cli`
- review lane:
  - `.orchestrator/bin/review-loop.sh`
- branch/PR governance:
  - `docs/10-operations/orchestration/branch-pr-governance-v0.1.md`

---

## 2. 초기 설정

### 가장 쉬운 실행
권장 방식은 `iTerm2`에서 아래 런처를 프로필 또는 핫키에 연결하는 것이다.

`/Users/hanyoung-jeong/Development/plshare_2/.orchestrator/bin/open-iterm-workbench.sh`

이 런처는 아래를 자동으로 처리한다.
- DB 없으면 초기화
- task 없으면 seed
- 현재 gate task 2개 승인
- 기존 background lane 정지
- iTerm2에서 Control / Worker 창 열기

### iTerm2에 한 번만 연결하는 방법
1. `iTerm2 > Settings > Profiles`로 들어간다
2. 새 프로필을 하나 만든다. 예: `Orchestrator`
3. `Command`를 `Send text at start` 또는 `Run command`로 설정한다
4. 아래 명령을 넣는다

```bash
bash /Users/hanyoung-jeong/Development/plshare_2/.orchestrator/bin/open-iterm-workbench.sh
```

5. 원하면 Hotkey Window나 단축키를 연결한다

이렇게 하면 이후부터는 bash를 직접 치지 않고 iTerm2 프로필 실행만 하면 된다.

### DB 초기화
```bash
bash .orchestrator/bin/init-db.sh
```

### 현재 프로젝트 task 적재
```bash
bash .orchestrator/bin/seed-current-project.sh
```

### 상태 보기
```bash
bash .orchestrator/bin/status.sh
```

### 현재 우선 확인
- 전략 전환 검토 task가 있는지 확인한다
- branch/PR consolidation task가 끝났는지 확인한다
- 위 두 gate가 열리기 전에는 FE/BE 구현 task를 승인하지 않는다

---

## 3. 승인 흐름

### 승인 대기 목록 보기
```bash
bash .orchestrator/bin/propose-approvals.sh
```

### 전체 승인
```bash
bash .orchestrator/bin/approve.sh all
```

### 특정 task만 승인
```bash
bash .orchestrator/bin/approve.sh <task-id>
```

---

## 4. worker 실행

각 worker는 별도 창이나 tmux pane에서 실행한다.

현재 권장 방식은 `tmux`가 아니라 `iTerm2 pane`이다.
또한 worker 기본값은 `headless loop`가 아니라 `visible interactive loop`다.

### Gemini worker
```bash
bash .orchestrator/bin/visible-worker-loop.sh gemini-cli
```

### Claude worker
```bash
bash .orchestrator/bin/visible-worker-loop.sh claude-code
```

### Codex worker
```bash
bash .orchestrator/bin/visible-worker-loop.sh codex-cli
```

### Review lane
```bash
bash .orchestrator/bin/review-loop.sh
```

### 한 번에 백그라운드 시작
```bash
bash .orchestrator/bin/start-lanes.sh
```

### 한 번에 종료
```bash
bash .orchestrator/bin/stop-lanes.sh
```

### iTerm2 작업대 열기
```bash
bash .orchestrator/bin/open-iterm-workbench.sh
```

### 현재 실행 중인 로그 바로 보기
```bash
bash .orchestrator/bin/follow-running-logs.sh
```

### 현재 실행 중인 산출물 바로 보기
```bash
bash .orchestrator/bin/follow-running-reports.sh
```

### headless worker가 필요할 때만
```bash
bash .orchestrator/bin/worker-loop.sh gemini-cli
bash .orchestrator/bin/worker-loop.sh claude-code
bash .orchestrator/bin/worker-loop.sh codex-cli
```

---

## 5. 추천 모니터 배치

### 모니터 1: Control Plane
- Codex 앱
- iTerm2 Window 1
  - 상단: dashboard
  - 하단: approval / reports / logs 요약

### 모니터 2: Worker Plane
- iTerm2 Window 2
  - 좌상: gemini worker
  - 우상: claude worker
  - 좌하: codex worker
  - 우하: review lane
  - 추가 pane: running log follower

---

## 6. 운영 루틴

### 시작 시
1. DB 초기화 또는 상태 확인
2. 승인 대기 task 검토
3. 보이는 작업이 필요하면 iTerm2 프로필 또는 `open-iterm-workbench.sh` 실행
4. 백그라운드 실행이 필요할 때만 `start-lanes.sh` 사용

### 작업 중
1. `status.sh`로 상태 확인
2. `events.sh`로 최근 이벤트 확인
3. 전략 검토와 branch/PR 정리 task가 우선 완료되도록 유지
4. failed task만 재판단

### 종료 시
1. done / failed / review 잔여 상태 확인
2. 필요한 경우 decision log 수동 기록

---

## 7. decision log 기록

자동 queue는 `task_events`를 남긴다.
제품/설계 차원의 확정 결정은 별도로 decision log에 남긴다.

명령:
```bash
bash .orchestrator/bin/record-decision.sh "Product Owner" "Gemini" "Freeze validation scope" "Keep validation aligned to frozen MVP" "docs/20-product/research/validation-plan-v0.1.md"
```

---

## 8. 현재 프로젝트 기준 첫 실행 순서

1. `bash .orchestrator/bin/init-db.sh`
2. `bash .orchestrator/bin/seed-current-project.sh`
3. `bash .orchestrator/bin/propose-approvals.sh`
4. `po-platform-direction-review-001`, `ops-branch-pr-consolidation-001`부터 승인
5. iTerm2 프로필 또는 `bash .orchestrator/bin/open-iterm-workbench.sh`
6. 전략 검토와 branch/PR 정리 결과를 검토
7. 이후 `pd-ux-flow-001`, `be-adapter-arch-001`를 승인

---

## 9. 주의 사항
- queue source of truth는 YAML이 아니라 SQLite다
- 기존 `.claude/scripts` 기반 queue는 legacy로 본다
- 같은 path를 두 task가 동시에 수정하지 않게 lock을 유지한다
- failed task는 무조건 자동 재시도하지 않는다
- dependency가 풀린 blocked task는 review lane이 자동으로 다음 상태로 해제한다
- branch/PR 정리가 끝나기 전에는 새 task 브랜치를 열지 않는다
- iTerm2 작업대를 쓰는 동안에는 background lane을 중복 실행하지 않는다
