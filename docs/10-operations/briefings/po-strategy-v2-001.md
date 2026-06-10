# Briefing — po-strategy-v2-001

## Role
Product Owner (전략 재검토 + 소셜 레이어 PRD 리서치)

## Trigger (CEO 중간점검 지시, 2026-06-10)
1. **플랫폼 우선순위 재검토**: "YouTube Music ↔ Spotify, YouTube Music ↔ Apple Music이 인지도 상 우선순위가 높았을 것 같은데 미구현이다." — 기존 PO 결정(`docs/20-product/strategy/platform-direction-review-v0.1.md`, Apple 유지·YTM=P2)을 **새 근거 하에 재검토**하라. 이전 결정을 변호하지 말 것. 무엇이 달라졌는지(아래 3번 SNS 방향 전환 포함)를 반영해 다시 평가하라.
2. **직관성**: 사용자 관점 "딸깍" 한 번으로 플레이리스트가 옮겨지는 경험이 핵심. 현재 플로우(가져오기→자산화→내보내기)는 단계가 많다.
3. **제품 방향 확장 — 소셜 레이어** (최초 기획 복원):
   - 플레이리스트 랭킹 / 사용자 랭킹
   - 플레이리스트에 사용자의 스토리를 담는 **"감성적인 선물"** 기능
   - 플레이리스트/음악과 연계해 자기 글을 올리는 SNS성 포스팅 (X, Instagram, SoundCloud 참고)
   - 좋아요 / 댓글 / DM / 피드

## 답해야 할 질문

### A. 플랫폼 코리도 우선순위 (재검토)
- 코리도 매트릭스: Spotify↔Apple(현행) / Spotify↔YTM / YTM↔Apple — 각각 (1)사용자 수요·인지도 (2)API 실현가능성 (3)매칭 품질 리스크 (4)정책 리스크로 점수화하고 순위를 매겨라.
- **YouTube Data API v3 수치 검증 필수**: 기본 쿼터 10,000 units/day, `playlistItems.insert` = 50 units로 알려져 있다 → 30곡 플레이리스트 **내보내기 1건 ≈ 1,500+ units → 하루 약 6건** 수준. 이 수치가 맞는지, 쿼터 증설 절차/조건, read(`playlists.list`, `playlistItems.list` = 1 unit)는 저렴한지 확인하라. **"읽기 먼저(YTM import), 쓰기는 쿼터 증설 후(YTM export)"** 단계 전략이 타당한지 평가하라.
- videoId↔ISRC 매칭: 우리 MatchingEngine(ISRC primary + fuzzy 0.5/0.3/0.2)으로 YTM 트랙 정규화가 가능한지, videoId 특유 리스크(live/cover/fan upload)의 완화책.
- 비공식 ytmusicapi 류 사용 리스크 평가 (정책/차단).

### B. 인증·계정 모델
- 소셜 기능은 실제 User 계정이 전제다. 소셜로그인 우선순위: **Google(=YouTube 권한과 시너지)** / Apple / 카카오 — 무엇부터?
- Google OAuth 한 번으로 (1)소셜로그인 (2)YouTube 라이브러리 접근을 같이 얻는 설계가 가능한지(scope 분리, 단계적 동의).

### C. 소셜 레이어 PRD 방향
- **포스트**란 무엇인가: 텍스트+플레이리스트/트랙 첨부? 글자수? 이미지?
- **랭킹**: 무엇을(플레이리스트/사용자), 어떤 지표로(좋아요/저장/전환수/팔로워), 어떤 주기로.
- **감성 선물**: 플로우(보내기→수신→언박싱), 기존 BM 가설("PA 선물하기 수수료")과의 연결, Emotional Context(일기/사진/태그)와의 결합.
- **MVP 컷**: 좋아요+댓글은 MVP-in, DM은 나중? 피드 정렬(팔로잉/추천)? 근거와 함께 제안.
- KPI 가설 갱신: K-factor, 피드 리텐션, 전환(딸깍 변환 완료율).

### D. 우선순위 산출물
- P0/P1/P2 재배치 표 + 3단계 로드맵(Identity → Corridor → Social) 권고.

## 산출물 (마크다운 한 문서, 섹션 구분)
1. Executive Summary (결정 권고 5줄)
2. 코리도 재검토 결론 (v0.1 대비 무엇이 바뀌었고 왜)
3. YouTube API 실현가능성 (쿼터 수치 포함)
4. 인증·계정 전략
5. 소셜 레이어 PRD 초안 (포스트/랭킹/선물/좋아요·댓글·DM 정의)
6. 우선순위 표 + 로드맵 권고
7. Freeze (확정 제안 사항)

## Do Not
- 이전 결정(v0.1) 문서를 부정하기 위한 재서술 금지 — 근거 변화 중심으로.
- 코드/구현 디테일 금지 (전략·PRD 레벨).
- Emotional Context 자산화 테제는 유지 — 소셜 레이어는 이를 증폭하는 방향.
