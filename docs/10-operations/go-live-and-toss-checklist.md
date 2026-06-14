# plshare2 — Go-Live & 토스 인앱출시 준비 체크리스트

> 생성 2026-06-14 · 자동 조사 종합(설정 감사 + 토스 공식문서 리서치 + 프로덕션 준비도 + WebView 호환성 감사).
> "나에게 시키면 됨" = 코드 작업 지시 / "직접 콘솔" = 외부 콘솔에서 직접 / [추정] = 코드·문서 미확인.

---

# plshare2 Go-Live 준비물 (운영자 직접 준비)

> 전제: 현재 기본 프로파일이 `demo`(H2 + Mock, 자격증명 0개)라 **아무 설정 없이 배포하면 H2 + 로그인 우회 상태로 뜬다**. prod로 띄우려면 아래를 순서대로 진행. 추정 항목은 [추정] 표기.

## 0. 의존성 순서 (먼저 이것부터)
1. **확정 도메인 2개를 먼저 정한다** — 프론트(예: `app.plshare.com`), 백엔드 API(예: `api.plshare.com`). 이 값이 정해져야 redirect URI / CORS / env가 전부 채워진다. **모든 후속 단계의 선행 조건.**
2. 자격증명 발급 → 인프라 프로비저닝 → 코드 선행 수정(차단 항목) → 배포 → 검증.

---

## A. 자격증명 / 연동 (콘솔에서 직접 발급)

### A-1. 세션 시크릿 (가장 쉬움, 의존성 없음 — 먼저 해두기)
- [ ] `APP_SESSION_SECRET` 생성: `openssl rand -hex 32` (**≥32자 필수**). 미설정 시 공개 기본키 `demo-session-secret-change-me`로 서명 → 세션 위조 가능.
  - 키 이름: `APP_SESSION_SECRET` / 발급: 직접 생성 / **필수**
  - (선택) `APP_SESSION_TTL_SECONDS` 기본 2592000(30일)

### A-2. Google Cloud — 로그인 + YouTube (필수, 도메인 확정 후)
- [ ] **OAuth 2.0 Client ID** 생성: Console → APIs & Services → Credentials → Create → OAuth client ID → **Web application**
- [ ] **Authorized redirect URI 등록** (BE 콜백, 콘솔값과 env가 **정확히 일치**해야 함):
  - 로컬: `http://localhost:8080/api/auth/google/callback`
  - 배포: `https://<api-도메인>/api/auth/google/callback`
- [ ] **OAuth consent screen** 구성: External + 테스트 사용자(미검증 시 ≤100명). 로그인 스코프 `openid email profile`. YouTube export는 점진 동의로 `youtube` 스코프 추가.
- [ ] `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` 복사 → env 주입 (**필수**; 미설정 시 호출 시점 502/UPSTREAM_ERROR)
- [ ] `GOOGLE_REDIRECT_URI`를 **prod 백엔드 도메인**으로 (기본이 localhost라 배포 시 사실상 필수)
- [ ] **YouTube Data API v3 Enable** (export 및 search 호출에 필요)
- [ ] **YouTube Data API "API key" 발급** (OAuth와 **별개의 키**) — **익명 트랙 재생(public resolve)에 필요**. 단 코드가 아직 이 키를 안 쓰므로 §C 선행 수정 필요.

### A-3. 관리형 PostgreSQL (배포 시 필수)
- [ ] Cloud SQL / RDS / Supabase Postgres 등 인스턴스 프로비저닝 → DB `plshare` 생성
- [ ] `DATABASE_URL`(JDBC) / `DATABASE_USERNAME` / `DATABASE_PASSWORD` 확보 → env 주입
- [ ] Flyway가 기동 시 V1~V13 **자동 실행**(수동 마이그레이션 불필요). DB 인스턴스/네트워크/방화벽만 직접 준비.
- [ ] [추정] 빈 prod DB 첫 적용 시 `baseline-on-migrate: true` 동작 점검(낮음)

