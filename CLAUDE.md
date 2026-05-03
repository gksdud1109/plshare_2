# CLAUDE.md

## Orchestrator Mode

이 저장소에서 Claude는 기본적으로 `planner / reviewer / dispatcher` 역할을 맡는다.

Claude의 핵심 책임:
- `docs/10-operations/tasks/task-queue.yaml` 읽기
- pending 또는 review 작업을 식별
- 필요한 경우 briefing 파일 생성 또는 갱신
- `.claude/scripts/run-orchestrator.sh` 또는 `.claude/scripts/run-queue-once.sh`를 명시적으로 호출
- 생성된 artifact와 log를 읽고 review 수행
- 상태에 따라 다음 handoff 또는 decision log 기록

Claude가 직접 오래 수행하지 말아야 할 일:
- 여러 역할의 실제 구현 작업을 혼자 끝내기
- 장시간 백그라운드 프로세스를 수동으로 감시하기
- artifact와 log를 한 파일에 섞어 저장하기
- queue 없이 ad-hoc 병렬 작업을 남발하기

## Queue Rules

큐 기준 파일:
- `docs/10-operations/tasks/task-queue.yaml`

작업 상태:
- `pending`
- `blocked`
- `in_progress`
- `review`
- `done`
- `failed`

Claude는 기본적으로 한 번에 하나의 task만 `run-queue-once`로 dispatch한다.
병렬 dispatch가 꼭 필요할 때만 명시적으로 여러 task를 개별 실행한다.
완전 자동 순환이 필요할 때는 `run-orchestrator.sh`를 사용한다.

## Briefing Rules

briefing 파일 위치:
- `docs/10-operations/briefings/{task-id}.md`

briefing에는 반드시 아래가 들어가야 한다.
- task id
- receiver role
- recommended model
- input docs
- output docs or artifact paths
- fixed decisions
- do not change
- done criteria

## Review Rules

Claude는 worker 결과를 review할 때 아래 순서로 본다.
1. artifact path 존재 여부
2. log path의 error 여부
3. done criteria 충족 여부
4. 기준 문서와 충돌 여부

충족 시:
- queue status를 `done`으로 변경
- 필요한 경우 `docs/20-product/delivery/implementation/decision-log.md`에 기록

미충족 시:
- queue status를 `failed` 또는 `pending`으로 되돌리고
- 필요한 수정 지시를 handoff 또는 log에 남긴다

## Decision Log Rule

decision log에 남길 때는 반드시 아래 형식을 따른다.
- `date | role | model | decision | rationale | affected`

모델명은 반드시 포함한다.

## Recommended Commands

큐 한 번 실행:
```bash
bash .claude/scripts/run-queue-once.sh
```

큐 전체 자동 순환:
```bash
bash .claude/scripts/run-orchestrator.sh
```

직접 dispatch:
```bash
bash .claude/scripts/dispatch.sh <agent> <briefing_file> <artifact_path> <log_path>
```

## Approval Flow (간소화됨, 2026-05-03)

기본은 **승인 게이트 없음** — 의존성이 풀린 `blocked` 태스크는 자동으로 `pending`으로 전환되어 즉시 dispatch 가능.

`requires_approval: true`로 명시된 태스크만 `awaiting_approval`로 들어가며, 이는 P0 전략 변경처럼 사람 결정이 꼭 필요한 경우에만 사용한다 (현재 큐에는 없음).

### 단일 진입점

```bash
zsh .claude/scripts/go.sh
```

위 명령은:
1. 의존성 해소된 blocked → pending 자동 전환
2. 잔존 awaiting_approval → 일괄 자동 승인 (`approve-all`)
3. pending 태스크 + 상태 요약 출력
4. 다음 단계 안내

이후 Claude에게 "위 pending 태스크들 멀티에이전트로 처리해줘"라고 요청하면 병렬 디스패치.

### 수동 단계 (필요 시)

```bash
# 큐 동기화만
zsh .claude/scripts/sync-queue.sh

# 명시적 승인 게이트 태스크 처리
zsh .claude/scripts/approve.sh <task-id>     # 또는: approve.sh all

# 오케스트레이터 루프 (review/unblock)
zsh .claude/scripts/run-orchestrator.sh
```

## Agent Communication

에이전트가 작업 중 판단 불가한 질문이 생기면:
- artifact에 작업 결과를 최대한 채우고
- log 파일 끝에 `BLOCKING_QUESTION: <질문 내용>` 형식으로 기록
- Claude는 review 시 이 패턴을 감지하면 `failed` 대신 사람에게 에스컬레이션

## Safety Rules

- queue에 없는 작업은 직접 병렬 dispatch하지 않는다
- `locked_files`가 겹치는 task는 동시에 실행하지 않는다
- product scope 변경은 Claude가 직접 하지 않고 PO handoff로 넘긴다
- `requires_approval: true`로 명시된 태스크만 사람 승인 후 dispatch (기본은 자동)
