# Task Queue Legacy

이 디렉터리는 이전 `Claude + YAML` 기반 오케스트레이션 흔적을 보관한다.

현재 기준:
- source of truth queue는 `task-queue.yaml`이 아니다
- current queue는 `.orchestrator/state/queue.db`다
- current 설계와 운영 규칙은 `docs/10-operations/orchestration/`을 따른다

이 디렉터리를 유지하는 이유:
- 이전 시행착오를 기록으로 남기기 위해
- legacy script와 current SQLite 구조를 명확히 분리하기 위해

current 진입점:
- 설계: `docs/10-operations/orchestration/sqlite-queue-design-v0.1.md`
- lane 운영: `docs/10-operations/orchestration/worker-lanes-v0.1.md`
- 실무 매뉴얼: `docs/10-operations/orchestration/operator-manual-v0.1.md`