### A-4. (dormant) Spotify — ⚠️ prod 기동 차단 주의
- [ ] **`SPOTIFY_CLIENT_ID` / `SPOTIFY_CLIENT_SECRET`는 prod yml에 기본값이 없음** → prod 프로파일이면 전환 기능을 안 켜도 **앱 시작 자체가 실패**. **켜지 않더라도 더미값이라도 반드시 주입** 필요.
- [ ] 실제로 켤 때만: developer.spotify.com/dashboard → Create app(Web API) → Redirect URI `…/api/auth/spotify/callback` 등록 → ID/Secret 복사
- [ ] UI 노출은 `NEXT_PUBLIC_CONVERSION_ENABLED=true`일 때만 (기본 off)

### A-5. (dormant) Apple MusicKit — 선택
- [ ] 켤 때만: developer.apple.com → Keys → Media Services(MusicKit) → .p8 **1회 다운로드** → `APPLE_TEAM_ID` / `APPLE_KEY_ID` / `APPLE_PRIVATE_KEY_PEM`(개행→`\n` 치환) / `APPLE_STOREFRONT`(기본 us). 미설정이어도 기동은 성공(첫 호출 시 throw).

### A-6. (선택) S3 객체스토리지 — ⚠️ 코드 stub
- [ ] 사진 업로드를 prod에서 실제로 쓰려면 키만으로 **안 됨** — 어댑터가 stub(`S3StorageAdapter.kt:44,62` throw). 키(`S3_BUCKET`/`AWS_*`/`S3_ENDPOINT`/`S3_PUBLIC_BASE_URL`) 준비와 **별개로 코드 구현 필요**.

---

## B. 인프라 (단계별)

### B-1. 백엔드 → Cloud Run
- [ ] `backend/Dockerfile`(temurin 17, EXPOSE 8080) 빌드 → Artifact Registry/GCR 푸시
- [ ] 환경변수 주입 (**시크릿은 Secret Manager 권장**):
  - `SPRING_PROFILES_ACTIVE=prod` (**필수 — 미지정 시 H2+Mock으로 뜸**. demo를 절대 섞지 말 것 → §C 세션가드 사각지대)
  - `APP_SESSION_SECRET`(≥32), `GOOGLE_CLIENT_ID/SECRET/REDIRECT_URI`, `DATABASE_URL/USERNAME/PASSWORD`
  - `SPOTIFY_CLIENT_ID/SECRET`(더미라도) — 기동 차단 회피
  - (켤 경우) Apple / S3 / YouTube API key
- [ ] Cloud SQL 연결: VPC connector 또는 Cloud SQL socket
- [ ] HTTPS/도메인: Cloud Run URL 또는 커스텀 도메인 매핑

### B-2. 프론트엔드 → Vercel
- [ ] **빌드타임 env** (번들 인라인 — 빌드 시 반드시 설정):
  - `NEXT_PUBLIC_API_BASE_URL` = 실 API 도메인
  - `NEXT_PUBLIC_DEMO_MODE=false`
  - (선택) `NEXT_PUBLIC_CONVERSION_ENABLED`
- [ ] **런타임 env**:
  - `API_BASE_INTERNAL_URL` = BFF→BE 내부 주소
  - `NEXT_PUBLIC_APP_URL` = 실 프론트 도메인
- [ ] [참고] `NEXT_PUBLIC_SUPABASE_*`는 placeholder/미사용. 실제 인증은 자체 HMAC 세션 + BFF. 설정 불필요(혼동 주의).

### B-3. 도메인 / DNS / HTTPS 일치 확인 (배포 후)
- [ ] 프론트·API 도메인 DNS/HTTPS 매핑
- [ ] Google 콘솔의 redirect URI = `GOOGLE_REDIRECT_URI` env = 실제 BE 도메인 **3자 일치**
- [ ] `FE_REDIRECT_URL`(BE) = 실 프론트 도메인(예: `https://<app>/import`)
- [ ] BE CORS allowlist에 프론트 도메인 포함 확인 (→ §C-4 코드 선행 필요)

