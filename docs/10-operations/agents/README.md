# Agents

이 디렉터리는 역할 기반 에이전트의 페르소나, 책임, 입력/출력 형식, 대체 가능 범위를 정의한다.

원칙:
- 에이전트는 이름이 아니라 역할로 운영한다.
- 같은 역할은 다른 AI가 대체할 수 있도록 입력/출력 형식을 고정한다.
- 작업 요청은 반드시 이 문서들의 책임 범위 안에서 나눈다.
- 모든 agent는 `docs/README.md`의 공통 협업 규칙을 먼저 따른다.

공통 해석:
- 한 작업에는 하나의 주 산출물만 둔다.
- 다른 계층 문서를 직접 넓게 수정하지 않는다.
- 다음 단계로 넘길 판단은 handoff 형식으로 남긴다.

구성:
- `product-owner.md`
- `product-designer.md`
- `frontend-engineer.md`
- `backend-engineer.md`
- `reviewer-operator.md`

도구별 참고 문서:
- `gemini-strategist.md`
- `claude-experience-designer.md`
- `codex-cli-implementer.md`
- `codex-app-orchestrator.md`
