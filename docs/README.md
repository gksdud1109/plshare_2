# Docs

이 디렉터리는 프로젝트의 기준 문서, 운영 문서, 제품 문서를 역할과 개발 흐름에 맞춰 정리한 저장소다.

## 빠른 길찾기

- 팀 운영 규칙: `docs/10-operations/`
- 에이전트 역할 문서: `docs/10-operations/agents/`
- 프롬프트와 작업 모듈: `docs/10-operations/prompts/`, `docs/10-operations/modules/`
- 제품 전략과 최신 PRD: `docs/20-product/strategy/`
- 시장 리서치: `docs/20-product/research/`
- 기능/비기능 요구사항: `docs/20-product/requirements/`
- UX 및 화면 설계: `docs/20-product/design/`
- 데이터/기술 설계: `docs/20-product/data/`
- 구현 및 전달 메모: `docs/20-product/delivery/`

## 디렉터리 구조

- `00-foundation/`
  프로젝트의 기준점이 되는 문서
- `10-operations/`
  사람과 AI가 협업하는 운영 규칙, 에이전트, 모듈, 프롬프트
- `20-product/`
  실제로 무엇을 만들고 있는지 정의하는 제품 문서

## 권장 읽기 순서

1. `docs/00-foundation/README.md`
2. `docs/10-operations/README.md`
3. `docs/20-product/README.md`

## 현재 기준 문서

- 문서 구조 기준: `docs/00-foundation/document-architecture.md`
- 운영 모델: `docs/10-operations/agent-operating-model.md`
- 전체 워크플로우: `docs/10-operations/ai-agent-workflow.md`
- 최신 PRD: `docs/20-product/strategy/prd-v0.2.md`
- 최신 리서치: `docs/20-product/research/market-research-v0.3.md`

## 공통 협업 규칙

사람과 모든 agent는 아래 5개만 공통으로 지킨다.

1. 작업 시작 전 `current` 문서를 먼저 읽는다.
2. 한 작업은 한 폴더에만 주 산출물을 남긴다.
3. 전략, 요구사항, UX, 데이터, 구현 문서를 한 번에 섞어 고치지 않는다.
4. 내 범위를 넘는 결정은 직접 확정하지 말고 handoff 또는 `docs/20-product/delivery/implementation/open-issues.md`에 남긴다.
5. 최신 문서는 하나만 유지하고, 이전 버전은 `archive/`로 보낸다.

## 협업 기본 흐름

문서 협업은 아래 순서를 기본으로 한다.

1. `strategy`
2. `research`
3. `requirements`
4. `design`
5. `data`
6. `delivery`

위 단계를 거슬러 올라가는 수정이 필요하면, 직접 넓게 다시 쓰지 말고 상위 문서 담당에게 넘긴다.

루트 `docs/`에는 안내 문서만 두고, 실제 산출물 기준은 각 하위 README에서 관리한다.
