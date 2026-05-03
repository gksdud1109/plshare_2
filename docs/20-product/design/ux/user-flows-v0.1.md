# User Flows v0.1

## 문서 정보
- Date: `2026-05-03`
- Role: `Product Designer`
- Model: `Claude`
- Version: `v0.1`
- Based on:
  - `docs/20-product/strategy/prd-v0.2.md`
  - `docs/20-product/strategy/product-strategy-v0.1.md`
  - `docs/20-product/strategy/mvp-feature-priority-v0.1.md`
  - `docs/20-product/strategy/platform-direction-review-v0.1.md`
  - `docs/20-product/requirements/functional-requirements-v0.2.md`
  - `docs/20-product/design/ux/design-direction-v0.1.md`
- Scope: `MVP user journeys for Spotify Import → Emotional Context → Apple Music Export`

---

## 0. 설계 원칙 (요약)

이 문서의 모든 플로우는 다음 원칙을 전제로 한다.

- **자산화 우선:** 사용자는 곡 목록이 아니라 `Playlist Asset (PA)`을 만든다. 플로우의 정점은 "임포트 완료"가 아니라 "감성 맥락이 부착된 자산이 저장되는 순간"이다.
- **비대칭 브릿지 고정:** MVP 범위는 `Spotify Import → Add Emotional Context → Apple Music Export`로 동결된다. Apple Import / Spotify Export / YouTube Music은 본 문서에서 다루지 않는다.
- **Player-Agnostic:** 앱 내부에서 음원을 재생하지 않는다. 모든 재생은 외부 앱 연결(Open in Spotify / Apple)로 처리한다.
- **Refined 톤:** 모든 전환은 묵직하고 부드럽다. 단계 사이의 대기 화면도 "물건이 천천히 다듬어지는" 인상을 줘야 한다.
- **실패 가시화:** 매칭 실패, 인증 만료, quota 초과는 숨기지 않고 사용자가 통제 가능한 상태로 노출한다.

---

## 1. 핵심 사용자 여정 (End-to-End)

### 1.1 전체 흐름도

```
   ┌─────────────┐
   │   Landing   │  /
   │   "취향을   │
   │   자산으로" │
   └──────┬──────┘
          │ Sign in / Connect Spotify
          ▼
   ┌─────────────┐        ┌──────────────────┐
   │   Spotify   │  fail  │  Reconnect /     │
   │    OAuth    │───────▶│  Error guidance  │
   └──────┬──────┘        └──────────────────┘
          │ success
          ▼
   ┌─────────────┐
   │  Playlist   │  /import
   │  Selection  │
   └──────┬──────┘
          │ pick playlist
          ▼
   ┌─────────────┐
   │  Import &   │  /import/[playlistId]/progress
   │ Normalize   │  (ISRC matching, confidence scoring)
   └──────┬──────┘
          │ done
          ▼
   ┌──────────────────┐
   │  PA Detail       │  /assets/[id]
   │  Emotional       │  (cover, diary, tags, photo,
   │  Context Author  │   match-failure resolution)
   └──────┬───────────┘
          │
          ├──────────────► Save Draft / Publish
          │
          ├──────────────► Export to Apple Music
          │                 /assets/[id]/export
          │                 → /assets/[id]/export/result
          │
          └──────────────► Share Listening Page
                            /share/[token]
```

### 1.2 4단계 압축 모델

| 단계 | 사용자 행위 | 산출물 | 핵심 지표 |
|:---|:---|:---|:---|
| Import | Spotify 연결, 플레이리스트 선택 | `Imported Playlist` | OAuth 성공률, Import 완료율 |
| Assetize | 감성 맥락(일기/사진/태그) 부착 | `Playlist Asset` | Context Density (텍스트/사진 첨부율) |
| Share | 감상 페이지 링크 공유 | `Share Link` | Viral K-factor |
| Export | Apple Music 플레이리스트로 내보내기 | `Export Job` | Import/Export Ratio |

이 4단계 중 어느 하나라도 막히면 "취향 자산 매니지먼트 레이어" 가설이 검증되지 않는다. 따라서 모든 화면은 다음 단계로의 명확한 진입 동선을 갖는다.

---

## 2. Flow A: Spotify Import

### 2.1 목표
Spotify 사용자가 자신의 플레이리스트를 plshare2의 자산 저장소로 끌어오게 한다.

### 2.2 전제 조건
- 사용자는 plshare2 계정으로 로그인되어 있다 (FR-001).
- Spotify 계정을 보유하고 있다.

### 2.3 Step-by-step

**A-1. 진입 (`/` 또는 `/import`)**
- 랜딩 또는 빈 라이브러리 상태에서 "Spotify에서 가져오기" 단일 CTA를 표시한다.
- 카피 톤: "당신의 플레이리스트를 자산으로 옮겨오세요."

