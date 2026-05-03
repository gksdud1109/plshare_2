# Screen Specs v0.1

## 문서 정보
- Date: `2026-05-03`
- Role: `Product Designer`
- Model: `Claude`
- Version: `v0.1`
- Based on:
  - `docs/20-product/design/ux/user-flows-v0.1.md`
  - `docs/20-product/design/ux/design-direction-v0.1.md`
  - `docs/20-product/requirements/functional-requirements-v0.2.md`
- Scope: `MVP screen inventory, per-screen states, component inventory, interaction patterns`
- Target Stack: `Next.js 14+ App Router`

---

## 0. 공통 규약

- **상태 4종 표기:** 모든 화면은 `empty / loading / failure / success` 4가지 상태를 정의한다. 일부 화면(예: 진행 화면)은 의미상 변형되지만, 4종은 빠지지 않는다.
- **레이아웃 grid:** 12-column, max-width 1180px (라이브러리/상세). 감상 페이지(`/share/[token]`)는 본문 720px 중심.
- **Typography:** 본문은 serif/sans 혼용 가능. 제목은 표지처럼 큰 weight, 일기 본문은 읽기 호흡 우선.
- **컬러:** Muted tones (design-direction-v0.1 §3). 강조색은 단일 accent.
- **State 배지 컬러:**
  - exact / high-confidence: neutral
  - low-confidence: amber
  - unmatched: muted red
- **카피 톤:** "자산", "보존", "옮겨오다" 등 자산화 어휘를 우선. "재생", "스트리밍" 등 재생 중심 어휘는 외부 앱 연결 CTA에서만 사용.

---

## 1. 화면 인벤토리 (Route Map)

| # | 라우트 | 목적 | 인증 | 핵심 FR |
|:---|:---|:---|:---|:---|
| 1 | `/` | 랜딩, 가치 제안, 진입 | 비로그인 가능 | — |
| 2 | `/auth/spotify` | Spotify OAuth 진입 / 콜백 | 로그인 필요 | FR-003 |
| 3 | `/import` | Spotify 플레이리스트 선택 | 로그인 + Spotify 연결 | FR-010, FR-011 |
| 4 | `/import/[playlistId]/progress` | 임포트/정규화 진행 | 로그인 | FR-012, FR-020~FR-022 |
| 5 | `/assets` | 내 자산 라이브러리 | 로그인 | FR-060 |
| 6 | `/assets/[id]` | PA 상세 + Emotional Context 입력 | 로그인 (작성자) | FR-030~FR-042, FR-061 |
| 7 | `/assets/[id]/export` | Apple Music 익스포트 매핑 확인 | 로그인 + Apple 연결 | FR-070~FR-072, FR-074 |
| 8 | `/assets/[id]/export/result` | 익스포트 결과 | 로그인 | FR-073 |
| 9 | `/share/[token]` | 공유 감상 페이지 | 비로그인 가능 | FR-062, FR-063 |

### 1.1 보조 라우트 (참고)
- `/auth/login`, `/auth/logout` — FR-001 (스펙 본문 외 단순 화면)
- `/settings` — 연결 관리, 탈퇴 (FR-002, FR-003) — 본 문서 §2에서는 제외

---

## 2. 화면별 상세 명세

각 화면은 **목적 / 진입 경로 / 주요 컴포넌트 / 핵심 인터랙션 / 4가지 상태**로 구성한다.

---

### 2.1 `/` 랜딩

**목적:** "취향 자산 매니지먼트 레이어"라는 제품 명제를 한 화면 안에 전달하고, Spotify 연결로 유도한다.

**진입 경로:** 외부 유입, 도메인 직접 접근, 로그아웃 후

**주요 컴포넌트:**
- `HeroStatement` — 단일 카피 ("당신의 플레이리스트를 자산으로 옮겨오세요")
- `PrimaryCTA` — "Spotify로 시작하기"
- `AssetCardPreview` — 자산이 어떻게 보이는지 정적 미리보기 (1~2개)
- `Footer` — 정책, 운영자 연락처

**핵심 인터랙션:**
- 스크롤은 최소화. Above the fold에서 가치+CTA 완결.
- CTA 클릭 시 미인증 사용자는 가입/로그인 → `/auth/spotify`로 이어짐.

