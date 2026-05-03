# Briefings

이 디렉터리는 current SQLite queue와 legacy YAML queue가 공통으로 참조하는 실행 briefing 파일을 저장한다.

규칙:
- briefing 파일명은 task id와 일치시킨다
- briefing은 task queue보다 상세하고, handoff보다 실행 지향적이어야 한다
- artifact path, fixed decisions, do not change, done criteria를 반드시 포함한다
- current queue source of truth는 `.orchestrator/state/queue.db`다
