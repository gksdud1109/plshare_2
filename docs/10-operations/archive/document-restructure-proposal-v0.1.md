# Document Restructure Proposal v0.1

## 목적
현재 `docs/` 안에는 두 성격의 문서가 섞여 있다.

- 운영 / 워크플로우 / 에이전트 문서
- 제품 / 리서치 / 요구사항 / 설계 문서

이 문서는 두 종류를 분리해 더 체계적이고 빠르게 문서를 찾고, 역할별로 작업하기 쉬운 구조를 제안한다.

---

## 1. 현재 구조의 문제

### 문제 1. 운영 문서와 제품 문서가 같은 레벨에 섞여 있다
예:
- `docs/10-operations/agent-operating-model.md`
- `docs/10-operations/ai-agent-workflow.md`
- `docs/20-product/strategy/prd-v0.2.md`
- `docs/20-product/research/market-research-v0.3.md`

이 구조에서는 `팀 운영 기준`과 `실제 제품 기준`이 탐색 경로상 섞인다.

### 문제 2. 역할 기반 탐색보다 폴더명 기반 탐색이 강하다
현재는 `agents`, `modules`, `prompts`, `prd`, `research` 같은 분류가 있지만,
`PO가 어디를 먼저 봐야 하는지`, `디자이너가 어디를 봐야 하는지`, `개발자가 어디를 봐야 하는지`가 구조에서 바로 드러나지 않는다.

### 문제 3. 제품 문서의 단계 흐름이 약하다
제품 문서는 보통 아래 흐름으로 소비된다.

`전략 -> 리서치 -> 요구사항 -> UX -> 데이터 -> 구현`

현재는 `prd`와 `research`는 있지만, `전략 계층`과 `요구사항 계층`이 분리되어 있지 않다.

---

## 2. 목표 원칙

새 구조는 아래를 만족해야 한다.

1. 운영 문서와 제품 문서를 분리한다.
2. 최신 기준 문서가 어디 있는지 빠르게 보여야 한다.
3. 역할별 진입점이 명확해야 한다.
4. 버전 문서와 아카이브 규칙이 일관돼야 한다.
5. 리포지토리의 실행 순서와 문서 구조가 대응돼야 한다.

---

## 3. 권장 목표 구조

```text
docs/
  README.md
  00-foundation/
    README.md
    life-logging-ledger-plan.md
    document-architecture.md

  10-operations/
    README.md
    agent-operating-model.md
    ai-agent-workflow.md
    agents/
    modules/
    prompts/

  20-product/
    README.md
    strategy/
      README.md
      prd-v0.2.md
      product-strategy-v0.1.md
      mvp-feature-priority-v0.1.md
      archive/
    research/
      README.md
      market-research-v0.3.md
      risk-register.md
      archive/
    requirements/
      README.md
      functional-requirements-v0.2.md
      non-functional-requirements-v0.2.md
      archive/
    design/
      README.md
      ux/
    data/
      README.md
    delivery/
      README.md
      implementation/
```

---

## 4. 이 구조가 좋은 이유

### A. 운영과 제품을 분리한다
- `10-operations/`: AI 협업 규칙, 프롬프트, 에이전트, 모듈
- `20-product/`: 실제 서비스 기획과 설계 산출물

이렇게 나누면 문서 목적이 혼동되지 않는다.

### B. 숫자 prefix로 정렬 순서를 강제한다
- `00-foundation`
- `10-operations`
- `20-product`

파일 탐색기에서 자연스럽게 위에서 아래로 읽힌다.

### C. 제품 문서를 단계별로 정리한다
- `strategy`
- `research`
- `requirements`
- `design`
- `data`
- `delivery`

이 구조는 실제 제품 개발 흐름과 대응된다.

### D. 역할별 접근 경로가 쉬워진다
- PO: `20-product/strategy`, `20-product/research`, `20-product/requirements`
- Product Designer: `20-product/design`
- Frontend Engineer: `20-product/design`, `20-product/delivery`
- Backend Engineer: `20-product/data`, `20-product/delivery`
- Reviewer / Operator: `10-operations`, `20-product/delivery`

---

## 5. 현재 파일 기준 추천 매핑