**상태:**
- **empty:** 로그아웃 상태 기본 화면. 별도 빈 데이터 처리 없음.
- **loading:** 정적 페이지이므로 페이지 페이드 인만. 별도 스켈레톤 불필요.
- **failure:** 페이지 자체 로드 실패는 글로벌 error boundary 처리.
- **success:** 로그인 사용자가 진입했을 때 CTA가 "내 라이브러리로"로 변경.

---

### 2.2 `/auth/spotify` Spotify 인증 진입

**목적:** Spotify OAuth 시작 / 콜백 처리.

**진입 경로:** `/`의 CTA, `/import`의 미연결 가이드, `/assets/[id]`의 재인증 안내

**주요 컴포넌트:**
- `ConnectionStatusCard` — "Spotify에 연결합니다" 안내, 권한 범위 명시 (읽기 전용)
- `PrimaryCTA` — "Spotify로 인증하기"
- `SecondaryCTA` — "취소"

**핵심 인터랙션:**
- 클릭 시 Spotify OAuth 페이지로 redirect.
- 콜백 수신 후 `Streaming Connection` 생성 → `/import`로 자동 이동.

**상태:**
- **empty:** 미연결 상태. 안내 카드와 CTA 표시.
- **loading:** OAuth redirect 직후 "Spotify에서 돌아오는 중…" 짧은 대기 화면.
- **failure:** `auth_failed`, `user_denied`, `network_error` 사유별 카피 분리. 재시도 CTA + "다른 방법으로 시작하기" 보조 옵션.
- **success:** 짧은 확인 ("연결되었어요") 후 자동 라우팅. 머물지 않음.

---

### 2.3 `/import` Spotify 플레이리스트 선택

**목적:** 사용자의 Spotify 플레이리스트 목록을 보여주고 임포트 대상을 선택하게 한다.

**진입 경로:** `/auth/spotify` 성공 직후, `/assets`의 "새로 가져오기"

**주요 컴포넌트:**
- `PlaylistGrid` — Spotify 플레이리스트 카드 그리드
  - 각 카드: 커버, 제목, 트랙 수, 마지막 수정일
- `URLImportInput` — 보조 입력 ("URL로 가져오기")
- `ConnectionPill` — 우상단 "Spotify 연결됨" 상태 표시

**핵심 인터랙션:**
- 카드 클릭 → 즉시 `/import/[playlistId]/progress`로 이동, 백그라운드 임포트 시작.
- URL 입력 → validate → 동일하게 progress 페이지로 이동.
- 다중 선택 없음 (MVP).

**상태:**
- **empty:** Spotify 계정에 플레이리스트가 0개. "Spotify에 플레이리스트가 없어요" 안내 + "URL로 가져오기" 보조.
- **loading:** 그리드 자리 8개 카드 스켈레톤. Refined 톤 — shimmer 대신 정적 페이드.
- **failure:**
  - 토큰 만료 → 재연결 카드
  - API 응답 실패 → "Spotify에서 데이터를 받지 못했어요" + 재시도
- **success:** 플레이리스트 카드 그리드 정상 렌더링.

---

### 2.4 `/import/[playlistId]/progress` 임포트/정규화 진행

**목적:** 임포트 + 정규화의 비동기 작업 진행 상태를 보여주고, 완료 시 PA 초안으로 전환한다.

**진입 경로:** `/import`의 카드/URL 클릭

**주요 컴포넌트:**
- `ProgressNarrative` — 3-stage 진행 표시
  1. 곡 정보를 불러오는 중
  2. ISRC로 정규화하는 중
  3. 매칭 신뢰도를 분류하는 중
- `TrackPreviewList` — 가져온 트랙이 점진적으로 추가되는 리스트 (옵션, 시각적 변화로 진행감 부여)
- `CancelLink` — "그만두기" (보조)

**핵심 인터랙션:**
- 백엔드 작업 상태를 polling 또는 SSE로 추적.
- 완료 시 PA 초안이 생성되며 자동으로 `/assets/[id]`로 라우팅 (FR-031).
- 부분 실패는 작업 종료 처리 — 완료 화면으로 진행 (FR-012).

**상태:**
- **empty:** 진행 화면은 본질적으로 비어있지 않으므로 별도 없음.
- **loading:** 기본 상태. 단계별 narrative + 부드러운 페이드.
- **failure:**
  - `auth_expired` → 재인증 안내
  - `playlist_not_found` → "찾을 수 없어요" + 라이브러리로 이동
  - `quota_exceeded` → "잠시 후 다시 시도해 주세요" + 자동 재시도 카운터
  - `network_error` → 재시도 CTA
