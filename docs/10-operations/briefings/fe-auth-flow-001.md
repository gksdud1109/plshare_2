# Briefing — fe-auth-flow-001

## Role
Frontend Engineer (Next.js 16 App Router)

## Task
사용자 인증 + 세션 UI 구현. 단, **NextAuth 라이브러리를 설치하지 말 것.**

## 왜 NextAuth를 쓰지 않는가 (중요)
백엔드가 이미 서버사이드 OAuth 핸드셰이크를 소유한다:
- `GET http://localhost:8080/api/auth/spotify/start` → 302 redirect to Spotify authorize
- `GET http://localhost:8080/api/auth/spotify/callback?code=&state=` → 토큰 교환 후 FE로 302 redirect: `http://localhost:3000/import?session=<grantId>`
- `POST http://localhost:8080/api/auth/spotify/refresh` body `{grantId}` → 갱신
- `GET http://localhost:8080/api/auth/spotify/me?grantId=<uuid>` → grant 상태 `{grantId, userId, expiresAt, expiringSoon, scope}`

NextAuth를 깔면 이 핸드셰이크와 중복·충돌한다. 대신 **BE가 발급한 `grantId`를 secure httpOnly 쿠키에 담는 경량 세션 레이어**를 만든다.

## 구현 항목 (정확히 이 파일들만)

### 1. `frontend/src/lib/auth/session.ts`
- 세션 모델: `{ grantId: string; userId?: string; demo?: boolean }`
- 쿠키 이름 상수 `plshare_session`
- 서버 유틸 (next/headers `cookies()` 사용):
  - `getSession(): Promise<Session | null>` — 쿠키 파싱
  - `setSessionCookie(session)` / `clearSessionCookie()` — route handler에서 사용
- 쿠키 옵션: `httpOnly: true, sameSite: 'lax', path: '/', secure: process.env.NODE_ENV === 'production', maxAge: 60*60*24*30`

### 2. `frontend/src/app/api/auth/session/route.ts`
- `GET` → 현재 세션 반환 `{ authenticated: boolean, session?: {...} }`. 세션 있으면 BE `/api/auth/spotify/me?grantId=`로 검증 시도(실패해도 demo 세션이면 그대로 authenticated:true).

### 3. `frontend/src/app/api/auth/callback/route.ts`
- `GET ?session=<grantId>` → grantId를 세션 쿠키에 저장하고 `/import`로 redirect.
- BE 콜백이 `/import?session=<grantId>`로 보내므로, **대안**: 아래 4의 미들웨어가 `/import?session=`을 가로채 이 route로 위임하거나, `/import` 페이지가 마운트 시 `?session=` 쿼리를 잡아 `POST /api/auth/session`으로 저장하게 해도 됨. 가장 단순한 방식 택일 후 주석으로 설명.

### 4. `frontend/src/app/api/auth/logout/route.ts`
- `POST` → 세션 쿠키 삭제, `{ ok: true }` 반환

### 5. `frontend/src/middleware.ts`
- 보호 라우트: `/assets`, `/assets/:path*`, `/import`, `/import/:path*`
- 세션 쿠키 없으면 → `/auth/spotify`로 redirect (원래 목적지를 `?next=` 쿼리로 보존)
- `matcher`로 위 경로만 매칭. `/`, `/auth/spotify`, `/share/:path*`, `/api/:path*`, static asset은 제외.

### 6. `frontend/src/app/(auth)/` — 라우트 그룹
- 기존 `frontend/src/app/auth/spotify/page.tsx`를 이 그룹으로 옮기거나, 새 `(auth)/layout.tsx`만 추가 (인증 화면 공통 셸). **기존 `/auth/spotify` URL은 유지돼야 한다** (E2E가 사용). 라우트 그룹 `(auth)`는 URL에 영향 없으므로 안전.

### 7. Header 로그인 상태 표시
- `frontend/src/components/ui/SessionBadge.tsx` (client component)
- `/api/auth/session` fetch → authenticated면 "연결됨" + 로그아웃 버튼, 아니면 "Spotify 연결" 링크(→ `/auth/spotify`)
- 기존 `PageShell.tsx`의 헤더(LIBRARY 링크 옆)에 끼워넣기. PageShell은 최소 수정.

## E2E 보존 (절대 깨면 안 됨)
기존 Playwright E2E(`frontend/e2e/demo-flow.spec.mjs`)는 세션 셋업 없이 이렇게 흐른다:
`/` → "Spotify로 시작하기" 클릭 → `/auth/spotify` → (1.5s 후 자동) `/import` → 플레이리스트 선택 → `/assets/[id]` → export → share.

따라서 **`/auth/spotify` mock 페이지가 `/import`로 redirect하기 직전에 데모 세션을 반드시 설정해야 한다.** 그래야 미들웨어가 보호 라우트(`/import`, `/assets`)에서 redirect-loop를 일으키지 않는다.

- `frontend/src/app/auth/spotify/page.tsx` 수정: 마운트 시 `POST /api/auth/session`(또는 전용 `/api/auth/demo-session`)으로 `{ demo: true, grantId: 'demo-grant' }` 세션을 설정 → 그 다음 `/import`로 router.push. (현재 1.5초 지연 연출은 유지)
- 데모 세션 설정은 BE 없이도 동작해야 함 (route handler가 쿠키만 셋팅).

검증: BE를 demo 프로파일로, FE를 prod build로 띄우고 `node e2e/demo-flow.spec.mjs` → **16/16 PASS 유지가 완료 조건.**

## 절대 금지
- `backend/` 수정 금지
- NextAuth / next-auth / @auth/* 등 인증 라이브러리 설치 금지 (package.json 변경 최소화, 추가 dependency 없이)
- `frontend/e2e/` 수정 금지 (테스트를 바꿔서 통과시키지 말 것)
- 기존 라우트 URL 변경 금지 (`/auth/spotify`, `/import`, `/assets/...` 그대로)
- demo 플로우 회귀 금지

## Done Criteria
- Spotify 로그인/로그아웃 플로우 (BE start/callback 연동 + logout route)
- secure httpOnly 쿠키 세션 영속화
- 보호 라우트가 세션 없을 때 `/auth/spotify`로 redirect
- 헤더가 로그인 상태 표시
- `npm run build` + `npx tsc --noEmit` + `npm run lint` 모두 통과
- 기존 E2E 16/16 유지

## 작업 디렉토리
`frontend/` 안에서만 작업. 백엔드 컨트랙트는 위에 명시된 엔드포인트를 신뢰하고 사용.
