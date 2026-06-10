# Briefing — pd-social-ux-research-001

## Role
Product Designer (UX 리서치 + 플로우 설계, Nocturne 디자인시스템 위)

## Trigger (CEO 중간점검 지시, 2026-06-10)
1. "사용자 관점에서 **딸깍**으로 플레이리스트 전환할 수 있는 직관성 보완 필요" — 현재 가져오기→자산화→내보내기 플로우는 단계가 많다.
2. 소셜 레이어 추가: 랭킹 / 감성 선물 / SNS성 포스팅(글+음악) / 좋아요·댓글·DM·피드.

## 리서치 대상 (벤치마크)

### A. 원클릭 전환 UX
- TuneMyMusic, Soundiiz, FreeYourMusic, SongShift: 링크 붙여넣기→목적지 선택→완료까지 몇 단계인가, 인증 마찰을 어디서 흡수하나, 매칭 실패를 어떻게 보여주나.
- 우리 목표 플로우 제안: **"플레이리스트 링크 붙여넣기 → 목적지 픽 → 변환"** 3-스텝. 자산화(Emotional Context)는 변환 후 선택 단계로 미루는 안 vs 끼워넣는 안 비교.

### B. 음악×소셜 피드
- SoundCloud(파형 위 댓글), Airbuds(친구 청취 피드), Spotify Blend/Jam, Instagram Notes 음악, X 음악 카드.
- "포스트 = 글 + 플레이리스트/트랙 첨부"의 카드 해부도(anatomy): 커버/제목/발췌/재생 프리뷰/감정태그.

### C. 감성 선물
- 카카오톡 선물하기(감성 포장·메시지 카드), Spotify Wrapped 공유 카드, 손편지/언박싱 메타포.
- 보내기(스토리 작성→포장 선택→전송) / 받기(알림→언박싱 연출→내 라이브러리 저장) 플로우 초안.

### D. 랭킹
- Spotify Charts, Melon Top100, Product Hunt(투표), 왓챠피디아(취향). 랭킹이 "차트"가 아니라 "발견 피드"처럼 느껴지게 하는 패턴.

## 산출물 (파일 2개)
1. `docs/20-product/design/ux/social-ux-research-v0.1.md`
   - 벤치마크별 핵심 패턴 (스텝 수/마찰 지점/차용할 것·피할 것)
   - 원클릭 전환 타깃 플로우 (단계 다이어그램, 현행 대비 비교)
   - 포스트 카드 anatomy, 선물 플로우, 랭킹 패턴 권고
2. `docs/20-product/design/ux/user-flows-v0.2-social.md`
   - Flow E: 원클릭 변환 (붙여넣기→목적지→진행→완료)
   - Flow F: 피드 (작성: 글+자산 첨부 / 소비: 좋아요·댓글)
   - Flow G: 선물 (보내기/받기/언박싱)
   - Flow H: 랭킹 탐색 → 자산 상세 → 변환/저장 전환
   - 신규 라우트 인벤토리 제안 (`/feed`, `/p/[postId]`, `/gift/...`, `/ranking`, `/u/[handle]` 등) — 기존 9 라우트와 충돌 없게
   - 각 화면 empty/loading/failure/success 상태 정의

## 제약
- 디자인 토큰은 Nocturne(`docs/20-product/design/ux/design-system-v0.2-dark.md`) 유지 — 새 테마 만들지 말 것.
- 기존 9개 라우트 URL·E2E 플로우를 깨지 않는 **추가** 구조로 제안.
- 모바일 우선 고려 (SNS 소비 맥락).
- 코드 작성 금지 — 문서만.