- **success:** "OOO개 곡을 자산으로 옮겼어요" 짧은 완료 표시 후 자동 라우팅. 누락 곡 수가 있으면 카운트 함께 표시.

---

### 2.5 `/assets` 내 자산 라이브러리

**목적:** 사용자가 만든 모든 PA를 자산처럼 진열한다 (단순 리스트 지양).

**진입 경로:** 글로벌 nav, 로그인 직후, 익스포트 결과의 "라이브러리로"

**주요 컴포넌트:**
- `AssetCardGrid` — PA를 표지가 있는 카드로 진열
  - 각 카드: 커버, 제목, 짧은 메모 1줄, 공개 상태 배지, 마지막 수정 시각, 소스 플랫폼 아이콘
- `EmptyState` — 자산이 0개일 때
- `NewImportCTA` — 우상단 "Spotify에서 가져오기"
- `FilterBar` — 공개 상태 / 소스 / 정렬 (MVP에서는 정렬만 — 최신순)

**핵심 인터랙션:**
- 카드 클릭 → `/assets/[id]`
- 카드 hover/long-press → 보조 액션 메뉴 (공유 링크 복사, 보관)
- 새로고침은 자동 (mount 시), 명시적 pull-to-refresh 없음.

**상태:**
- **empty:** "아직 자산이 없어요. 첫 플레이리스트를 가져와 보세요" + `NewImportCTA` 강조.
- **loading:** 카드 스켈레톤 6개.
- **failure:** "라이브러리를 불러오지 못했어요" + 재시도. 캐시된 마지막 목록은 흐릿하게 유지.
- **success:** PA 카드 그리드.

---

### 2.6 `/assets/[id]` PA 상세 (Emotional Context 입력 포함)

**목적:** 자산의 모든 정보를 보여주고, Emotional Context를 작성/편집하며, export·share·외부 앱 열기 동선의 허브.

**진입 경로:** `/import/[playlistId]/progress` 완료, `/assets` 카드 클릭, 알림

**주요 컴포넌트:**
- `AssetCoverHeader` — 커버 이미지 (대비중), 제목, 공개 상태 토글, 작성자
- `EmotionalContextEditor`
  - 제목 / 설명 입력
  - 일기 영역 (multi-line, 자동 저장)
  - `EmotionTagPicker` (큐레이션 + 자유 입력)
  - `PhotoAttachment` (커버와 별도 감성 이미지)
- `TrackListPanel`
  - 각 행: `TrackRow` + `MatchConfidenceBadge`
  - 매칭 실패 트랙 분리 섹션 ("해결 필요")
  - `AlternativeTrackPicker` (실패 트랙 클릭 시 전개)
- `ActionRail` — 우측 또는 하단 고정
  - "Apple Music으로 내보내기"
  - "공유 링크 만들기" (public일 때만 활성)
  - "외부 앱에서 열기" (Spotify / Apple)
- `AutoSaveIndicator` — 상단에 미세하게

**핵심 인터랙션:**
- 모든 텍스트 입력은 debounce 자동 저장. 명시적 "저장" 버튼 없음 — 단, 공개 전환은 명시적 클릭 필요.
- 매칭 실패 트랙 클릭 → 후보 리스트 inline 전개 → 선택 시 즉시 반영.
- 트랙 순서 조정은 MVP에서 제외 (FR-032의 일부는 P1으로 미룸 — 본 라운드에서는 "트랙 제외"만).
- 공개 상태 토글은 변경 시 확인 모달 ("공개로 전환하면 누구나 링크로 볼 수 있어요").

**상태:**
- **empty:** 자산이 막 생성된 직후. 일기 영역에 placeholder ("오늘 이 곡들과 어떤 시간을 보냈나요?"), 태그 미선택, 사진 미첨부.
- **loading:** 자산 데이터 fetch 중. 헤더 스켈레톤 + 트랙 리스트 스켈레톤.
- **failure:**
  - 자산 로드 실패 → "이 자산을 불러오지 못했어요" + 라이브러리로 이동
  - 자동 저장 실패 → 상단 배너 "저장이 일시적으로 실패했어요" + 수동 저장 CTA
  - 이미지 업로드 실패 → 인라인 토스트, 텍스트는 보존
