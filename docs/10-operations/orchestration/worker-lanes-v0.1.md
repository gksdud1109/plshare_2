# Worker Lanes v0.1

## 목적
이 문서는 현재 프로젝트에서 어떤 worker가 어떤 종류의 task를 담당하는지 정의한다.

---

## 1. Lane 구조

### Lane A. Product Owner
- Worker: `gemini-cli`
- 책임:
  - strategy refinement
  - validation plan
  - research memo
  - priority clarification

### Lane B. Product Designer
- Worker: `claude-code`
- 책임:
  - user flows
  - screen specs
  - copy direction
  - emotional asset UX

### Lane C. Engineer
- Worker: `codex-cli`
- 책임:
  - backend architecture
  - frontend implementation planning
  - actual code changes
  - tests

### Lane D. Review
- Worker: deterministic `review-loop`
- 책임:
  - report 존재 확인
  - output 변경 확인
  - validate command 수행
  - done/failed 판정

---

## 2. 병렬성 원칙
- Product Owner lane은 독립 문서 작업을 병렬로 수행 가능
- Product Designer와 Engineer는 write scope가 겹치지 않을 때만 병렬
- Review lane은 항상 별도

---

## 3. 현재 프로젝트의 초기 병렬 실행

즉시 돌릴 수 있는 task:
- `po-platform-direction-review-001`
- `ops-branch-pr-consolidation-001`

이후 dependency 해제 후 돌릴 task:
- `po-validation-plan-001`
- `pd-ux-flow-001`
- `fe-implementation-plan-001`
- `be-adapter-arch-001`

추가 게이트:
- `be-adapter-arch-001`은 문서 산출물만 허용한다
- backend 구현은 사용자 검토 승인 이후 별도 task로 분리한다

---

## 4. 사람의 역할
사람은 아래만 결정한다.
- 승인 대기 task 승인
- failed task 재시도 여부
- freeze point 승인
- 제품 방향 변경 승인
