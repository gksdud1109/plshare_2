# Module Registry

## 목적
업무를 모듈 단위로 나눠, 한 에이전트가 토큰 한도나 세션 문제로 중단되어도 다른 에이전트가 같은 입력/출력 계약으로 이어받을 수 있게 한다.

---

## 모듈 설계 원칙
- 한 모듈은 하나의 산출물만 책임진다.
- 입력 문서는 명시한다.
- 출력 경로를 고정한다.
- 완료 조건은 테스트 가능하거나 검토 가능해야 한다.
- 같은 모듈 안에 전략, UX, 구현을 섞지 않는다.

---

## 모듈 목록

### M1. Product Framing
- 목적: 기획 초안을 PRD 구조로 정리
- 입력: `docs/00-foundation/life-logging-ledger-plan.md`
- 출력: `docs/20-product/strategy/prd-v0.2.md`
- 주 담당: `Gemini`
- 대체 담당: `Claude`
- 완료 조건: 문제 정의, 사용자, MVP 범위, 기능 요구사항, 성공 지표 포함

### M2. Risk and Policy Scan
- 목적: 데이터, 규제, 개인정보, 플랫폼 리스크 정리
- 입력: `docs/20-product/strategy/prd-v0.2.md`
- 출력: `docs/20-product/research/risk-register.md`
- 주 담당: `Gemini`
- 대체 담당: `Codex CLI`
- 완료 조건: 리스크 항목, 영향도, 대응 방향 분리

### M3. Data Model Draft
- 목적: 핵심 엔티티와 관계를 ERD 문서로 정리
- 입력: `docs/20-product/strategy/prd-v0.2.md`
- 출력: `docs/20-product/data/erd-v0.1.md`
- 주 담당: `Gemini`
- 대체 담당: `Codex CLI`
- 완료 조건: 엔티티, 필드 초안, 관계, 미결정 이슈 포함

### M4. User Flow Design
- 목적: 핵심 사용자 흐름 정의
- 입력: `docs/20-product/strategy/prd-v0.2.md`
- 출력: `docs/20-product/design/ux/user-flows.md`
- 주 담당: `Claude`
- 대체 담당: `Gemini`
- 완료 조건: 주요 사용자 타입별 핵심 플로우 정의

### M5. Screen Specification
- 목적: 화면 목록과 상태별 요구사항 정의
- 입력: `docs/20-product/design/ux/user-flows.md`
- 출력: `docs/20-product/design/ux/screen-specs.md`
- 주 담당: `Claude`
- 대체 담당: `Codex CLI`
- 완료 조건: 화면 목표, 액션, 상태, 컴포넌트 요구사항 포함

### M6. Copy Guide
- 목적: 톤 앤 매너와 화면별 핵심 문구 정리
- 입력: `docs/20-product/strategy/prd-v0.2.md`
- 출력: `docs/20-product/design/ux/copy-guide.md`
- 주 담당: `Claude`
- 대체 담당: `Gemini`
- 완료 조건: 랜딩, 온보딩, 빈 상태, 오류 상태 문구 포함

### M7. Repo Bootstrap
- 목적: 앱 기본 구조 초기화
- 입력: `docs/20-product/strategy/prd-v0.2.md`, `docs/20-product/design/ux/screen-specs.md`
- 출력: 코드베이스 초기 구조
- 주 담당: `Codex CLI`
- 대체 담당: `Codex 앱`
- 완료 조건: 기본 앱 실행 가능, lint/test 기준 정리

### M8. Auth Module
- 목적: 인증 플로우 구현
- 입력: `docs/20-product/strategy/prd-v0.2.md`, `docs/20-product/design/ux/user-flows.md`, `docs/20-product/data/erd-v0.1.md`
- 출력: 인증 관련 코드
- 주 담당: `Codex CLI`
- 대체 담당: `Codex 앱`
- 완료 조건: 로그인/세션/오류 처리 흐름 구현

### M9. Playlist Capture Module
- 목적: 플레이리스트 생성/기록 UI와 API 구현
- 입력: `docs/20-product/design/ux/screen-specs.md`, `docs/20-product/data/erd-v0.1.md`
- 출력: 생성 플로우 코드
- 주 담당: `Codex CLI`
- 대체 담당: `Codex 앱`
- 완료 조건: 입력, 저장, 오류 처리, 기본 검증 동작

### M10. Proof Metadata Module
- 목적: 인간 원본 증명 메타데이터 수집 구조 구현
- 입력: `docs/20-product/strategy/prd-v0.2.md`, `docs/20-product/data/erd-v0.1.md`
- 출력: 메타데이터 구조 및 관련 코드
- 주 담당: `Codex CLI`
- 대체 담당: `Codex 앱`
- 완료 조건: 오프체인 메타데이터 저장 구조와 관련 문서 반영

### M11. Review and Regression
- 목적: 구현 결과 리뷰와 회귀 점검
- 입력: 관련 PR 또는 diff
- 출력: 리뷰 메모, 수정 요청
- 주 담당: `Codex 앱`
- 대체 담당: `Codex CLI`
- 완료 조건: 버그, 누락, 테스트 공백 식별

### M12. Daily Triage
- 목적: 오픈 이슈, 문서 미반영, 테스트 실패를 점검
- 입력: 저장소 상태, 이슈 목록
- 출력: `docs/20-product/delivery/implementation/open-issues.md`
- 주 담당: `Codex 앱`
- 대체 담당: `Codex CLI`
- 완료 조건: 우선순위와 다음 액션이 정리됨

---

## 대체 규칙
- 주 담당이 응답 중단, 토큰 한도, 품질 저하 상태이면 대체 담당으로 즉시 전환한다.
- 대체 담당은 같은 입력 문서와 같은 출력 경로를 사용한다.
- 출력 형식을 바꾸지 않는다.
- 인수인계 시 새 판단보다 기존 문서 기준을 우선한다.

---

## 비상 운영 순서
1. 중단된 모듈 번호를 확인한다.
2. 마지막 정상 산출물 경로를 확인한다.
3. 같은 모듈의 대체 담당에게 그대로 넘긴다.
4. 완료 후 변경된 판단만 `docs/20-product/delivery/implementation/decision-log.md`에 기록한다.
