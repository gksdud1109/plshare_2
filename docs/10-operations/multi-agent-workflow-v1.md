# Multi-Agent Workflow v1.0
## plshare2 — 최효율 AI 하네스 엔지니어링 설계

- Date: `2026-05-03`
- Author: `Orchestrator (Claude)`
- Status: `Active`

---

## 1. 설계 철학

### 1.1 핵심 원칙

| 원칙 | 설명 |
|:---|:---|
| **Human-in-the-loop Gate** | `requires_approval: true` 태스크만 사람 승인 이후 실행 |
| **Pull-based Dispatch** | Orchestrator가 push하지 않는다. Worker가 queue를 polling해서 pull한다 |
| **Atomic Claim** | `task_queue.rb claim-next-for-agent`가 파일 잠금 안에서 pending → in_progress를 원자 처리한다 |
| **File-based Handoff** | 에이전트 간 통신은 YAML/Markdown 파일. REST API나 IPC 없음 |
| **Artifact Ownership** | 각 태스크는 자신의 output 파일만 쓴다. locked_files 외 수정 금지 |
| **Idempotent Review** | review는 git diff로 실제 변경 여부를 검증. 사전 존재 파일로 false-positive 방지 |
| **Branch-per-Task** | 모든 구현 작업은 격리된 git branch에서 진행. main에 직접 커밋 금지 |

### 1.2 최신 AI 하네스 엔지니어링 트렌드 반영

이 설계는 2025-2026년 업계 관행을 반영한다:

- **Claude Code Hooks** (Anthropic, 2025): PreToolUse/PostToolUse/Stop 이벤트를 활용한 side-effect 자동화
- **Git Worktrees** (GitHub Engineering): 태스크별 독립 파일시스템 → 병렬 작업 중 충돌 없음
- **Structured Agent Output** (OpenAI Evals 방식): 각 에이전트 출력은 정해진 경로/형식으로만. free-form stdout 최소화
- **Approval-gate pattern** (Anthropic Safety): autonomous 루프에 반드시 human checkpoint 삽입
- **Queue-based orchestration** (Temporal/Celery 철학): 상태는 queue에 있고, worker는 stateless

---

## 2. 아키텍처 전체 그림

```
┌─────────────────────────────────────────────────────────────────┐
│                     HUMAN (Operator)                            │
│                                                                 │
│  claude desktop/code  ◄──── propse-decisions.sh (Stop hook)    │
│         │                                                       │
│    approve.sh ──────────────────────────────────────┐          │
└─────────────────────────────────────────────────────┼──────────┘
                                                       │ approve
                                                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                  task-queue.yaml  (Single Source of Truth)      │
│                                                                 │
│  awaiting_approval ──[approve]──► pending                       │
│  pending ──[worker picks up]──► in_progress                     │
│  in_progress ──[done]──► review                                 │
│  review ──[orchestrator]──► done / failed                       │
│  blocked ──[sync-queue unblock]──► awaiting_approval/pending    │
└─────────────────────────────────────────────────────────────────┘
         ▲                              │
         │ mark_review                  │ poll (8s interval)
         │                              ▼
┌────────┴─────┐              ┌─────────────────────────┐
│  worker.sh   │              │   run-orchestrator.sh   │
│  (Terminal)  │              │   (Claude Desktop/Code) │
│              │              │                         │
│  gemini-cli  │              │  sync → review → unblock│
│  codex-cli   │              │  → notify claude-code   │
│  claude-code │              │                         │
└──────────────┘              └─────────────────────────┘
         │                              │
         │ writes artifacts             │ review-task.sh
         ▼                              ▼
┌─────────────────┐          ┌──────────────────────────┐
│  docs/20-product│          │  git diff check          │
│  backend/src    │          │  done_criteria verify    │
│  frontend/src   │          │  auto-approve or flag    │
└─────────────────┘          └──────────────────────────┘
         │
         │ PostToolUse Write hook
         ▼
  .claude/logs/artifact-writes.log
```

---

## 3. 에이전트 역할 분담

### 3.1 에이전트 특성 비교

| 에이전트 | 모델 | 작업 방식 | 출력 방식 | 최적 태스크 유형 |
|:---|:---|:---|:---|:---|
| **gemini-cli** | Gemini 2.0 | stdout 생성 | 단일 Markdown 파일 | 전략 검토, 문서 초안, 리서치, PO/PM 작업 |
| **codex-cli** | GPT-4o | workspace 편집 | 다수 소스파일 직접 수정 | BE/FE 구현, 코드 리팩터링, 테스트 작성 |
| **claude-code** | Claude 3.5 Sonnet | workspace 편집 | 다수 문서/코드 파일 수정 | 다중 파일 문서화, 복잡한 전략 동기화, 리뷰 |