### 런타임 버전 (참고)
| 항목 | 버전 |
|---|---|
| Java | 17 |
| Spring Boot | 3.2.4 (업그레이드 권장) |
| Node | 20 |
| Next.js | 16.2.3 |
| PostgreSQL | 16 |

---

## C. 런칭 전 코드/보안 필수 작업 (차단성 표기)

> 표기: **🔴 BLOCKER**(이게 없으면 prod 동작 불가/즉시 사고) · 🟡 강력권장 · ⚪ 위생
> "나에게 시키면 됨" = 운영자가 작업 지시(코드 수정)하면 되는 항목 / "직접 콘솔" = 운영자가 외부 콘솔에서 직접 해야 하는 항목

### 🔴 BLOCKER (런칭 차단)
- [ ] **C-1. 프로파일 강제** — `SPRING_PROFILES_ACTIVE=prod` 주입 + prod 기동 매니페스트 작성. **[직접 콘솔/인프라 env]** (작업량 S)
- [ ] **C-2. 세션 시크릿 주입 + 가드 보강** — `APP_SESSION_SECRET`(≥32) 주입 **[직접]** + 가드가 `demo` 포함 시 통째 스킵되는 사각지대(`demo,postgres` 등) 블랙리스트 방식 강화 **[나에게 시키면 됨]** (S). prod에 demo를 절대 섞지 말 것.
- [ ] **C-3. CORS origin env 외부화** — `WebConfig.kt`가 localhost 하드코딩(`allowedOrigins("http://localhost:3000"...)`)이라 **Vercel 프론트에서 모든 호출 CORS 차단**. `ALLOWED_ORIGINS` env화 후 prod 도메인 추가. **[나에게 시키면 됨]** (S, 의존: 확정 도메인)
- [ ] **C-4. YouTube API-key resolve 경로 구현** — `TrackPlaybackService.kt:55`가 `"demo-youtube-token"` 리터럴을 실제 YouTube API에 Bearer로 전송 → **prod에서 익명 재생 전부 401**(gift-first 핵심 동선 붕괴). `RealYouTubeClient` search에 `key=` 파라미터 경로 추가 + 리터럴 제거 + 설정 바인딩. **[나에게 시키면 됨]** (M, 의존: YouTube Data API **key** 발급 [직접 콘솔])

### 🟡 강력 권장 (런칭 직전)
- [ ] **C-5. 익명 resolve 레이트리밋 + read/write 쿼터 분리** — 공개 `GET /api/tracks/{id}/youtube`가 export와 동일 일일 budget(8000u) 소모. 익명 80개 신규 trackId로 당일 budget 소진 → export 전면 마비(DoS). IP/recipient 토큰버킷 + read budget 분리. **[나에게 시키면 됨]** (M)
- [ ] **C-6. OAuth access/refresh 토큰 암호화** — `GoogleAccessGrant.kt`가 평문 varchar 저장. JPA `AttributeConverter`로 AES-GCM. **[나에게 시키면 됨 + 암호화 키 관리 직접]** (M, 의존: KMS/env 키)
- [ ] **C-7. BFF 프록시 경로 allowlist** — `api/backend/[...path]/route.ts`가 전 경로/전 메서드 catch-all 프록시 + 세션토큰 자동첨부. `/api/...` prefix allowlist. **[나에게 시키면 됨]** (S)
- [ ] **C-8. Spring Boot 3.2.4 → 최신 패치/3.3.x 업그레이드** [추정: CVE 세부 미검증, 버전 노후 근거]. **[나에게 시키면 됨]** (S~M)
- [ ] **C-9. Next.js 16.2.3 미들웨어 우회 CVE 적용성 확인/업그레이드** [추정: 적용성 코드로 미확인]. 인증 게이트가 미들웨어 기반이라 점검 필요. **[나에게 시키면 됨]** (S)

### ⚪ 위생
- [ ] **C-10. `.env.local` untrack** — `git rm --cached frontend/.env.local` (현재 추적 중. 내용은 무해하나 패턴 위험). **[나에게 시키면 됨]** (S)

---

