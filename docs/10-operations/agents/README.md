# Agents

이 디렉터리는 역할 기반 에이전트의 페르소나, 책임, 입력/출력 형식, 대체 가능 범위를 정의한다.

## 🚨 세션 시작 시 반드시 할 일

1. **Git workflow 규칙을 먼저 읽는다:** [`../git-workflow.md`](../git-workflow.md)
2. Pre-flight 체크를 실행한다:
   ```bash
   git branch --show-current    # 내 브랜치 확인
   git status --short           # 더러운 파일 확인
   git log --oneline -5         # 최근 히스토리
   git stash list               # 미해결 stash
   ```
3. 본인 역할의 문서를 읽고, `docs/10-operations/handoffs/`에서 최신 지시서를 찾는다.
4. **반드시 자기 역할 prefix의 브랜치**에서 작업을 시작한다:
   - FE → `feat/fe/<기능명>`
   - BE → `feat/be/<기능명>`
   - Ops/문서 → `ops/<작업명>`
   - **절대로 `main`에 직접 커밋하지 않는다.**
5. 자기 영역(`/frontend`, `/backend`) 외 파일은 건드리지 않는다. 다른 에이전트의 브랜치에도 커밋하지 않는다.

원칙:
- 에이전트는 이름이 아니라 역할로 운영한다.
- 같은 역할은 다른 AI가 대체할 수 있도록 입력/출력 형식을 고정한다.
- 작업 요청은 반드시 이 문서들의 책임 범위 안에서 나눈다.
- 모든 agent는 `docs/README.md`의 공통 협업 규칙을 먼저 따른다.

공통 해석:
- 한 작업에는 하나의 주 산출물만 둔다.
- 다른 계층 문서를 직접 넓게 수정하지 않는다.
- 다음 단계로 넘길 판단은 handoff 형식으로 남긴다.
- 현재 제품 기준은 `docs/20-product/strategy/product-baseline-v2.md`다.
- P0는 Google identity, Spotify/YouTube import, Emotional Context, social sharing,
  Spotify -> YouTube Music conversion이다.
- ranking, gift, Apple export는 P1이며 B2B는 범위 밖이다.
- 문서 충돌 시 `docs/README.md`의 우선순위를 따른다.

구성:
- `product-owner.md`
- `product-designer.md`
- `frontend-engineer.md`
- `backend-engineer.md`
- `reviewer-operator.md`

historical 참고 문서:
- `archive/gemini-strategist.md`
- `archive/claude-experience-designer.md`
- `archive/codex-cli-implementer.md`
- `archive/codex-app-orchestrator.md`

현재 운영 원칙:
- 사람과 agent는 role-based 문서를 source of truth로 사용한다
- tool-based 문서는 historical reference로만 본다