**A-2. OAuth 인증 (`/auth/spotify`)**
- 외부 Spotify OAuth 페이지로 이동.
- 권한 범위는 읽기 전용 (`playlist-read-private`, `playlist-read-collaborative`).
- 콜백 후 `Streaming Connection`이 생성된다 (FR-003).
- 실패 시: `auth_failed` 사유와 재시도 CTA를 표시.

**A-3. 플레이리스트 선택 (`/import`)**
- 사용자의 Spotify 플레이리스트 목록을 카드 그리드로 표시.
- 각 카드: 커버 이미지, 제목, 트랙 수, 마지막 수정일.
- URL 직접 붙여넣기 입력란을 보조 옵션으로 제공 (FR-010).
- 다중 선택은 MVP에서 제외 — 한 번에 하나의 플레이리스트만 임포트한다 (스코프 단순화).

**A-4. 임포트 진행 (`/import/[playlistId]/progress`)**
- 비동기 작업으로 트랙 메타데이터 수집 (FR-011, FR-012).
- 진행률은 `metadata 수집 → ISRC 매칭 → 신뢰도 분류`의 3단계로 표시.
- 묵직한 progress indicator (선형 바보다는 페이드 카운터 형태) — Refined 톤 유지.
- 부분 실패는 작업을 중단하지 않는다 (FR-012). 누락된 트랙 수만 카운트.

**A-5. 정규화 결과 확인 (→ `/assets/[id]`)**
- 임포트 완료 시 자동으로 PA 초안이 생성되며 PA 상세 페이지로 라우팅된다 (FR-031).
- 첫 진입 시 다음을 노출:
  - 정규화된 트랙 목록 (FR-022 신뢰도 배지 포함)
  - 매칭 실패/저신뢰 트랙 수
  - "감성 맥락을 추가해 자산으로 완성하세요" 가이드 카피

### 2.4 분기와 예외
- **OAuth 거부:** "권한 없이는 가져올 수 없어요" 안내 + 재시도.
- **유효하지 않은 URL:** 인라인 에러, 입력 유지 (FR-010 수용 기준).
- **빈 플레이리스트:** 임포트 진행 후 "곡이 없는 플레이리스트입니다" 상태 → 자산 생성하지 않음.
- **부분 실패:** 진행 화면에 "12곡 중 3곡 매칭 실패"를 표시하고 그대로 PA 초안 단계로 진행.

---

## 3. Flow B: Emotional Context Authoring

### 3.1 목표
정규화된 플레이리스트를 단순 목록에서 `자산`으로 전환한다. Context Density는 가설 검증의 핵심 지표다 (목표 > 40%).

### 3.2 진입점
- Flow A 완료 직후 자동 진입 (`/assets/[id]`)
- 라이브러리(`/assets`)에서 초안 PA 클릭

### 3.3 Step-by-step

**B-1. PA 상세 진입 (`/assets/[id]`)**
- 상단: 커버 이미지 영역 (기본값은 Spotify 원본). 클릭하여 사용자 이미지 업로드 가능 (FR-041).
- 하단: 트랙 리스트와 감성 맥락 입력 영역이 좌우 또는 상하로 분할.

**B-2. 텍스트 맥락 입력 (FR-040)**
- "이 자산의 한 줄 설명" 입력 (필수 아님, 강력 권장).
- "일기" 영역: 자유 서식 텍스트, 자동 저장.
- 작성 중 메타데이터(작성 시각, 편집 횟수, 입력 지속 시간)는 백그라운드 수집 — 단, MVP에서는 사용자에게 노출하지 않음 (FR-050, P1).

**B-3. 감정/상황 태그 (FR-042)**
- 태그 피커 컴포넌트로 큐레이팅된 후보 (예: "비 오는 날", "퇴근길", "위로", "집중") + 자유 입력.
- 다중 선택 가능. 시각적으로는 절제된 chip 형태.

**B-4. 사진 첨부 (FR-041)**
- 커버 이미지와 별도로 1장 이상의 감성 이미지를 첨부 가능.
- 업로드 실패 시 텍스트 저장은 유지 (FR-041 수용 기준).

**B-5. 매칭 실패 트랙 처리 (FR-023)**
- PA 상세 페이지 내 "해결 필요" 섹션에 저신뢰/실패 트랙을 분리 표시.
- 사용자 액션:
  - 대체 트랙 후보 중 선택
  - 해당 트랙을 자산에서 제외
  - "그대로 두기" — 매칭 실패 상태로 유지하되 export 시 누락됨을 사전 고지