## 실행 순서 요약 (최소 경로)
1. 도메인 2개 확정 → `APP_SESSION_SECRET` 생성
2. Google OAuth Client + consent + YouTube API v3 Enable + **API key** 발급 [직접 콘솔]
3. PostgreSQL 프로비저닝 [직접 콘솔]
4. **선행 코드 수정**: C-1 프로파일 / C-3 CORS env / C-4 YouTube API-key / C-2 세션가드 [나에게 지시]
5. backend 이미지 → Cloud Run + env/Secret(+`SPOTIFY_*` 더미) 주입
6. frontend → Vercel + 빌드/런타임 env
7. 도메인/DNS/HTTPS + redirect URI 3자 일치 검증
8. (직전) C-5~C-10 권장 작업

---

# plshare2 토스 앱인토스(Apps in Toss) 인앱 출시 준비

> 출처: 토스 공식 앱인토스 개발자센터(`developers-apps-in-toss.toss.im`). "공식 확인" vs "[추정]" 명시. 조사일 2026-06-14.

## ⚠️ 가장 큰 차단점 3가지 (모두 공식 확인)
1. **외부 OAuth(Google 로그인) 금지** — 미니앱은 **토스 로그인만** 허용. plshare의 Google OAuth 리다이렉트 흐름은 **그대로 입점 불가** → 토스 로그인으로 교체 필수. 게다가 Google은 인앱 WebView에서 OAuth를 정책상 차단(`disallowed_useragent`)하므로 기술적으로도 안 돌아옴.
2. **토스 로그인 = 사업자 등록 사실상 전제** — 토스 로그인·결제·프로모션은 사업자 필수. plshare는 로그인 기반(취향 자산·선물)이라 [추정: 근거 강함] **사업자 등록이 사실상 필수**. 현재 solo/pre-business로는 막힘.
3. **결제는 토스페이/인앱결제(IAP)만** — 외부 PG/자체 결제 금지. 유료화 시 토스 결제 + 사업자 동시 조건.

---

## 1. 입점 / 제휴 절차 (공식 확인)
- [ ] **앱인토스 콘솔**(`apps-in-toss.im.toss.im/workspace`) 워크스페이스 생성 → 앱 등록 → 검토 요청
- [ ] 계정 요건: **만 19세 이상 + 본인 명의 토스앱 로그인**. 토스 비즈니스 회원 기반
- [ ] 워크스페이스: 사업자당 1개 (개인 개발자는 사업자 없이도 생성·테스트 가능)
- [ ] 검수: 콘솔 '검토 요청하기' → 심사 → 승인 → 출시. **최소 1회 테스트 완료 필수**
- [ ] 검수 소요: 앱 등록 1~2영업일, 출시 검수 3~5영업일. 전체 체감 비게임 **약 1~3개월**
- [ ] 출시 반영: 승인 후 **즉시 전체 사용자 반영**(점진 롤아웃 아님) → 사전 테스트 중요
- [ ] 반려 시: 원인 확인 후 새 번들 업로드로 재신청

### 사업자 / 자격 요건
- 워크스페이스 생성·개발·테스트: 사업자 **없이 가능**
- 사업자 없이 **출시**: 가능하나 기능 제한
- 사업자 **필수** 기능: **토스 로그인 · 비즈월렛 · 프로모션 · 인앱 광고 · 토스페이 · 인앱결제**
- → [추정] plshare는 로그인 기반이라 **사업자 등록 선행 필요**

---

