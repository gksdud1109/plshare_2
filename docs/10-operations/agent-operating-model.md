# Agent Operating Model

## 1. 목적
이 문서는 `Gemini`, `Claude`, `Codex CLI`, `Codex 앱`을 이 프로젝트에서 어떻게 분업시키고, 어떤 문서를 어디에 남길지 고정하기 위한 운영 기준이다.

핵심 원칙:
- AI별로 브랜치를 나누지 않는다.
- 역할은 AI별로 나누고, 브랜치는 산출물별로 나눈다.
- 모든 중간 산출물은 반드시 `md` 문서로 저장한다.
- 코드 작업 전에는 반드시 문서 기준점이 먼저 있어야 한다.

현재 동결 기준:
- 단일 기준은 `docs/20-product/strategy/product-baseline-v2.md`
- P0는 Google identity, Spotify/YouTube import, Emotional Context, social sharing,
  Spotify -> YouTube Music conversion
- ranking, gift, Apple export는 P1
- proof signal과 B2B는 P0 범위 밖
- 문서가 충돌하면 product baseline이 우선한다

---

## 2. 역할 고정

이 프로젝트의 기본 역할 단위는 `PO`, `Product Designer`, `Frontend Engineer`, `Backend Engineer`, `Reviewer / Operator`다.
AI 도구는 이 역할을 수행하는 수단으로 배치한다.

### Product Owner
책임:
- PRD 초안 작성
- 시장/사용자/비즈니스 구조화
- MVP 범위 결정
- 기능 우선순위 정리
- 정책/리스크 문서 초안

산출물 예시:
- `docs/20-product/strategy/prd-v0.2.md`
- `docs/20-product/research/market-research-v0.3.md`
- `docs/20-product/strategy/mvp-feature-priority-v0.1.md`

원칙:
- 제품 방향과 범위를 고정한다.
- 구현 방법을 단독으로 확정하지 않는다.

주 담당 AI:
- Primary: `Gemini`
- Backup: `Claude`

---

### Product Designer
책임:
- UX 플로우 설계
- 정보 구조 설계
- 화면 스펙 작성
- 카피라이팅
- 상태별 UX 정의

산출물 예시:
- `docs/20-product/design/ux/user-flows-v0.1.md`
- `docs/20-product/design/ux/screen-specs-v0.1.md`
- `docs/20-product/design/ux/copy-guide-v0.1.md`

원칙:
- 코드 구현보다 화면과 경험 설계에 집중한다.
- "어떻게 보여야 하는가"를 명확하게 만든다.

주 담당 AI:
- Primary: `Claude`
- Backup: `Gemini`

---

### Frontend Engineer
책임:
- 프론트엔드 화면 구현
- UI 상태 처리
- 폼과 상호작용 구현
- 스펙을 UI 코드로 반영
- UI 차이 문서화

산출물 예시:
- 프론트엔드 코드
- `docs/20-product/delivery/implementation/ui-decision-log.md`

원칙:
- UX 문서를 기준으로 구현한다.
- 서버 구조를 단독 결정하지 않는다.

주 담당 AI:
- Primary: `Codex CLI`
- Backup: `Codex 앱`
- Design support: `Claude`

---

### Backend Engineer
책임:
- API/인증/DB 구현
- 메타데이터 수집 구조 구현
- 데이터 무결성 유지
- 테스트 및 검증
- 구현 의사결정 기록

산출물 예시:
- 백엔드 코드
- 마이그레이션
- `docs/20-product/delivery/implementation/decision-log.md`
- `docs/20-product/delivery/implementation/open-issues.md`

원칙:
- 문서 없는 기능은 바로 구현하지 않는다.
- 이슈 단위로 받아 끝까지 검증한다.

주 담당 AI:
- Primary: `Codex CLI`
- Backup: `Codex 앱`
- Research support: `Gemini`

---

### Reviewer / Operator
책임:
- 병렬 작업 orchestration
- worktree 기반 동시 작업
- 코드리뷰
- 반복 작업 자동화
- 문서와 코드 진행 상태 연결

산출물 예시:
- 병렬 작업 브랜치
- 리뷰 코멘트
- 자동화 설정
- `docs/20-product/delivery/implementation/open-issues.md`

원칙:
- 실행 허브로 사용한다.
- 조정, 검토, 자동화에 집중한다.

주 담당 AI:
- Primary: `Codex 앱`
- Backup: `Codex CLI`

---

## 3. 문서 구조

