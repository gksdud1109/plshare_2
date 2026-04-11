# Collaboration Protocol v0.1

## 목적
이 문서는 현재 정리된 `docs/` 구조 위에서 AI 에이전트가 어떻게 협업하고, 어떻게 작업을 넘기고, 무엇을 기록해야 하는지 규칙으로 명세한다.

핵심 목표:
- 문서는 기준점으로만 남긴다
- 실제 협업은 `handoff`와 `decision log` 중심으로 굴린다
- 역할별 책임과 다음 액션이 분명해야 한다

---

## 1. 협업 원칙

### Rule 1. 기준 문서와 작업 소통을 분리한다
- 전략, 요구사항, 리서치는 `docs/20-product` 문서에 남긴다
- 실제 작업 인수인계는 `handoff` 문서로 처리한다
- 확정 결정은 `decision log`에 남긴다

### Rule 2. 한 번에 하나의 기준 문서를 따른다
- 동일한 주제에 대해 여러 문서를 동시에 source of truth로 사용하지 않는다
- 현재 작업은 항상 최신 `strategy`, `requirements`, `design`, `data` 문서를 기준으로 삼는다
- 문서가 충돌하면 `product-strategy -> prd -> mvp-feature-priority -> functional/non-functional requirements -> design/data/delivery` 순으로 맞춘다

### Rule 3. handoff 없이 역할을 넘기지 않는다
- PO -> Designer
- PO -> Engineer
- Designer -> Engineer
- Engineer -> Reviewer

각 전환에는 반드시 handoff가 있어야 한다.

### Rule 4. decision은 짧고 확정적으로 남긴다
- 긴 회의록 대신 `무엇을 결정했는지`만 남긴다
- 결정하지 못한 것은 handoff나 open issues로 넘긴다

### Rule 5. 현재 동결된 제품 기준을 임의로 넓히지 않는다
- P0는 `Spotify import -> Assetize -> Apple Music export`로 고정한다
- 기술적 `proof signal`은 `P1`로 둔다
- `B2B`는 `Future Expansion`으로만 다룬다
- 이 기준을 바꾸려면 current 문서를 직접 넓히지 말고 PO handoff와 decision log를 먼저 갱신한다

---

## 2. 소통 수단

### 기준 문서
위치:
- `docs/20-product/...`

용도:
- 제품과 설계의 현재 기준점

### Handoff
위치:
- `docs/10-operations/handoffs/`

용도:
- 특정 역할에게 다음 작업을 넘기는 짧은 계약 문서

### Decision Log
위치:
- `docs/20-product/delivery/implementation/decision-log.md`

용도:
- 확정된 결정 기록

---

## 3. handoff 규칙

모든 handoff는 아래를 포함해야 한다.
- 작업명
- 받는 역할
- 권장 모델
- 입력 문서
- 출력 문서
- 현재 고정된 결정
- 변경 금지 범위
- 완료 조건
- 완료 후 넘길 다음 역할

핵심:
- handoff는 짧아야 한다
- handoff는 새 기획이 아니라 실행 계약이어야 한다

---

## 4. decision log 규칙

decision log의 각 항목은 아래 필드를 포함해야 한다.
- date
- role
- model
- decision
- rationale
- affected docs or files

`model` 필드는 필수다.

허용 예시:
- `Gemini`
- `Claude`
- `Codex CLI`
- `Codex App`

---

## 5. 추천 운영 흐름

1. PO가 최신 전략 문서를 확정한다
2. PO가 handoff로 다음 역할에 작업을 넘긴다
3. 작업 수행자는 기준 문서만 읽고 작업한다
4. 새 결정이 생기면 decision log에 남긴다
5. 결과물은 다음 handoff로 넘긴다

즉:
- `문서 = 기준`
- `handoff = 작업 계약`
- `decision log = 확정 기록`

---

## 6. 현재 프로젝트 기준 다음 흐름

1. Product Designer가 `Spotify import -> Assetize -> Apple export` UX 설계 시작
2. Engineer가 asymmetric adapter architecture 설계 시작
3. Reviewer / Operator가 handoff와 open issues를 current 기준으로 유지