- **success:** 모든 컴포넌트 정상 렌더링, 자동 저장 동작 정상, "방금 저장됨" 미세 표시.

---

### 2.7 `/assets/[id]/export` Apple Music 익스포트 매핑 확인

**목적:** export 실행 직전 사용자가 무엇이 어떻게 전송되는지 확인하고 결정하게 한다.

**진입 경로:** `/assets/[id]`의 "Apple Music으로 내보내기" CTA

**주요 컴포넌트:**
- `ExportSummaryCard`
  - 전송 예정 곡 수
  - 누락 예정 곡 수 + 사유 분포 (매칭 실패 / 사용자 제외)
- `TargetPlaylistEditor` — Apple Music에 생성될 제목/설명 미리보기 (편집 가능)
- `ExportHistoryNote` — 직전 export 이력 (있을 때만, FR-074)
- `PrimaryCTA` — "내보내기 시작"
- `AppleConnectionGuard` — Apple 미연결 시 인증 모달 트리거

**핵심 인터랙션:**
- Apple 미연결 상태에서는 본문이 흐릿하게 처리되고 인증 모달이 우선 노출.
- 직전 export 이력이 있으면 "재시도"와 "새로 내보내기"를 시각적으로 구분 (FR-074).
- 누락 곡 수 클릭 시 누락 사유 상세를 inline 전개.

**상태:**
- **empty:** 자산에 매칭된 트랙이 0개. "내보낼 수 있는 곡이 없어요" + 매칭 해결 안내 → `/assets/[id]`로 돌아가기.
- **loading:** 매핑 미리보기 계산 중. 요약 카드 스켈레톤.
- **failure:**
  - Apple 인증 만료 → 재인증 모달
  - Apple 미구독 계정 감지 → "이 계정에서는 라이브러리에 쓸 수 없어요" + 차단
  - 매핑 계산 실패 → 재시도 CTA
- **success:** 요약 카드 + CTA 활성. 클릭 시 `/assets/[id]/export/result`로 이동.

---

### 2.8 `/assets/[id]/export/result` 익스포트 결과

**목적:** export 작업의 최종 상태를 보여주고 다음 행동(외부 앱 열기, 라이브러리로 돌아가기, 재시도)으로 연결한다.

**진입 경로:** `/assets/[id]/export`의 "내보내기 시작" 후, 알림에서 진입

**주요 컴포넌트:**
- `ResultHero` — 상태 아이콘 + 결과 카피
- `TransferStats` — 성공 곡 수 / 누락 곡 수
- `MissingTrackList` — 누락된 곡과 사유 (`partially completed` 또는 `failed`일 때)
- `OpenInAppleCTA` — "Apple Music에서 열기" 딥링크
- `BackToAssetCTA` — "자산으로 돌아가기"
- `RetryCTA` — `failed` 또는 `partially completed`일 때

**핵심 인터랙션:**
- 결과 상태에 따라 hero 카피와 액션 우선순위가 달라진다.
- 재시도는 새 `Export Job`을 생성하지 않고 기존 job의 누락분만 재처리.

**상태 (export job status에 매핑):**
- **empty:** 의미 없음 — 결과 페이지는 항상 데이터를 가짐.
- **loading:** export job 상태가 `queued` 또는 `running`. 결과 페이지에 도달하기 전에는 진행 인디케이터만 노출.
- **failure (failed):** "내보내기에 실패했어요" + 사유 + 재시도. 자산은 영향 없음을 명시.
- **success:**
  - `completed`: "Apple Music으로 모두 옮겨졌어요" + Open in Apple
  - `partially completed`: "대부분 옮겨졌어요. 일부 곡은 누락됐어요" + 누락 목록 + Open in Apple

---

### 2.9 `/share/[token]` 공유 감상 페이지

**목적:** 비로그인 수신자에게 PA를 책 표지처럼 보여주고, 그들의 플랫폼에서 열도록 안내한다.

**진입 경로:** 외부 링크 클릭, SNS 공유

**주요 컴포넌트:**
- `ListeningHero` — 큰 커버 이미지, 제목, 작성자
- `DiaryProse` — 일기/메모 본문 (타이포그래피 우선, 본문 720px)
- `TagRibbon` — 감정/상황 태그 (절제된 chip)
- `TrackList` — 간결한 곡 목록, 신뢰도 배지 비노출
- `OpenInExternalApp` — Spotify / Apple Music 버튼 (FR-063)
- `MissingTrackNote` — 매칭 실패 곡 수 사전 고지
- `SaveToMyLibrary` — P1 (MVP에서는 비노출)
- `SignUpInvite` — 페이지 하단 "당신의 자산도 만들어 보세요" 보조