## 2. 기술 요건 (공식 확인)
- [ ] 런타임 `Granite`. SDK: **WebView SDK** / RN SDK. plshare Next.js는 **WebView SDK 경로** 적합
- [ ] 기존 웹 연동: `@apps-in-toss/web-framework` 설치 → `npx ait init` → `granite.config.ts`
- [ ] **SDK 2.x 필수** (2026-03-23 이후 1.x 번들 업로드 불가). 2.x = RN 0.84 / React 19
- [ ] **SSR 금지** ← Next.js 16 SSR을 **CSR/정적 구조로 재구성** 필요
- [ ] eval/외부 코드 실행 금지, **라이트모드 필수**, 인터랙션 2초+ 지연 금지, 제스처 확대축소 비활성
- [ ] 외부 이동 유도 금지(자사 앱 설치·브라우저 히스토리 조작 금지)
- [ ] 네비게이션 바: 비게임 규격(좌 뒤로가기, 중앙 로고+미니앱명 국문, 우 기능버튼 최대 1개)
- [ ] 도메인: 운영 `https://<app>.apps.tossmini.com`, 테스트 `*.private-apps.tossmini.com`. **HTTPS/WSS 강제**
- [ ] 대상 OS: Android 7+ / iOS 16+
- [ ] TDS(Toss Design System) 적용 권장(일부 문서는 검수 기준 표현 — 사실상 준수 권장)
- [ ] 외부 웹 API(일반 fetch, YouTube Data API key 검색 등)는 제한 안 함 [plshare 적용은 추정]

---

## 3. 인증 — 토스 로그인 vs Google OAuth (★ 핵심)
- **허용**: 토스 로그인만 (공식 확인). 자사/소셜/간편 로그인 불가
- **토스 로그인 흐름**: 표준 OAuth2 Authorization Code. 클라 `appLogin`(WebView SDK v1.0.3+)으로 인가코드 → **반드시 서버**가 `/api-partner/v1/apps-in-toss/user/oauth2/generate-token`로 토큰 교환 → `/login-me`로 사용자 조회. 외부 브라우저 리다이렉트 아닌 **토스 앱 내장 인증**
- 토큰: AccessToken 1h / RefreshToken 14d / 인가코드 10분(1회성). 식별 `userKey`(앱별 고유), 동의항목 암호화 제공, **DI는 항상 null**
- 비로그인/게스트 운영 가능 여부: **공식 문서 명시 없음** [추정/미확인 — 콘솔 문의 필요]

---

## 4. 결제 (공식 확인)
- [ ] 토스페이/IAP만. 외부 PG 금지
- [ ] IAP 가격: 최소 400원 ~ 최대 1,400,000원
- [ ] 수수료: 앱마켓 15~30% + 토스 5%
- [ ] 검수: 결제 중 음악 일시정지, 주문금액=결제창금액 일치, 취소 시 이전화면 복귀
- [ ] [추정] 미니앱은 앱인토스 IAP/토스페이 흐름 사용(토스페이먼츠 일반 가맹 연동과 다른 경로)
- → **초기 무료(gift-first) 출시 권장.** 유료화 시 토스 결제 + 사업자 동시

---

## 5. WebView 코드 변경 (감사 기반 — file:line은 §하단)

### 🔴 P0 — 인증 (전체 차단)
- [ ] **P0-1. Google OAuth 외부 hop 제거 → 토스 로그인 배선.** 현재 `auth/continue/page.tsx`가 비-localhost면 무조건 `accounts.google.com`으로 hop → 인앱 WebView에서 차단 + 복귀 시 세션 미수립 → 보호 경로 무한 리다이렉트. `proceed()`에 토스 환경 감지 분기 추가: 인앱이면 `appLogin` → BE 토큰 교환 → `setSessionCookie`
- [ ] **P0-2. 데모 게이트가 hostname 기준** → prod WebView에선 절대 안 탐. WebView용 실 로그인 대체 경로 필요(=P0-1)

### 🟡 P1 — 세션/프록시
- [ ] **P1-1. 세션 쿠키 `SameSite=Lax` + 서드파티 격리** → WKWebView에서 세션 유실 위험. 프론트·BE를 **동일 사이트(서브도메인 BFF)** first-party로 묶기, 안 되면 브리지 토큰 fallback
- [ ] **P1-2. BFF 쿠키→Bearer 변환 의존** → 쿠키 없으면 전부 401 (P0-1/P1-1 해결에 종속)