### 3.2 태스크 배정 기준

```
태스크 유형 결정 트리:

구현(코드 작성)이 필요한가?
  ├─ YES → 백엔드/인프라? → codex-cli
  │         프론트엔드? → codex-cli (또는 claude-code if complex)
  └─ NO  → 단일 파일 문서/전략? → gemini-cli
            다중 파일 동기화? → claude-code
            리뷰/승인? → claude-code (orchestrator)
```

---

## 4. 태스크 생명주기 (Branch-per-Task)

### 4.1 전체 흐름

```
1. 태스크 정의
   task-queue.yaml에 태스크 추가
   → status: blocked (의존성 있음) 또는 pending (바로 시작 가능)
   → 전략·비용·외부 공개 변경만 requires_approval: true 설정

2. 승인 단계  [HUMAN GATE]
   propose-decisions.sh → 승인 대기 태스크 목록 표시
   operator가 approve.sh <task-id> 실행
   → status: awaiting_approval → pending

3. Worker 픽업
   worker.sh <agent> polling → pending 태스크 발견
   → status: pending → in_progress
   → git checkout -b feat/<role>/<task-id>

4. 작업 실행
   briefing 파일 읽어서 CLI 실행
   → 실시간 출력 tee로 로그 기록
   → 결과물을 output_docs 경로에 저장

5. 리뷰 단계  [AUTO-REVIEW + HUMAN ESCALATION]
   → status: in_progress → review
   orchestrator run-orchestrator.sh 실행
   review-task.sh → git diff로 변경 검증
   → worker exit_code=0, artifact, validate_cmd, done_criteria 검토 후 status → done
   → 미충족 시: status → failed + 사유 기록

6. PR 생성
   create-pr.sh <task-id> <branch>
   → gh pr create with done_criteria checklist
   → PR review by operator

7. 머지 & 다음 태스크 언블록
   PR merge → main
   sync-queue.sh 실행
   → 의존성 해소된 태스크들: blocked → awaiting_approval
   → propose-decisions.sh가 다음 승인 대기 목록 표시
```

### 4.2 Branch 전략

```
main
 ├── feat/po/<task-id>          # Product Owner 태스크
 ├── feat/be/<task-id>          # Backend 태스크  
 ├── feat/fe/<task-id>          # Frontend 태스크
 ├── feat/pd/<task-id>          # Product Designer 태스크
 └── ops/<task-id>              # 운영/인프라 태스크

규칙:
- 각 태스크는 독립 브랜치에서 실행
- worker는 브랜치를 자동 생성하지 않는다. 병렬 구현은 사전에 worktree를 배정한다
- 완료 후 create-pr.sh로 PR 자동 생성
- merge 후 브랜치 삭제 (gh pr merge --delete-branch)
```

### 4.3 Git Worktree 활용 (병렬 작업 시)

codex-cli/claude-code가 동시에 다른 태스크를 처리할 때:

```zsh
# 태스크 시작 시 worker.sh 내부에서 실행
WORKTREE_PATH="/tmp/plshare2-worktrees/$TASK_ID"
git worktree add "$WORKTREE_PATH" -b "feat/be/$TASK_ID"
cd "$WORKTREE_PATH"
# → 이 디렉토리에서 codex/claude 실행
# → main 워크트리와 파일 충돌 없음

# 완료 후
git worktree remove "$WORKTREE_PATH"
```

현재 구현: `worker.sh`에서 간단한 `git checkout -b` 방식 사용.
병렬 태스크가 2개 이상 동시 실행될 때 worktree로 업그레이드 권장.

---

## 5. Claude Code Hooks 설정

### 5.1 현재 활성 훅 (`.claude/settings.local.json`)

#### Stop Hook — 세션 종료 시 승인 대기 태스크 브리핑
```json
{
  "Stop": [{
    "hooks": [{
      "type": "command",
      "command": "cd /path/to/plshare_2 && zsh .claude/scripts/propose-decisions.sh 2>/dev/null || true",
      "timeout": 15
    }]
  }]
}
```
**효과**: Claude 응답이 끝날 때마다 승인 대기 중인 태스크를 자동으로 요약해서 보여줌.
→ Operator가 "뭘 승인해야 하나"를 별도로 확인할 필요 없음.

