# Document Architecture

## 목적
이 문서는 `docs/` 아래 문서가 어떤 목적을 가지며, 어디에 위치하고, 언제 갱신되고, 언제 아카이브되는지 정리한다.

핵심 원칙:
- 문서는 `foundation -> operations -> strategy -> research -> requirements -> design -> data -> delivery` 흐름을 따른다.
- 루트에는 내비게이션 문서만 둔다.
- 최신본과 이력본을 구분한다.
- 한 문서는 하나의 책임만 가진다.

---

## 1. 문서 계층

### A. Foundation
프로젝트 전체의 기준점이 되는 문서.

위치:
- `docs/00-foundation/`

예시:
- `docs/00-foundation/life-logging-ledger-plan.md`
- `docs/00-foundation/document-architecture.md`

역할:
- 프로젝트의 기본 문제의식과 문서 구조 기준을 고정한다.

### B. Operations
사람과 AI가 협업하는 규칙을 정의하는 문서.

위치:
- `docs/10-operations/`

예시:
- `docs/10-operations/agent-operating-model.md`
- `docs/10-operations/ai-agent-workflow.md`
- `docs/10-operations/agents/`
- `docs/10-operations/modules/`
- `docs/10-operations/prompts/`

역할:
- 역할 분담, 핸드오프, 프롬프트, 모듈 계약을 관리한다.

### C. Product
무엇을 만들지 정의하는 문서.

위치:
- `docs/20-product/strategy/`
- `docs/20-product/research/`
- `docs/20-product/requirements/`

예시:
- PRD
- 제품 전략
- 시장 조사
- 기능 요구사항
- 비기능 요구사항
- MVP 우선순위

역할:
- 제품 범위와 검증 질문을 정리한다.

### D. Design
어떻게 경험하게 할지 정의하는 문서.

위치:
- `docs/20-product/design/`

예시:
- 사용자 플로우
- 화면 스펙
- 카피 가이드
- 감성 UI 원칙

### E. Data / Technical Design
어떻게 저장하고 연결할지 정의하는 문서.

위치:
- `docs/20-product/data/`

예시:
- ERD
- RLS 초안
- 데이터 동의 정책
- 메타데이터 구조

### F. Delivery
실행과 변경 기록을 남기는 문서.

위치:
- `docs/20-product/delivery/`

예시:
- decision log
- open issues
- architecture notes
- rollout notes

---

## 2. 최신본 규칙

### Strategy
- 최신 전략 문서는 `docs/20-product/strategy/` 루트에 둔다.
- 이전 PRD 버전은 `docs/20-product/strategy/archive/`에 둔다.

### Research
- 최신 리서치 문서는 `docs/20-product/research/` 루트에 둔다.
- 이전 버전은 `docs/20-product/research/archive/`에 둔다.

### Requirements
- 최신 요구사항 문서는 `docs/20-product/requirements/` 루트에 둔다.
- 이전 버전은 `docs/20-product/requirements/archive/`에 둔다.

### 기타 버전 문서
- 최신본 우선 원칙을 적용한다.
- 이전 버전이 중요한 경우에만 같은 폴더에 `archive/`를 만든다.

---

## 3. 문서 명명 규칙

기본 규칙:
- 설명적인 kebab-case 사용
- 버전이 필요한 문서는 `-v0.1`, `-v0.2`, `-v1.0` 형식 사용
- 문서 종류가 파일명에서 드러나야 한다

예시:
- `prd-v0.2.md`
- `market-research-v0.3.md`
- `screen-specs-v0.1.md`
- `erd-v0.1.md`

---

## 4. 문서 생성 순서

권장 순서:
1. Foundation 문서
2. Operations 기준 문서
3. Strategy
4. Research
5. Requirements
6. Design
7. Data
8. Delivery

---

## 5. 역할별 주 문서

- Product Owner: `docs/20-product/strategy`, `docs/20-product/research`, `docs/20-product/requirements`
- Product Designer: `docs/20-product/design`
- Frontend Engineer: `docs/20-product/design`, `docs/20-product/delivery`
- Backend Engineer: `docs/20-product/data`, `docs/20-product/delivery`
- Reviewer / Operator: `docs/10-operations`, `docs/20-product/delivery`

---

## 6. 문서 라이프사이클

### Draft
- 초안 작성 중
- AI가 생성하거나 사람이 편집 중인 단계

### Current
- 현재 기준 문서
- 다른 문서와 구현의 입력으로 사용되는 단계

### Archived
- 더 이상 최신 기준점은 아니지만 추적을 위해 보관되는 단계

---

## 7. 폴더별 책임 요약

```text
docs/
  README.md
  00-foundation/
  10-operations/
    agents/
    modules/
    prompts/
  20-product/
    strategy/
      archive/
    research/
      archive/
    requirements/
      archive/
    design/
      ux/
    data/
    delivery/
      implementation/
```