권장 디렉터리 구조:

```text
docs/
  00-foundation/
    life-logging-ledger-plan.md
    document-architecture.md
  10-operations/
    agent-operating-model.md
    ai-agent-workflow.md
    agents/
      product-owner.md
      product-designer.md
      frontend-engineer.md
      backend-engineer.md
      reviewer-operator.md
    modules/
      module-registry.md
      handoff-template.md
    prompts/
      doc-restructure-execution-v0.1-prompt.md
  20-product/
    strategy/
      prd-v0.2.md
      product-strategy-v0.1.md
      mvp-feature-priority-v0.1.md
      archive/
    research/
      market-research-v0.3.md
      archive/
    requirements/
      playlist-first-product-intent.md
      functional-requirements-v0.2.md
      non-functional-requirements-v0.2.md
      archive/
    design/
      ux/
    data/
    delivery/
      implementation/
```

운영 규칙:
- 재사용 가능한 skill 목록은 저장소 최상위 `/skills/README.md` 단일 파일로만 관리한다
- 역할 문서는 `docs/10-operations/agents`
- 역할 간 인수인계는 `docs/10-operations/handoffs`
- 대체 가능한 업무 모듈은 `docs/10-operations/modules`
- 실행 프롬프트는 `docs/10-operations/prompts`
- 협업 규칙은 `docs/10-operations/collaboration-protocol-v0.1.md`
- 문서 구조 기준은 `docs/00-foundation/document-architecture.md`
- 최신 PRD는 `docs/20-product/strategy`, 이전 PRD는 `docs/20-product/strategy/archive`
- 리서치 문서는 `docs/20-product/research`
- 요구사항 문서는 `docs/20-product/requirements`
- 데이터 설계는 `docs/20-product/data`
- UX 문서는 `docs/20-product/design/ux`
- 실제 구현 중 결정사항은 `docs/20-product/delivery/implementation`

---

## 4. 브랜치 전략

권장 방식:
- 문서 정리 단계: `docs/restructure` 같은 브랜치 하나에서 정리
- 기능 구현 단계: 기능별 브랜치 생성

좋은 예시:
- `docs/restructure`
- `feat/auth-onboarding`
- `feat/playlist-create`
- `feat/proof-metadata-pipeline`

피해야 할 예시:
- `gemini-branch`
- `claude-branch`
- `codex-branch`

이유:
- AI 이름으로 나누면 브랜치 책임이 모호해진다.
- 산출물 기준 브랜치가 검토와 머지가 쉽다.

---

## 5. PRD 운영 원칙

질문에 대한 답은 `예, Gemini에게 가장 먼저 시킬 일은 PRD 초안 작성`이 맞다.

다만 권장 방식은 다음과 같다.

1. 현재 기획 초안을 기준 문서로 둔다.
2. Gemini에게 `PRD v0.1`을 작성시킨다.
3. 결과를 `docs/20-product/strategy/archive/prd-v0.1.md`로 저장한다.
4. Claude에게 그 PRD를 기반으로 UX 문서를 작성시킨다.
5. Codex는 PRD와 UX 문서가 생긴 뒤 구현을 시작한다.

즉, `기획 초안 -> PRD -> UX 스펙 -> 구현` 순서를 유지하는 것이 좋다.

---

## 6. 핸드오프 규칙

각 AI에 작업을 넘길 때는 항상 아래 4가지를 같이 준다.

- 입력 문서 경로
- 이번 작업의 산출물 경로
- 결정하면 안 되는 범위
- 완료 조건

예시:
- 입력: `docs/00-foundation/life-logging-ledger-plan.md`
- 출력: `docs/20-product/strategy/archive/prd-v0.1.md`
- 금지: 기술 스택 확정 금지, 토큰 이코노미 확정 금지
- 완료 조건: 문제 정의, 사용자, MVP 범위, 기능 요구사항, 성공 지표 포함

---

## 7. 지금 당장 권장하는 다음 순서

1. 이 문서를 기준으로 역할을 고정한다.
2. `docs/10-operations/agents/*`와 `docs/10-operations/modules/*`를 기준으로 역할과 대체 경로를 고정한다.
3. `docs/20-product/strategy/prd-v*.md`를 Product Owner 역할로 작성한다.
4. `docs/20-product/design/ux/*.md`를 Product Designer 역할로 작성한다.
5. 그 다음부터 Frontend/Backend Engineer 역할이 구현을 시작한다.
