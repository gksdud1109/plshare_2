# Branch And PR Governance v0.1

## 목적
이 문서는 현재 프로젝트에서 role 기반으로 흩어진 작업 브랜치를 먼저 PR로 정리하고, 병합 이후 다시 `task 단위 브랜치`로 돌아가는 운영 규칙을 정의한다.

## 현재 원칙
- 기존 `PO / Product Designer / Frontend / Backend` 성격의 브랜치 산출물은 먼저 PR로 정리한다
- PR 정리와 병합이 끝나기 전에는 새 구현 브랜치를 늘리지 않는다
- 병합 이후 새 작업은 반드시 `task id` 기준 브랜치로 분리한다
- orchestrator는 branch/PR 정리 task가 `done` 되기 전까지 후속 FE/BE task를 열지 않는다

## 정리 순서
1. 현재 활성 브랜치와 변경 범위를 role 기준으로 분류한다
2. role 브랜치별로 PR 초안을 만든다
3. 충돌, 중복, 폐기 항목을 정리한다
4. 기준 브랜치에 병합한다
5. 이후 새 작업은 `task-id` 또는 `feat/<task-id>` 단위로 다시 시작한다

## PR 정리 대상
- Product Owner 산출물 브랜치
- Product Designer 산출물 브랜치
- Frontend Engineer 산출물 브랜치
- Backend Engineer 산출물 브랜치

## 병합 이후 규칙
- 한 브랜치는 한 task만 책임진다
- task briefing과 브랜치 이름은 대응되어야 한다
- PR 설명에는 `task id`, `briefing`, `done criteria`를 포함한다
- 병합 전 review lane과 human approval을 모두 거친다

## Orchestrator Gate
- `ops-branch-pr-consolidation-001`이 완료되기 전에는 새 구현 task를 `pending`으로 열지 않는다
- 전략 전환 검토가 동시에 진행 중이면 `po-platform-direction-review-001` 완료 후에만 제품/설계/구현 task를 연다

## 추천 브랜치 패턴
- 문서: `docs/<task-id>`
- 구현: `feat/<task-id>`
- 리뷰 대응: `fix/<task-id>`

## 현재 프로젝트 적용
- 먼저 branch/PR 정리 문서를 만든다
- 병합 계획과 기준 브랜치를 확정한다
- 그 다음 UX, backend architecture, frontend implementation plan task를 순차 해제한다
