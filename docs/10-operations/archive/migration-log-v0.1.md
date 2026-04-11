# Docs Migration Log v0.1

## 목적
이 문서는 `docs/10-operations/archive/document-restructure-proposal-v0.1.md`를 기준으로 실행한 문서 구조 마이그레이션 결과를 기록한다.

실행 목표:
- 운영 문서와 제품 문서 분리
- 제품 문서를 개발 흐름 기준으로 재배치
- current/archive 구조 명확화
- 내부 경로 참조 정리

---

## 1. 생성된 새 구조

생성된 상위 구조:
- `docs/00-foundation/`
- `docs/10-operations/`
- `docs/20-product/`

생성된 제품 하위 구조:
- `docs/20-product/strategy/`
- `docs/20-product/research/`
- `docs/20-product/requirements/`
- `docs/20-product/design/`
- `docs/20-product/data/`
- `docs/20-product/delivery/`

추가 생성된 navigation/guide 문서:
- `docs/00-foundation/README.md`
- `docs/10-operations/README.md`
- `docs/20-product/README.md`
- `docs/20-product/requirements/README.md`
- `docs/20-product/requirements/archive/README.md`
- `docs/20-product/research/archive/README.md`
- `docs/20-product/design/README.md`
- `docs/20-product/delivery/README.md`

---

## 2. 이동된 파일

### Foundation
- `docs/life-logging-ledger-plan.md` -> `docs/00-foundation/life-logging-ledger-plan.md`
- `docs/document-architecture.md` -> `docs/00-foundation/document-architecture.md`

### Operations
- `docs/agent-operating-model.md` -> `docs/10-operations/agent-operating-model.md`
- `docs/ai-agent-workflow.md` -> `docs/10-operations/ai-agent-workflow.md`
- `docs/document-restructure-proposal-v0.1.md` -> `docs/10-operations/document-restructure-proposal-v0.1.md`
- `docs/agents/*` -> `docs/10-operations/agents/*`
- `docs/modules/*` -> `docs/10-operations/modules/*`
- `docs/prompts/*` -> `docs/10-operations/prompts/*`

### Product / Strategy
- `docs/prd/prd-v0.2.md` -> `docs/20-product/strategy/prd-v0.2.md`
- `docs/prd/product-strategy-v0.1.md` -> `docs/20-product/strategy/product-strategy-v0.1.md`
- `docs/prd/mvp-feature-priority-v0.1.md` -> `docs/20-product/strategy/mvp-feature-priority-v0.1.md`
- `docs/prd/archive/prd-v0.1.md` -> `docs/20-product/strategy/archive/prd-v0.1.md`
- `docs/prd/README.md` -> `docs/20-product/strategy/README.md`
- `docs/prd/archive/README.md` -> `docs/20-product/strategy/archive/README.md`

### Product / Research
- `docs/research/market-research-v0.3.md` -> `docs/20-product/research/market-research-v0.3.md`
- `docs/research/market-research-v0.2.md` -> `docs/20-product/research/archive/market-research-v0.2.md`
- `docs/research/market-research-v0.1.md` -> `docs/20-product/research/archive/market-research-v0.1.md`
- `docs/research/README.md` -> `docs/20-product/research/README.md`

### Product / Requirements
- `docs/prd/playlist-first-product-intent.md` -> `docs/20-product/requirements/playlist-first-product-intent.md`
- `docs/prd/functional-requirements-v0.2.md` -> `docs/20-product/requirements/functional-requirements-v0.2.md`
- `docs/prd/non-functional-requirements-v0.2.md` -> `docs/20-product/requirements/non-functional-requirements-v0.2.md`
- `docs/prd/functional-requirements-v0.1.md` -> `docs/20-product/requirements/archive/functional-requirements-v0.1.md`
- `docs/prd/non-functional-requirements-v0.1.md` -> `docs/20-product/requirements/archive/non-functional-requirements-v0.1.md`

### Product / Design
- `docs/ux/*` -> `docs/20-product/design/ux/*`

### Product / Data
- `docs/data/*` -> `docs/20-product/data/*`

### Product / Delivery
- `docs/implementation/*` -> `docs/20-product/delivery/implementation/*`
- 추가 발견: `docs/implementation/decision-log.md` -> `docs/20-product/delivery/implementation/decision-log.md`

---

## 3. 참조가 수정된 문서

### Navigation / structure
- `docs/README.md`
- `docs/00-foundation/document-architecture.md`
- `docs/10-operations/README.md`
- `docs/20-product/README.md`
- `docs/20-product/strategy/README.md`
- `docs/20-product/strategy/archive/README.md`
- `docs/20-product/research/README.md`
- `docs/20-product/design/README.md`
- `docs/20-product/delivery/README.md`