**핵심 인터랙션:**
- 비로그인 접근 가능. 작성자 식별 정보는 닉네임 수준만.
- 외부 앱 열기 클릭 시 가능한 한 deep link 우선, 실패 시 web fallback.
- Open Graph / Twitter Card 메타 태그를 충실히 — viral K-factor 핵심 채널.

**상태:**
- **empty:** 자산은 존재하지만 일기/사진/태그가 모두 비어있음 — 곡 목록과 커버만 표시. 그래도 "감상 페이지" 톤은 유지.
- **loading:** 본문 스켈레톤 (커버 → 제목 → 본문 순서대로 페이드).
- **failure:**
  - 토큰 무효 / 자산 비공개 전환 → "이 자산은 더 이상 공개되지 않아요"
  - 자산 삭제됨 → 410 응답 페이지
- **success:** 모든 컴포넌트 정상 렌더링.

---

## 3. 화면×상태 매트릭스 (요약)

| 화면 | empty | loading | failure | success |
|:---|:---|:---|:---|:---|
| `/` | 기본 (로그아웃) | 페이드 인 | error boundary | CTA 변경 (로그인) |
| `/auth/spotify` | 안내+CTA | 콜백 대기 | 사유별 분기 | 짧은 확인 후 라우팅 |
| `/import` | 플레이리스트 0개 | 카드 스켈레톤 | 토큰/네트워크 분기 | 그리드 |
| `/import/[id]/progress` | — | narrative + 페이드 | 4종 사유 | 자동 라우팅 |
| `/assets` | "첫 자산을 만들어 보세요" | 카드 스켈레톤 | 재시도+캐시 | 카드 그리드 |
| `/assets/[id]` | 빈 Context placeholder | 헤더+리스트 스켈레톤 | 로드/저장/업로드 분기 | 자동 저장 OK |
| `/assets/[id]/export` | 매칭곡 0개 | 매핑 계산 중 | 인증/미구독/계산 실패 | 요약+CTA |
| `/assets/[id]/export/result` | — | (이전 단계 인디케이터) | failed | completed / partial |
| `/share/[token]` | Context 비어있음 | 본문 페이드 | 무효/비공개/삭제 | 감상 페이지 |

---

## 4. 컴포넌트 인벤토리

재사용 컴포넌트는 design-direction-v0.1의 Refined 원칙을 단일 출처로 한다.

### 4.1 자산/도메인 컴포넌트
| 이름 | 설명 | 사용 화면 |
|:---|:---|:---|
| `AssetCard` | PA를 표지가 있는 카드로 진열 | `/assets`, `/` (preview) |
| `AssetCoverHeader` | 상세 페이지 상단 커버+제목 영역 | `/assets/[id]`, `/share/[token]` |
| `TrackRow` | 곡 한 줄 표현 (제목/아티스트/길이) | `/assets/[id]`, export 미리보기, 감상 페이지 |
| `MatchConfidenceBadge` | 매칭 신뢰도 시각화 (4단계) | `/assets/[id]` (작성자 화면) |
| `EmotionTagPicker` | 큐레이션+자유 입력 태그 피커 | `/assets/[id]` |
| `PhotoAttachment` | 커버/감성 이미지 업로드+미리보기 | `/assets/[id]` |
| `AlternativeTrackPicker` | 실패 트랙의 대체 후보 인라인 선택 | `/assets/[id]` |
| `EmotionalContextEditor` | 일기/설명/태그/사진을 묶은 컨테이너 | `/assets/[id]` |
| `ConnectionPill` | 외부 플랫폼 연결 상태 표시 | nav, `/import`, `/assets/[id]/export` |

### 4.2 상태/시스템 컴포넌트
| 이름 | 설명 |
|:---|:---|
| `EmptyState` | 비어 있는 컬렉션의 절제된 안내 |
| `Skeleton` | 정적 페이드 스켈레톤 (shimmer 미사용) |
| `ErrorBoundaryFallback` | 글로벌 오류 폴백 |
| `InlineToast` | 일시적 알림 (저장 실패, 업로드 실패) |
| `AutoSaveIndicator` | "방금 저장됨" 미세 표시 |
| `ProgressNarrative` | 다단계 비동기 작업의 묵직한 진행 표시 |
| `ConfirmModal` | 공개 전환, 삭제 등 확인 |

