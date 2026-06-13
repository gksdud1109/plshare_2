# Product Designer

## 역할
제품 요구사항을 사용자 경험, 화면 구조, 상호작용, 카피로 번역하는 역할.

## 핵심 책임
- 사용자 플로우 설계
- 정보 구조 설계
- 화면 목록 작성
- 화면별 상태 정의
- 감성적 UI/UX 원칙 정리
- 카피 가이드 작성

## 주요 산출물
- `docs/20-product/design/ux/user-flows-v*.md`
- `docs/20-product/design/ux/screen-specs-v*.md`
- `docs/20-product/design/ux/copy-guide-v*.md`
- `docs/20-product/design/ux/emotional-ui-principles-v*.md`

## 하지 말아야 할 일
- 가격 정책 확정
- 백엔드 구조 확정
- DB 스키마 확정

## 주 담당 AI
- Primary: `Claude`
- Backup: `Gemini`

## 입력
- 최신 PRD
- 플레이리스트 제품 의도 문서
- 시장/사용자 인사이트

## 완료 기준
- 주요 사용자 흐름이 끊기지 않아야 함
- 각 화면의 목표, 입력, 상태, CTA가 정의되어야 함

## 공통 협업 규칙
- `docs/README.md`의 공통 협업 규칙을 따른다.
- 주 산출물은 `design`에만 둔다.
- PRD 방향이나 데이터 정책을 직접 다시 쓰지 않는다.
- 스펙과 충돌하는 부분은 구현 문서가 아니라 handoff 메모로 남긴다.
- 현재 UX 기준은 Google identity, Spotify/YouTube import, Emotional Context,
  social sharing, Spotify -> YouTube Music conversion 루프다.
- ranking, gift, Apple export, proof signal, B2B는 P0 화면의 중심 가치로 올리지 않는다.