### Operations docs
- `docs/10-operations/agent-operating-model.md`
- `docs/10-operations/ai-agent-workflow.md`
- `docs/10-operations/modules/module-registry.md`
- `docs/10-operations/agents/product-owner.md`
- `docs/10-operations/agents/gemini-strategist.md`
- `docs/10-operations/agents/claude-experience-designer.md`
- `docs/10-operations/agents/codex-cli-implementer.md`
- `docs/10-operations/prompts/README.md`
- `docs/10-operations/prompts/doc-restructure-execution-v0.1-prompt.md`

### Product docs
- `docs/20-product/strategy/prd-v0.2.md`
- `docs/20-product/strategy/mvp-feature-priority-v0.1.md`
- `docs/20-product/requirements/functional-requirements-v0.2.md`
- `docs/20-product/requirements/non-functional-requirements-v0.2.md`
- `docs/20-product/research/archive/market-research-v0.1.md`
- `docs/20-product/research/archive/market-research-v0.2.md`

---

## 4. current/archive 정리 결과

### Strategy
현재:
- `prd-v0.2.md`
- `product-strategy-v0.1.md`
- `mvp-feature-priority-v0.1.md`

Archive:
- `archive/prd-v0.1.md`

### Research
현재:
- `market-research-v0.3.md`

Archive:
- `archive/market-research-v0.1.md`
- `archive/market-research-v0.2.md`

### Requirements
현재:
- `playlist-first-product-intent.md`
- `functional-requirements-v0.2.md`
- `non-functional-requirements-v0.2.md`

Archive:
- `archive/functional-requirements-v0.1.md`
- `archive/non-functional-requirements-v0.1.md`

---

## 5. 애매한 문서와 보수적 배치

### `document-restructure-proposal-v0.1.md`
배치:
- `docs/10-operations/`

이유:
- 제품 결정 문서가 아니라 문서 운영 구조를 바꾸는 제안서이기 때문

### `life-logging-ledger-plan.md`
배치:
- `docs/00-foundation/`

이유:
- 현재 전략/PRD의 상위 입력 문서 역할을 하며, 최신 제품 결정 문서라기보다 프로젝트 출발점에 가깝기 때문

### `implementation/decision-log.md`
배치:
- `docs/20-product/delivery/implementation/decision-log.md`

이유:
- 초기 스캔에는 드러나지 않았지만 실제 구현 결정 로그 파일이 루트 계열에 남아 있었고, delivery 문서로 보는 편이 안전했기 때문

### Prompt archive
배치:
- `docs/10-operations/prompts/archive/`

이유:
- 다수 프롬프트가 과거 경로나 과거 버전 문서를 가리키는 실행 기록 성격을 가지기 때문

---

## 6. 미해결 사항

아래 경로들은 이번 마이그레이션에서 새로 만들지 않았다.
이들은 원래도 실제 문서가 없었거나, 향후 생성 예정인 placeholder 경로다.

- `docs/20-product/research/risk-register.md`
- `docs/20-product/data/erd-v0.1.md`
- `docs/20-product/design/ux/user-flows.md`
- `docs/20-product/design/ux/screen-specs.md`
- `docs/20-product/design/ux/copy-guide.md`
- `docs/20-product/delivery/implementation/decision-log.md`
- `docs/20-product/delivery/implementation/ui-decision-log.md`
- `docs/20-product/delivery/implementation/open-issues.md`

이 항목들은 마이그레이션 오류가 아니라, 운영 문서와 모듈 문서에서 예상 산출물로 참조하는 경로다.

---

## 7. 후속 정리 권장 사항

1. `docs/10-operations/document-restructure-proposal-v0.1.md`는 historical proposal이므로, source path 예시를 유지할지 실제 path 중심으로 한 번 더 정리할지 결정할 것
2. `docs/10-operations/prompts/archive/`의 과거 프롬프트는 새 구조 기준으로 모두 한 번 검토할 것
3. `docs/20-product/design/ux/`, `docs/20-product/data/`, `docs/20-product/delivery/implementation/`에 실제 current 문서를 채울 것
4. 이후 새 문서를 만들 때는 반드시 `20-product` 하위 단계별 폴더를 따를 것

---

## 8. 검증 메모

검증 완료 항목:
- 루트 `docs/README.md`에서 새 구조로 이동 가능
- old `docs/prd/`, `docs/research/` 폴더 제거
- `current`와 `archive`가 전략/리서치/요구사항에 대해 분리됨
- 주요 운영 문서와 제품 문서의 경로 참조가 새 구조를 가리키도록 수정됨