- MVP에서는 후보 자동 추천만 제공. 수동 검색 매칭은 P1.

**B-6. 저장 / 공개 상태 전환**
- 자동 저장된 초안은 "비공개로 보관" 또는 "공개로 전환" 두 가지 상태로 명시적 변경 (FR-033).
- 공개 전환 시 공유 링크가 활성화된다.

### 3.4 분기와 예외
- **이미지 업로드 실패:** 인라인 토스트, 재시도 버튼. 다른 입력은 유지.
- **자동 저장 실패:** 상단 배너로 "저장이 일시적으로 실패했어요"와 수동 저장 CTA (FR-031 수용 기준).
- **세션 만료:** 입력 내용을 로컬에 임시 보존 후 재로그인 안내.

---

## 4. Flow C: Apple Music Export

### 4.1 목표
저장된 PA를 Apple Music 사용자의 라이브러리에 새 플레이리스트로 생성한다. 비대칭 브릿지 가설의 종착점.

### 4.2 전제 조건
- PA가 1개 이상의 매칭된 트랙을 보유 (FR-072).
- 사용자는 Apple Music 계정 연결이 필요하나, 이 단계 진입 시점에 연결을 유도해도 된다.

### 4.3 Step-by-step

**C-1. Export 진입 (`/assets/[id]/export`)**
- PA 상세에서 "Apple Music으로 내보내기" CTA 클릭.
- Apple 미연결 상태라면 먼저 MusicKit 인증 모달로 안내 (FR-003).

**C-2. 매핑 결과 확인**
- export 미리보기 화면에서 다음을 표시:
  - 전송 예정 곡 수
  - 누락 예정 곡 수와 사유 (매칭 실패 / 사용자 제외 / 지역 제한)
  - Apple Music에 생성될 플레이리스트 제목과 설명 미리보기
  - 직전 export 이력 (있을 경우 — FR-074)
- 사용자는 제목/설명을 마지막으로 편집할 수 있다.

**C-3. Export 실행**
- "내보내기 시작" 클릭 시 `Export Job`이 큐에 들어간다 (FR-073, status=`queued`).
- 진행 상태는 `running`으로 전환되며 사용자는 백그라운드 진행을 허용 가능.

**C-4. 결과 확인 (`/assets/[id]/export/result`)**
- 가능한 결과 상태:
  - `completed`: 모든 매칭 곡 전송 성공
  - `partially completed`: 일부 곡 누락 — 누락 목록 표시
  - `failed`: 인증/quota/네트워크 실패 — 사유와 재시도 CTA
- 성공 시 "Apple Music에서 열기" 딥링크 제공 (외부 앱 연결).
- 직전 export와의 차이가 있으면 "재시도" 또는 "새로 내보내기"를 명확히 구분 (FR-074).

### 4.4 분기와 예외
- **Apple 인증 만료:** export 시작 직전 재연결 모달.
- **Apple Music 미구독 계정:** 라이브러리 쓰기 불가 — 사용자에게 사전 고지 후 export 차단.
- **부분 실패:** 자산은 그대로 유지, export job만 `partially completed`로 기록.
- **Quota 초과:** "지금은 일시적으로 처리 한도에 도달했어요" 메시지와 자동 재시도 옵션.

---

## 5. Flow D: Asset Sharing (Listening Page)

### 5.1 목표
PA를 로그인 없이 누구나 볼 수 있는 감상 페이지 링크로 공유한다. K-factor > 0.2 가설의 핵심 채널.

### 5.2 Step-by-step

**D-1. 공유 활성화 (`/assets/[id]`)**
- PA 상태가 `public`일 때만 공유 가능 (FR-033, FR-062).
- "공유 링크 만들기" 클릭 시 `Share Link` 토큰이 발급되며 클립보드 복사 + 시스템 공유 시트 호출.

**D-2. 감상 페이지 (`/share/[token]`)**
- 비로그인 접근 허용 (FR-062 수용 기준).
- 표시 요소:
  - 커버 이미지 (큰 비중, 책 표지처럼 처리)
  - 제목, 작성자 이름
  - 일기/메모 본문 (타이포그래피 우선)
  - 트랙 리스트 (간결, 매칭 신뢰도는 비노출)
  - 감정/상황 태그
  - "Spotify에서 열기" / "Apple Music에서 열기" CTA (FR-063)
- 앱 내부 재생 없음.

**D-3. 외부 앱 연결**
- 수신자가 어떤 플랫폼을 쓰는지에 따라 자동 우선순위 노출.
- 미매칭 곡 수를 사전 고지 (FR-063 수용 기준).
- "이 자산을 내 라이브러리에도 저장하기"는 P1 (저장/좋아요 — FR-081).

