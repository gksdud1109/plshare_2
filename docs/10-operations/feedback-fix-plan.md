# 피드백 수정 계획 (검증 완료 / 실행 대기)

> 2026-06-15. 사용자 피드백 10건을 **러닝 스택 curl + 파일:줄 직접 검증**으로 재확인(다관점 워크플로 + 메인 직접 교차검증). 실행은 사용자 지시로 **보류** — 이 문서는 확정된 계획.

## 분류 요약
- **확정 버그 4**: item1(랭킹 클릭 403), item7(무드영상 데드엔드), item10(import 상태계약), item9(compose 멱등성)
- **기능/UX 5**: item8(선택기 미리보기), item4(타인 목록·용어), item2(DM 선물), item5(YT 검색/import), item6(제작/선물 분리)
- **정상(by_design) 1**: item3(피드 텍스트 포스트는 시더 의도)

## 핵심 통찰 — 한 뿌리 + 두 선행작업
item1·4·7·8은 **"자산을 소유자/선물 밖에서 읽고 재생하는 표면이 덜 지어졌다"**는 한 뿌리. 두 횡단 선행작업이 여럿을 동시에 푼다:
- **(A) FE `types/asset.ts` 필드 패리티** — `assetKind`/`moodVideoId`/`moodChannelName`/`moodTrackListText` 추가, 잘못 든 `hasEmotionalContext` 제거. → item7·item8 선행조건.
- **(B) BE 공개 asset read 계약** — 비소유자도 읽는 읽기전용 엔드포인트(사적필드 선별). → item1 해결 + item4 전제.

## 용어 결정 (확정)
- 목록/생성/관리 맥락 → **'플레이리스트'** ('나의 플레이리스트', '플레이리스트 선택')
- 공유/선물의 정서적 맥락 → 기존 **'취향'** 유지 ('취향 카드')
- MOOD_VIDEO(트랙0 단일영상)는 카드/배지에서 **'영상'** 으로 개별 표기
- **도메인 코드(Asset/AssetKind/라우트 `/assets`)는 불변 — 한글 카피만 치환**(사용자노출 ~39회)

## 확정 버그 상세

### item1 — 랭킹/피드 카드 클릭 시 403 "자산을 불러오지 못했어요" · P1 · M
- **증거**: 랭킹 API는 200 정상. `PlaylistRankCard.tsx:32` → `/assets/{id}` → `AssetController.kt:42` `requireOwner`. rank1(jimin 자산)→**HTTP 403 FORBIDDEN**, rank4(demo 자산)→200. 공개 read는 `/share/{token}`뿐, 공개 by-id 없음.
- **근본**: 랭킹/피드(전체 공개 목록) vs 자산 상세(소유자 전용 읽기)의 **권한 모델 불일치**. 단발 버그 아님 — 타인 자산 상세로 가는 모든 경로(피드 PostCard 임베드 등)에서 재현.
- **처방**: BE 공개 read by-id 엔드포인트(예 `GET /api/public/assets/{id}`, `diaryText`/`photoUrls` 등 사적필드 선별 제외) + FE 카드 라우팅/403 폴백. **`requireOwner` 단순 제거 금지**(편집·공유 보안 붕괴).

### item7 — MOOD_VIDEO 듀얼포맷 미배선 (데드엔드) · P1 · L
- **증거**: MOOD_VIDEO가 **선물 언박싱 1화면에만** 배선. `types/asset.ts`의 AssetDetail/SharedAsset/AssetSummary에 `assetKind`/mood* **누락**. 자산상세(`0곡` 빈 화면)·공유(`ShareTrackList`는 tracks.map만)·카드(`0 tracks`)·gift선택기(`0곡`) 전부 깨짐. **export**: `ExportService` runApple:148/runYouTube:235가 `asset.tracks`(0개)만 순회 → **빈 플레이리스트를 'completed'로 생성(거짓 성공)** — 가장 해로움.
- **처방(3단)**: ① **export 가드**(MOOD_VIDEO면 빈 성공 차단 → 명시 실패 또는 자산상세에서 **export 버튼 숨김+안내**가 더 정직). ② FE 타입 패리티(선행 A). ③ **`MoodVideoPlayer`를 `UnboxingView.tsx:182-230` 검증된 임베드 블록에서 추출**(신규작성 금지·단일출처)해 자산상세·공유에서 `assetKind` 분기.

