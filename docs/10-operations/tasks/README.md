# Task Queue

이 디렉터리의 `task-queue.yaml`이 현재 큐의 단일 source of truth다.

현재 기준:
- current queue는 `task-queue.yaml`이다
- 실행기는 `.claude/scripts/task_queue.rb`, `worker.sh`, `review-task.sh`다
- `.orchestrator/state/queue.db`는 2026-04 시점의 legacy snapshot이며 실행에 사용하지 않는다

current 진입점:
- 워크플로우: `docs/10-operations/multi-agent-workflow-v1.md`
- 큐 확인: `ruby .claude/scripts/task_queue.rb task-queue.yaml ids-by-status pending`

SQLite 관련 문서는 역사적 설계 기록으로만 유지한다.