#### PostToolUse Write Hook — 아티팩트 기록
```json
{
  "PostToolUse": [{
    "matcher": "Write|Edit",
    "hooks": [{
      "type": "command",
      "command": "jq -r '.tool_input.file_path // empty' | { read -r f; [[ \"$f\" == */docs/20-product/* ]] && echo \"$(date) WRITE $f\" >> .claude/logs/artifact-writes.log; } 2>/dev/null || true",
      "timeout": 5
    }]
  }]
}
```
**효과**: Claude가 docs/20-product/ 하위 파일을 수정할 때마다 자동 로그.
→ 어느 세션에서 어떤 아티팩트를 언제 수정했는지 추적 가능.

### 5.2 권장 추가 훅 (선택적)

#### PreToolUse Bash Hook — 위험 명령 차단
```json
{
  "PreToolUse": [{
    "matcher": "Bash",
    "hooks": [{
      "type": "command",
      "command": "cmd=$(jq -r '.tool_input.command'); echo \"$cmd\" | grep -qE '(git push --force|rm -rf|DROP TABLE)' && echo '{\"decision\":\"block\",\"reason\":\"위험 명령 차단\"}' || true"
    }]
  }]
}
```

#### PostCompact Hook — 컴팩션 후 컨텍스트 복원 알림
```json
{
  "PostCompact": [{
    "hooks": [{
      "type": "command",
      "command": "echo '{\"systemMessage\": \"⚠️  컨텍스트 컴팩션 완료. CLAUDE.md 재확인 권장\"}'"
    }]
  }]
}
```

---

## 6. 오케스트레이터 루프 설계

### 6.1 run-orchestrator.sh 실행 패턴

```zsh
# Claude Desktop 터미널에서 실행
zsh .claude/scripts/run-orchestrator.sh

# 한 번만 실행 (CI/cron용)
zsh .claude/scripts/run-queue-once.sh
```

**Orchestrator 내부 루프 (최대 20 iteration)**:
```
iteration마다:
1. sync-queue   → 의존성 해소 체크, 새 awaiting_approval 발생
2. review       → review 상태 태스크 자동 검토
3. unblock      → 의존성 해소된 blocked 태스크 → awaiting_approval
4. progress 없으면 break (무한루프 방지)
```

### 6.2 BLOCKING_QUESTION 에스컬레이션

Worker가 진행 불가 상황 감지 시:
```
# 에이전트가 로그 파일에 기록
echo "BLOCKING_QUESTION: Apple Music API scope 결정 필요. Current assumption: read-only. Confirm?" >> .claude/logs/worker-gemini.log
# → status를 failed로 마킹하지 않고 in_progress 유지
# → Orchestrator가 이 패턴 감지 → 사람에게 알림
```

현재 감지 방식: `review-task.sh`에서 실패 시 reason 필드에 기록 → Operator가 확인.
향후 개선: Orchestrator가 log tail을 스캔해서 `BLOCKING_QUESTION:` 패턴 자동 감지.

---

## 7. 듀얼 모니터 배치 가이드

### 7.1 권장 레이아웃

```
┌─────────────────────────────┐  ┌─────────────────────────────┐
│       모니터 1 (주)          │  │       모니터 2 (보조)         │
│                             │  │                             │
│  ┌─────────────────────┐    │  │  ┌─────────────────────┐    │
│  │  Claude Desktop App │    │  │  │  Terminal A          │    │
│  │  (Orchestrator)     │    │  │  │  worker.sh gemini-cli│    │
│  │                     │    │  │  │  [WAITING / RUNNING] │    │
│  │  - 태스크 승인       │    │  │  └─────────────────────┘    │
│  │  - 결정 리뷰         │    │  │                             │
│  │  - 전략 논의         │    │  │  ┌─────────────────────┐    │
│  └─────────────────────┘    │  │  │  Terminal B          │    │
│                             │  │  │  worker.sh codex-cli │    │
│  ┌─────────────────────┐    │  │  │  [WAITING / RUNNING] │    │
│  │  Terminal (main)    │    │  │  └─────────────────────┘    │
│  │  run-orchestrator   │    │  │                             │
│  │  approve.sh         │    │  │  ┌─────────────────────┐    │
│  │  create-pr.sh       │    │  │  │  Terminal C (선택)   │    │
│  └─────────────────────┘    │  │  │  git log / gh pr    │    │
│                             │  │  │  tail -f logs/      │    │
└─────────────────────────────┘  │  └─────────────────────┘    │
                                  └─────────────────────────────┘
```

### 7.2 각 터미널 실행 명령