### item10 — import 상태계약 불일치 · P1 · S
- **증거**: BE는 `RUNNING→"running"` 송출, FE 유니온은 `queued|matching|completed|failed`(running 없음), 데모 픽스처는 또 `matching`. **happy-path는 정상 완료→library 생성**(무한로딩 **재현 안 됨**, mock이 sub-second). 진짜 결함 = running 구간 '실패' 오라벨 + 잠재 무한폴링.
- **처방**: 상태 어휘 한쪽 통일(BE `ImportDtos.kt`에서 `RUNNING→"matching"` 매핑) + `progress/page.tsx` statusText running/matching 케이스 + 폴링 타임아웃 시 'library에서 계속' 안내. (외부/미지 플레이리스트 실패는 MockSpotifyClient 화이트리스트 한계 — 코드버그 아님.)

### item9 — compose 멱등성 부재 · P2 · M
- **증거**: 동일 입력 2회 → 다른 id 둘 다 200. CatalogService에 dedup 0건. **제목 미지정 한정 아님**(동일제목 직접입력도 중복).
- **처방**: **제목중복 금지 ❌**(정당한 동명 차단 부작용). 클라이언트 멱등키(X-Idempotency-Key) 더블서브밋 차단 또는 자동번호. item6 '자산 생성 UX'와 묶어 처리.

## 기능 / UX

| 항목 | 처방 | 비고 |
|---|---|---|
| item8 선택기 미리보기 P1·M | 1차: summary DTO에 `assetKind` 배지+상위N곡 문자열(S) → 2차: 아코디언 lazy `getAsset`(M) | FE 타입 패리티 공유 |
| item4a 타인 목록 | **제품결정 선행**. 진행 시 `GET /api/users/{handle}/assets`(공개=shareToken 보유분) | 주석 "ownerId 없어서"는 거짓 |
| item4b 용어 | 위 용어 결정대로 한글 카피 치환 | 독립·저위험 |
| item2 DM 선물 P2 | **경량판**: gift/send 완료뷰 '쪽지로 보내기' + 스레드의 `/gift/{token}` 평문 → 읽기전용 카드 자동 임베드. **Message.giftToken 컬럼 추가는 과투자(L)** | 수신자 모델 결정 필요 |
| item5a YT 검색→곡추가 | 엔진(`searchVideoCandidates`) **이미 존재** → `GET /api/youtube/search` 노출(쿼터 reserve+디바운스+캐시) + 검색바. compose 계약을 비카탈로그 videoId로 확장 | 검색은 보조(큐레이션 정체성 보호) |
| item6 제작/선물 분리 | **트랜잭션 묶기 ❌**(BE 이미 분리). create 자동 `router.push` 제거 + 두갈래 CTA(S) + `DELETE /api/assets/{id}`(Gift 약참조 보호)(M) | '원자성'은 오답 처방 |

## 실행 순서 (작은 PR, 의존성)
1. **PR1 (S)** 용어 교체 — 독립·즉시
2. **PR2 (S)** FE 타입 패리티 — 횡단 선행(PR6·PR8 전제)
3. **PR3 (S)** export 가드 — 거짓 성공 차단(최우선), BE 단독
4. **PR4 (S)** import 상태 어휘 통일
5. **PR5 (M)** 공개 asset read + 카드 라우팅 (권한경계 신중, item4 전제)
6. **PR6 (L)** MoodVideoPlayer 추출 → 자산상세·공유 재생
7. **PR7 (S→M)** create 자동이동 제거+CTA / DELETE 자산 (+item9 멱등키)
8. **PR8 (M)** 선물 선택기 미리보기

→ **PR1~4는 전부 S·저위험·고ROI 첫 묶음.**

## 제품 결정 필요 (코드 아님 — 실행 전 확정)
1. **프로필 = 공개 큐레이션 공간?** (현 모델은 shareToken/gift 사적 전달) — item4a·item1 권한모델 공유. 결정 전엔 '준비 중' 탭 **임시 숨김**이 정직.
2. **DM 선물 수신자 모델** — 현 `dedicationTo`는 자유텍스트라 handle 아님. handle picker 도입 여부.
3. **YT Music 플레이리스트 import(item5b)** — Google OAuth 의존 → 토스 인앱 제약 충돌 + 데모도 grant 미시드로 깨짐 → **보류 권고**.

## 조치 불필요
- **item3**: by_design. 임베드 체인 정상, 자산 없는 포스트는 시더 의도(텍스트 온보딩 1건). (백로그: `getPostAssetEmbed` N+1+silent null 견고화.)