### 🟡 P1~P2 — 핵심 가치(선물 재생·공유)
- [ ] **P1-3. YouTube iframe** — `TrackEmbed.tsx`에 `playsinline=1` 추가 + `autoplay=1` 제거 + 재생 실패 시 "유튜브에서 열기" 폴백. 선물 언박싱(앱 없이 듣기)의 핵심
- [ ] **P2-1. 클립보드** — `navigator.clipboard.writeText`가 WebView에서 조용히 실패. `execCommand('copy')` 폴백 또는 토스 네이티브 공유. (gift/send, assets/[id], ShareCallToAction 3곳)
- [ ] **P2-2. `navigator.share` else 폴백 없음** — `PostCard.tsx` 죽은 버튼 → 폴백 추가
- [ ] **P2-3. 외부링크 `target="_blank"`** — 토스 `openExternalBrowser` 위임 또는 앱스킴 (convert/result, export/result)
- [ ] **P2-4. export/convert의 YouTube 계정 OAuth** — P0-1과 동일하게 막힘 → 외부 브라우저 위임 또는 토스 로그인 후 BE OAuth

### ⚪ P3 — 위생
- [ ] **P3-1. 무한 리다이렉트 루프** — 세션 수립되면 자연 해소. `location.assign`→`location.replace`로 history 오염 방지
- [ ] **P3-2. viewport meta 부재** — `layout.tsx`에 `viewport = { width:'device-width', initialScale:1, viewportFit:'cover' }` + safe-area 패딩

---

## 6. "지금 코드로 준비 가능" vs "토스 파트너십/계정 발급 후 가능"

### ✅ 지금 코드로 준비 가능 (파트너십 불필요, 선행 가능)
- [ ] WebView/토스 환경 감지 유틸 신설 (`src/lib/env.ts` 확장) — 모든 분기의 전제
- [ ] Next SSR → CSR/정적 재구성 (SSR 금지 대응)
- [ ] `TrackEmbed` playsinline/폴백, 클립보드 execCommand 폴백, navigator.share 폴백, viewport export
- [ ] 라이트모드 강제, 네비바 규격 정리, 외부링크 분기 골격
- [ ] `@apps-in-toss/web-framework` 설치 + `granite.config.ts` + SDK 2.x 마이그레이션 골격
- [ ] 토스 로그인 **서버 토큰교환 핸들러** 골격 작성(클라 `appLogin` 분기 포함) — 단 실제 동작은 아래 필요

### 🔒 토스 파트너십/계정 발급 후 가능
- [ ] **사업자 등록** (토스 로그인·결제·프로모션 선행 조건)
- [ ] **앱인토스 콘솔 워크스페이스 + 앱 등록** → partner 자격증명(토큰교환 엔드포인트 인증값)
- [ ] **토스 로그인 실연동** (서버 토큰교환은 partner 자격 필요 — 골격은 미리, 실값은 발급 후)
- [ ] `*.apps.tossmini.com` 호스팅/도메인 배정
- [ ] 인앱결제(IAP) 실연동 (유료화 시)
- [ ] 검수 요청/통과

### ❓ 미확인 (콘솔/채널톡 문의)
- 순수 비로그인(게스트 열람) 미니앱 허용 여부 (gift-first 비로그인 동선)
- YouTube API-key resolve(외부 fetch)의 검수 통과 여부
- 토스페이먼츠 일반 가맹 vs 앱인토스 IAP 경계

---

### 주요 근거 파일 (절대경로)
- `/Users/hanyoung-jeong/Development/plshare_2/frontend/src/app/auth/continue/page.tsx` (Google hop 분기)
- `/Users/hanyoung-jeong/Development/plshare_2/frontend/src/app/api/auth/google/start/route.ts`
- `/Users/hanyoung-jeong/Development/plshare_2/frontend/src/middleware.ts`
- `/Users/hanyoung-jeong/Development/plshare_2/frontend/src/lib/auth/session.ts` (SameSite=Lax)
- `/Users/hanyoung-jeong/Development/plshare_2/frontend/src/app/api/backend/[...path]/route.ts`
- `/Users/hanyoung-jeong/Development/plshare_2/frontend/src/components/gift/TrackEmbed.tsx`
- `/Users/hanyoung-jeong/Development/plshare_2/frontend/src/app/layout.tsx` (viewport)