| 터미널 | 역할 | 실행 명령 |
|:---|:---|:---|
| **모니터1 - Claude App** | Orchestrator, 승인, 전략 | Claude Desktop App 사용 |
| **모니터1 - Main Terminal** | 오케스트레이터 루프, 승인 | `zsh .claude/scripts/run-orchestrator.sh` |
| **모니터2 - Terminal A** | Gemini worker | `AGENT=gemini-cli zsh .claude/scripts/worker.sh gemini-cli` |
| **모니터2 - Terminal B** | Codex worker | `AGENT=codex-cli zsh .claude/scripts/worker.sh codex-cli` |
| **모니터2 - Terminal C** | 모니터링 | `watch -n5 'ruby .claude/scripts/task_queue.rb docs/10-operations/tasks/task-queue.yaml list'` |

---

## 8. 일반 운영 루틴

### 8.1 하루 시작 루틴

```zsh
cd /path/to/plshare_2

# 1. 현재 큐 상태 확인
ruby .claude/scripts/task_queue.rb docs/10-operations/tasks/task-queue.yaml list

# 2. 승인 대기 태스크 브리핑
zsh .claude/scripts/propose-decisions.sh

# 3. 승인
zsh .claude/scripts/approve.sh all   # 또는 개별: approve.sh <task-id>

# 4. Worker 시작 (모니터2)
# Terminal A: zsh .claude/scripts/worker.sh gemini-cli
# Terminal B: zsh .claude/scripts/worker.sh codex-cli

# 5. Orchestrator 시작 (모니터1)
zsh .claude/scripts/run-orchestrator.sh
```

### 8.2 태스크 완료 후 루틴

```zsh
# 1. PR 생성
zsh .claude/scripts/create-pr.sh <task-id>

# 2. PR 리뷰 후 머지
gh pr merge <pr-number> --squash --delete-branch

# 3. 큐 동기화 (다음 태스크 언블록)
zsh .claude/scripts/sync-queue.sh

# 4. 새로운 승인 대기 태스크 확인
zsh .claude/scripts/propose-decisions.sh
```

### 8.3 문제 발생 시

```zsh
# 태스크 재시도
ruby .claude/scripts/task_queue.rb docs/10-operations/tasks/task-queue.yaml \
  set-status <task-id> pending

# 실패 원인 확인
cat .claude/logs/<task-id>.log | tail -50

# 강제 완료 처리 (주의!)
ruby .claude/scripts/task_queue.rb docs/10-operations/tasks/task-queue.yaml \
  set-status <task-id> done
```

---

## 9. 현재 큐 상태 & 다음 단계

### 9.1 완료된 작업

| 태스크 ID | 상태 | PR |
|:---|:---|:---|
| `po-strategy-sync-001` | done | merged |
| `po-platform-direction-review-001` | done | (직접 커밋) |
| BE domain entities | done | PR #1 (open) |
| FE initial scaffold | done | PR #2 (open) |
| ops/git-workflow-rules | done | PR #3 (open) |

### 9.2 진행 중 / 대기 중

| 태스크 ID | 상태 | 다음 액션 |
|:---|:---|:---|
| `be-adapter-arch-001` | awaiting_approval | 브리핑 읽고 승인 → Codex worker 실행 |
| `pd-ux-flow-001` | blocked | `be-adapter-arch-001` 완료 후 자동 언블록 |
| `fe-implementation-plan-001` | blocked | `pd-ux-flow-001` 완료 후 자동 언블록 |

### 9.3 즉시 실행 가능한 액션

```zsh
# PR #1, #2, #3 머지
gh pr merge 1 --squash --delete-branch
gh pr merge 2 --squash --delete-branch  
gh pr merge 3 --squash --delete-branch

# be-adapter-arch-001 승인 (문서 작업, 구현 없음)
zsh .claude/scripts/approve.sh be-adapter-arch-001

# Codex worker 시작
zsh .claude/scripts/worker.sh codex-cli
```

---

## 10. 향후 개선 로드맵

| 우선순위 | 개선 항목 | 설명 |
|:---|:---|:---|
| P1 | Git Worktree 자동화 | worker.sh에서 태스크별 worktree 자동 생성/삭제 |
| P1 | BLOCKING_QUESTION 자동 감지 | orchestrator가 로그 스캔해서 에스컬레이션 자동 감지 |
| P2 | Slack/Discord 알림 | Stop hook에서 웹훅 호출로 승인 요청 알림 발송 |
| P2 | 태스크 실행 시간 추적 | started_at/completed_at 기반 성능 메트릭 |
| P3 | 자동 PR 머지 봇 | CI 통과 + done_criteria 충족 시 자동 머지 |
| P3 | 멀티 레포 지원 | BE/FE 레포 분리 시 cross-repo task 핸들링 |