### 4.3 액션 컴포넌트
| 이름 | 설명 |
|:---|:---|
| `PrimaryCTA` | 단일 강조 액션 |
| `SecondaryCTA` | 보조 액션 |
| `OpenInExternalApp` | Spotify/Apple 외부 앱 열기 (감상 페이지/상세) |
| `ActionRail` | 상세 페이지 측면 액션 묶음 |
| `RetryCTA` | 실패 상태의 재시도 액션 |

---

## 5. 인터랙션 패턴 가이드라인 (Refined Tone)

### 5.1 전환 애니메이션
- **페이지 전환:** 좌우 슬라이드 대신 fade + slight Y-offset (8~12px). duration 400~500ms, easing은 무겁게(`cubic-bezier(0.22, 1, 0.36, 1)`).
- **카드 진입:** 카드들은 동시에 페이드 인하지 않고 60~80ms씩 stagger. 책장에서 책을 한 권씩 꺼내는 인상.
- **상세→리스트 복귀:** 직전 카드의 위치를 기억하고 그 자리로 sink-back.

### 5.2 마이크로 인터랙션
- **자동 저장:** 타이핑 후 1.2s debounce → 저장 → "방금 저장됨" 350ms 페이드. 깜빡이지 않는다.
- **태그 선택:** chip 클릭 시 색상 즉시 전환 + 미세한 scale (1 → 0.98 → 1, 200ms).
- **공유 링크 복사:** 클릭 직후 CTA 라벨이 "복사됨"으로 1.5s 잠시 변경 후 원복. 별도 토스트 없음.
- **매칭 실패 트랙 해결:** 후보 선택 시 해당 행이 잠시 강조 후 "해결됨" 섹션으로 묵직하게 이동 (재배치 애니메이션 350ms).
- **CTA hover:** 부드러운 elevation 증가 (1px), 색 변화는 최소.

### 5.3 진행 표현 (비동기 작업)
- **ProgressNarrative:** % 바 대신 "지금 OOO를 하고 있어요" 형태의 문장 전환을 우선. 단계 사이는 페이드 교체.
- **부분 실패 표기:** "12곡 중 9곡을 옮겼어요"처럼 결과를 문장으로. 빨간 경고색은 사용하지 않고 muted amber로 표기.
- **무한 로더:** 회전형 spinner는 사용하지 않는다. 진행을 모르는 경우에도 narrative + 천천히 페이드되는 dot pulse를 사용.

### 5.4 폼 / 입력
- **자동 저장 우선:** 명시적 저장 버튼은 공개 상태 전환과 export 시작에만.
- **placeholder는 안내문으로:** "오늘 이 곡들과 어떤 시간을 보냈나요?" 같은 자산화 톤.
- **에러 표기:** 인라인, 빨간 텍스트 대신 muted red + 짧은 한 문장. 입력값은 사라지지 않는다.

### 5.5 모달 / 오버레이
- 배경 dim은 무겁게 (opacity 0.55~0.65). 모달 자체는 작고 밀도 낮게.
- 닫기 액션은 항상 오른쪽 상단 + ESC.

### 5.6 외부 앱 연결 (Open in External App)
- 클릭 직후 짧은 "OOO에서 여는 중…" 안내 (1.5s) → deep link 시도 → 실패 시 web fallback.
- 외부로 이동했다가 돌아왔을 때는 직전 위치를 기억한다.

---

## 6. 비고

### 6.1 본 명세에서 의도적으로 제외된 항목
- 다중 플레이리스트 동시 임포트
- 트랙 순서 재정렬 UX (P1)
- 수동 곡 검색 매칭 (P1)
- 공개 자산 탐색 / 작성자 페이지 / 좋아요·저장 (P1, FR-080~FR-083)
- 운영자 화면 (FR-100~FR-103)
- B2B / YouTube Music / 앱 내 재생

### 6.2 다음 디자인 산출물 (제안)
- 컴포넌트 라이브러리 v0.1 (Figma)
- Refined 톤 비주얼 시안 (랜딩, PA 상세, 감상 페이지)
- 마이크로카피 풀 패스 (한국어, 자산화 어휘)
- 매칭 실패 해결 인터랙션 프로토타입