### 5.3 분기와 예외
- **링크 무효화:** 작성자가 자산을 비공개 전환하면 페이지는 "이 자산은 더 이상 공개되지 않아요" 상태.
- **삭제된 자산:** 410 응답과 안내 페이지.
- **봇 트래픽:** Open Graph 메타 태그는 노출하되 본문 콘텐츠는 정상 응답.

---

## 6. Edge Case 처리

### 6.1 매칭 실패 트랙
| 상태 | UI 처리 | 사용자 액션 |
|:---|:---|:---|
| `unmatched` | 빨간 점 배지 + "매칭 실패" 라벨 | 대체 후보 선택 / 제외 / 그대로 두기 |
| `low-confidence` | 노란 점 배지 + "확인 필요" 라벨 | 후보 검토 / 그대로 사용 |
| `high-confidence` | 회색 점 (거의 비노출) | 별도 액션 없음 |
| `exact` | 배지 없음 | 별도 액션 없음 |

매칭 실패 처리는 import 직후 강제하지 않는다. PA 상세에서 사용자가 자기 페이스로 해결할 수 있다.

### 6.2 인증 만료
- **Spotify token 만료:** import 작업 시점에만 영향. 만료 시 silent refresh 시도 → 실패 시 재인증 모달.
- **Apple MusicKit 만료:** export 시도 시점에 감지. 진행 중인 작업은 중단하고 재인증 후 재개.
- **plshare2 세션 만료:** 입력 중인 텍스트는 로컬 캐시에 보존, 재로그인 후 복원.

### 6.3 API Quota / Rate Limit
- **Spotify rate limit:** import progress 페이지에서 "데이터를 더 천천히 가져오고 있어요" 메시지로 자동 재시도. 사용자에게는 평균 대기 시간을 노출하지 않는다 (Refined 톤).
- **Apple Music quota:** export 요청 단위로 backoff. 실패 시 `failed` 상태로 기록하고 명시적 재시도 CTA.
- **사용자별 일일 한도(운영 정책):** MVP에서는 별도 표시 없음 — 운영 모니터링은 FR-101에 의존.

### 6.4 부분 실패의 일반 원칙
- 자산 자체는 절대 잃지 않는다 (출시 체크리스트 #3).
- 실패 항목은 "다시 시도 가능한 상태"로 표시한다.
- 사용자가 명시적으로 닫지 않는 한 실패 흔적은 사라지지 않는다.

---

## 7. 다음 단계 (Handoff to Frontend)

### 7.1 의존하는 동결 사항
- 라우트 트리는 `screen-specs-v0.1.md` §1을 단일 소스로 한다.
- 라우트별 4가지 상태(empty / loading / failure / success)는 `screen-specs-v0.1.md` §2에서 명세된 정의를 따른다.
- Refined 톤 인터랙션 패턴은 `screen-specs-v0.1.md` §5의 가이드라인을 따른다.

### 7.2 프론트엔드 (Next.js App Router) 진입 작업
1. `app/(auth)/auth/spotify/route.ts` — OAuth 콜백 핸들러
2. `app/import/page.tsx` — 플레이리스트 선택 (RSC + client interactions)
3. `app/import/[playlistId]/progress/page.tsx` — 임포트 진행, polling or SSE
4. `app/assets/page.tsx` — 라이브러리
5. `app/assets/[id]/page.tsx` — PA 상세, Emotional Context 작성 (server actions)
6. `app/assets/[id]/export/page.tsx` — export 미리보기
7. `app/assets/[id]/export/result/page.tsx` — export 결과
8. `app/share/[token]/page.tsx` — 비로그인 감상 페이지

### 7.3 백엔드 의존성
- `Spotify Read Adapter` (import, OAuth)
- `Normalization Engine` (ISRC 우선 매칭)
- `Apple Music Write Adapter` (export)
- `Share Link` 토큰 발급 및 무효화

### 7.4 디자인 다음 단계
- 컴포넌트 레벨 와이어프레임 / 비주얼 시안 작성 (`design-direction-v0.1.md`의 Refined 원칙 반영)
- 마이크로카피 전체 패스 (한국어, 자산화 톤)
- 매칭 실패 해결 UX의 인터랙션 프로토타입

---

## 8. 범위 외 (Out of Scope) 명시

다음 항목은 본 문서에서 의도적으로 다루지 않는다.

- B2B 시나리오 / 콘솔 / 데이터 판매 워크플로
- YouTube Music import 또는 export
- 앱 내부 음원 재생 / 미리듣기
- 결제, 선물하기, 토큰 발급
- 강한 인간성 검증(Biometrics, GPS) 노출 UX
- Apple Music import / Spotify export (P1+)
