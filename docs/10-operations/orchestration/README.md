# Orchestration

이 디렉터리는 현재 프로젝트의 비동기 협업 오케스트레이션 기준 문서를 둔다.

핵심 문서:
- `sqlite-queue-design-v0.1.md`
- `worker-lanes-v0.1.md`
- `operator-manual-v0.1.md`
- `branch-pr-governance-v0.1.md`

실행 레이어:
- `.orchestrator/`

핵심 원칙:
- 오케스트레이션은 LLM이 아니라 deterministic queue가 맡는다
- LLM은 specialist worker로만 동작한다
- 사람은 승인과 freeze point만 결정한다
