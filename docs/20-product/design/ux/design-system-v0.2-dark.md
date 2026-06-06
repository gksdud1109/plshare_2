# Design System v0.2 — "Nocturne" (Dark Premium)

- Date: `2026-06-06`
- Supersedes the visual layer of `design-direction-v0.1.md` (Refined Asset / light serif)
- Mood: **Apple Music × Toss polish** — deep dark, album-art-forward, glassy depth, emotional keepsake
- Single source of truth for the frontend redesign. All components/pages must conform.

---

## 1. 원칙
1. **앨범아트가 주인공.** 커버 이미지를 크게, 글로우(앰비언트)와 함께. 텍스트는 그 위/옆에서 보조.
2. **딥다크 + glassy 깊이.** 표면은 거의 검정, 카드/오버레이는 반투명 + blur + soft shadow.
3. **Pretendard 한글 우선.** 세리프 금지. 굵기 대비로 위계. (라틴은 Pretendard가 Inter 기반이라 함께 깔끔.)
4. **하나의 vivid 악센트.** 보라/인디고 #7C5CFF. CTA·포커스·진행·강조에만. 남발 금지.
5. **죽은 여백 제거.** 비대칭 editorial 대신 꽉 찬, 의도된 레이아웃. 큰 히어로 + 리듬감 있는 섹션.
6. **스프링 모션.** 등장은 fade+rise, 이미지 줌, 호버 lift. 과하지 않게.

---

## 2. 컬러 토큰 (CSS 변수, 다크 고정)

```
/* Surfaces — near-black, 층위가 올라갈수록 살짝 밝게 */
--bg:            #0B0B0F   /* page */
--surface-1:     #121218   /* base card */
--surface-2:     #181820   /* raised card / input */
--surface-3:     #20212B   /* hover / popover */
--hairline:      rgba(255,255,255,0.08)   /* borders/dividers */
--hairline-strong: rgba(255,255,255,0.14)

/* Text */
--text-hi:       #F4F4F7   /* headings */
--text:          #C7C8D1   /* body */
--text-mid:      #8E8FA0   /* secondary */
--text-low:      #5E5F6E   /* hints, captions */

/* Accent — vivid violet/indigo */
--accent:        #7C5CFF
--accent-hi:     #9B82FF   /* hover/brighter */
--accent-press:  #6A4AE0
--accent-soft:   rgba(124,92,255,0.14)  /* tints, focus ring bg */
--on-accent:     #0B0B0F   /* text on accent fills은 화이트(#fff) 사용; 어두운 칩 위 텍스트만 이 값 */

/* Signals */
--success:       #4ADE80
--warning:       #FBBF24
--danger:        #FB7185

/* Glass */
--glass-bg:      rgba(20,20,28,0.62)
--glass-border:  rgba(255,255,255,0.10)
--glass-blur:    18px
```

Tailwind v4 `@theme` 에 위를 `--color-*`로 매핑. 사용 예: `bg-bg`, `bg-surface-1`, `text-text-hi`, `text-accent`, `border-hairline`.

---

## 3. 타이포그래피 (Pretendard)

- 폰트 스택: `'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Malgun Gothic', system-ui, sans-serif`
- Pretendard Variable는 CDN `@import`로 로드(아래 globals). 오프라인 시 Apple SD Gothic Neo로 graceful fallback.
- **세리프(Fraunces) 완전 제거.**

스케일 (clamp 반응형):
| 토큰 | 크기 | 용도 |
|:--|:--|:--|
| display | `clamp(2.5rem, 6vw, 4.5rem)` / weight 800 / tracking -0.03em / line 1.05 | 히어로 |
| h1 | `clamp(2rem, 4vw, 3rem)` / 700 / -0.02em | 페이지 타이틀 |
| h2 | `1.5rem` / 700 | 섹션 |
| h3 | `1.125rem` / 600 | 카드 타이틀 |
| body | `1rem` / 400 / line 1.6 | 본문 |
| sm | `0.875rem` / 450 | 보조 |
| label | `0.75rem` / 600 / tracking 0.12em / uppercase | eyebrow/캡션 |
| num | tabular-nums | 트랙 길이/카운트 |

---

