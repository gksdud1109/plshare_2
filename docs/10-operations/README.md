# Operations

이 폴더에는 팀과 AI가 협업하는 운영 문서를 둔다.

현재 기준 문서:
- `agent-operating-model.md`: 역할 분담과 핸드오프 기준
- `collaboration-protocol-v0.1.md`: 문서, handoff, decision log 사용 규칙
- `ai-agent-workflow.md`: 전체 작업 흐름과 브랜치/이슈 운영 방식

현재 실행 기준:
- 제품 baseline은 `docs/20-product/strategy/product-baseline-v2.md`
- P0는 identity, import, Emotional Context, social share, Spotify→YTM corridor다
- `B2B`는 current scope가 아니라 `Future Expansion`
- 문서 충돌 시 `docs/README.md`의 현재 동결 기준을 따른다

하위 폴더:
- `agents/`: current 역할별 에이전트 문서
- `modules/`: 대체 가능한 작업 모듈과 입출력 계약
- `handoffs/`: current 작업 인수인계 문서
- `prompts/`: 실행 프롬프트와 프롬프트 아카이브
- `archive/`: historical 운영 문서

운영 문서는 제품 결정을 담기보다, 문서를 어떻게 만들고 이어받고 검토할지를 정의한다.

권장 읽기 순서:
1. `agent-operating-model.md`
2. `collaboration-protocol-v0.1.md`
3. `multi-agent-workflow-v1.md`