### Foundation으로 이동
- `docs/00-foundation/life-logging-ledger-plan.md` -> `docs/00-foundation/life-logging-ledger-plan.md`
- `docs/00-foundation/document-architecture.md` -> `docs/00-foundation/document-architecture.md`

### Operations로 이동
- `docs/10-operations/agent-operating-model.md` -> `docs/10-operations/agent-operating-model.md`
- `docs/10-operations/ai-agent-workflow.md` -> `docs/10-operations/ai-agent-workflow.md`
- `docs/10-operations/agents/*` -> `docs/10-operations/agents/*`
- `docs/10-operations/modules/*` -> `docs/10-operations/modules/*`
- `docs/10-operations/prompts/*` -> `docs/10-operations/prompts/*`

### Product / Strategy로 이동
- `docs/20-product/strategy/prd-v0.2.md` -> `docs/20-product/strategy/prd-v0.2.md`
- `docs/20-product/strategy/product-strategy-v0.1.md` -> `docs/20-product/strategy/product-strategy-v0.1.md`
- `docs/20-product/strategy/mvp-feature-priority-v0.1.md` -> `docs/20-product/strategy/mvp-feature-priority-v0.1.md`
- `docs/20-product/strategy/archive/*` -> `docs/20-product/strategy/archive/*`

### Product / Research로 이동
- `docs/20-product/research/market-research-v0.3.md` -> `docs/20-product/research/market-research-v0.3.md`
- `docs/research/*` -> `docs/20-product/research/*`

### Product / Requirements로 이동
- `docs/20-product/requirements/functional-requirements-v0.2.md` -> `docs/20-product/requirements/functional-requirements-v0.2.md`
- `docs/20-product/requirements/non-functional-requirements-v0.2.md` -> `docs/20-product/requirements/non-functional-requirements-v0.2.md`
- `docs/20-product/requirements/playlist-first-product-intent.md` -> `docs/20-product/requirements/playlist-first-product-intent.md`
- 이전 버전 요구사항도 같은 폴더 내 `archive/`로 이동

### Product / Design
- `docs/20-product/design/ux/*` -> `docs/20-product/design/ux/*`

### Product / Data
- `docs/20-product/data/*` -> `docs/20-product/data/*`

### Product / Delivery
- `docs/20-product/delivery/implementation/*` -> `docs/20-product/delivery/implementation/*`

---

## 6. 권장 운영 규칙

### Rule 1. 루트는 안내 역할만 한다
`docs/README.md`는 전체 내비게이션 역할만 한다.
실제 문서 기준은 각 하위 README에 둔다.

### Rule 2. 최신본 우선
- 각 폴더에는 최신본만 노출
- 이전 버전은 각 폴더의 `archive/`에 보관

### Rule 3. 한 문서 한 책임
- 전략 문서에 UX 디테일을 넣지 않는다
- 요구사항 문서에 운영 규칙을 넣지 않는다
- 운영 문서에 제품 방향 결정을 넣지 않는다

### Rule 4. 역할별 생성 책임
- PO는 `strategy`, `research`, `requirements`
- Designer는 `design`
- Frontend / Backend는 `data`, `delivery`
- Reviewer / Operator는 `operations`, `delivery`

---

## 7. 마이그레이션 순서

### Step 1. 구조 문서 먼저 확정
- 이 제안서를 기준으로 목표 구조 합의

### Step 2. 새 폴더와 README 생성
- 실제 파일 이동 전 빈 폴더와 안내 문서 생성

### Step 3. 운영 문서 이동
- `agents`, `modules`, `prompts`부터 분리

### Step 4. 제품 문서 이동
- `prd`, `research`, `ux`, `data`, `implementation` 순서로 이동

### Step 5. 링크/참조 경로 수정
- 문서 내부 경로와 프롬프트 경로 갱신

---

## 8. 추천 결론

가장 좋은 해법은 `docs`를 계속 한곳에 두되, 그 안을 아래 두 축으로 강하게 분리하는 것이다.

- `10-operations`: 사람과 AI가 협업하는 법
- `20-product`: 실제로 무엇을 만들고 있는가

이렇게 하면 운영 체계는 유지하면서도, 제품 문서가 더 명확한 흐름을 갖게 된다.