## 4. 깊이 / glass / radius / shadow
- radius: card 18px, button/pill 999px(또는 12px), input 14px, image 16px
- shadow: `--shadow-card: 0 8px 30px -12px rgba(0,0,0,0.6)`, `--shadow-pop: 0 20px 60px -20px rgba(0,0,0,0.7)`
- glass 유틸 `.glass`: `background: var(--glass-bg); backdrop-filter: blur(var(--glass-blur)); border: 1px solid var(--glass-border);`
- **앨범아트 글로우**: 커버 뒤에 같은 이미지를 blur+scale+opacity로 깔아 앰비언트 라이트 (`.cover-glow`). 또는 accent radial glow.

---

## 5. 핵심 컴포넌트 규격

- **Button**
  - primary: `bg-accent text-white`, hover `bg-accent-hi`, active `bg-accent-press`, radius full, h 48, px 24, 폰트 600, 포커스 `ring-2 ring-accent`. 스프링 hover(약간 lift+밝아짐).
  - secondary: `glass` + `border-hairline-strong text-text-hi`, hover surface-3.
  - ghost: text-only, text-mid → hover text-hi.
- **Card**: `bg-surface-1 border border-hairline rounded-[18px] shadow-card`, hover `-translate-y-1` + border-hairline-strong (transition spring).
- **AssetCard / PlaylistCard**: 정사각 커버(글로우), 하단 글래스 캡션(제목/트랙수/Emotional 배지). 호버 시 커버 살짝 줌 + lift.
- **TrackRow**: index(num, text-low) · 제목(text-hi) · 아티스트(text-mid) · 우측 길이(num) 또는 MatchBadge. 호버 surface-2.
- **MatchConfidenceBadge**: matched=success 점+라벨, alternative=warning, failed=danger. pill, 작은 dot.
- **EmotionTagPicker**: 토글 칩. 선택=accent-soft bg + accent text + accent border; 미선택=surface-2 + text-mid. 스프링 토글.
- **ProgressNarrative**: 문장 cycle(유지) + accent 진행 바(슬림, 글로우). 스피너 금지.
- **Toast**: glass + accent 좌측 보더, 하단 중앙 슬라이드업.
- **SessionBadge**: 헤더 우측. authed=accent dot + "연결됨" + 로그아웃 ghost; 아니면 "Spotify 연결" pill.
- **PageShell**: 상단 sticky glass 헤더(로고 좌 / 네비·SessionBadge 우), 컨텐츠 max-w-6xl, 충분한 상하 패딩, 페이지 배경에 은은한 accent radial glow 1개.

---

## 6. 페이지별 의도 (레이아웃)
- `/` 랜딩: 풀블리드 히어로 — 큰 display 카피 + accent CTA + 떠다니는 앨범아트 콜라주(글로우)로 오른쪽 void 채움. 하단 3-step은 아이콘+카드.
- `/auth/spotify`: 센터드, accent 펄스 글로우 + ProgressNarrative. 미니멀하지만 살아있게.
- `/import`: 플레이리스트 그리드(앨범아트 카드, 호버 줌). eyebrow + h1. void 금지 — 카드가 화면 채움.
- `/import/[id]/progress`: 큰 커버(가져오는 중, 글로우 펄스) + ProgressNarrative + accent 진행바. 중앙 정렬, 풍부하게.
- `/assets`: 라이브러리 그리드. 헤더 + "새로 가져오기" accent 버튼. Emotional 배지.
- `/assets/[id]`: 좌측 큰 커버(글로우) + 우측 메타/액션. 하단 좌 트랙리스트 / 우 Emotional Context 글래스 카드. 다크에서 입력 필드 대비 확보.
- `/assets/[id]/export`: 큰 커버 + 매핑 결과 리스트(MatchBadge) + 진행. accent.
- `/export/result`: 성공 히어로 — 큰 체크/커버 글로우 + "Apple Music에서 열기" accent + 매칭 요약.
- `/share/[token]`: 공개 키프세이크 — 시네마틱. 커버 글로우 풀블리드 + diary + 감정칩 + 트랙. "내 라이브러리 만들기" CTA. (OG 이미지는 별개 유지)

---

## 7. 제약 (기능 보존)
- 라우트 URL·데이터 흐름·API 클라이언트 변경 금지. **시각/마크업/클래스만** 변경.
- 기존 E2E(`frontend/e2e/demo-flow.spec.mjs`) 셀렉터/텍스트가 깨지지 않게: 버튼/링크의 한글 텍스트, `<textarea>`, 감정 태그 라벨, 라우팅 동작 유지.
- `npm run build` + `tsc --noEmit` + `lint` 통과. demo 플로우 16/16 유지.
- 다크 단일 테마(라이트 토글 불필요). prefers-color-scheme 분기 제거.
