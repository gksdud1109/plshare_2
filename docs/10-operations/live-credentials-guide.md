# Live Credentials Guide

> 대상: 실 자격증명으로 backend를 처음 실행하는 개발자/운영자.
> **demo 프로파일(자격증명 0개)로도 앱은 완전히 동작한다.** 이 가이드는 실제 Spotify/Apple MusicKit OAuth를 사용하려는 경우에만 필요하다.

---

## §1 Spotify

### 1-1. developer.spotify.com 앱 생성

1. [developer.spotify.com/dashboard](https://developer.spotify.com/dashboard) 로그인
2. **Create app** 클릭
3. 앱 이름/설명 자유 입력; Redirect URIs 항목에 아래 URI 정확히 입력:

   ```
   http://localhost:8080/api/auth/spotify/callback
   ```

   > 소스 기준 (`AuthController.kt:27`, `application-prod.yml:23`):
   > 기본값 `${SPOTIFY_REDIRECT_URI:http://localhost:8080/api/auth/spotify/callback}`
   > — 별도 env를 주입하지 않으면 이 값이 사용된다.

4. **APIs used**: "Web API" 체크 → 저장
5. 앱 대시보드 → **Settings** → **Client ID**와 **Client Secret** 복사

### 1-2. env 주입

```bash
export SPOTIFY_CLIENT_ID="<위에서 복사한 Client ID>"
export SPOTIFY_CLIENT_SECRET="<위에서 복사한 Client Secret>"
# redirect URI를 기본값에서 바꾸고 싶을 때만:
# export SPOTIFY_REDIRECT_URI="http://localhost:8080/api/auth/spotify/callback"
```

또는 프로젝트 루트의 `.env` 파일에 저장 (`.gitignore`로 보호됨 — §5 참고).

### 1-3. prod 프로파일로 백엔드 실행

```bash
cd backend
SPRING_PROFILES_ACTIVE=prod \
  SPOTIFY_CLIENT_ID="$SPOTIFY_CLIENT_ID" \
  SPOTIFY_CLIENT_SECRET="$SPOTIFY_CLIENT_SECRET" \
  ./gradlew bootRun
```

또는 JAR 빌드 후 실행:

```bash
./gradlew bootJar
SPRING_PROFILES_ACTIVE=prod \
  SPOTIFY_CLIENT_ID="$SPOTIFY_CLIENT_ID" \
  SPOTIFY_CLIENT_SECRET="$SPOTIFY_CLIENT_SECRET" \
  java -jar build/libs/backend-*.jar
```

`prod` 프로파일은 `application-prod.yml`을 로드한다. 해당 파일에는 `spotify.client-id: ${SPOTIFY_CLIENT_ID}` (기본값 없음) — **이 env가 없으면 앱 시작이 실패한다.**

### 1-4. 프론트엔드에서 `?live` 파라미터로 실모드 진입

`frontend/src/app/auth/spotify/page.tsx:21-23` 분기 로직:

```
localhost/127.0.0.1 + ?live 파라미터 → 실 OAuth 흐름 진입
localhost/127.0.0.1 + ?live 없음     → demo 세션 생성
비-localhost (배포 환경)              → 항상 실 OAuth 흐름
```

로컬에서 실 흐름을 테스트하려면:

```
http://localhost:3000/auth/spotify?live
```

1500ms 후 `http://localhost:8080/api/auth/spotify/start` 로 리다이렉트된다.

### 1-5. 검증 체크리스트

| 단계 | 기대 동작 | 확인 방법 |
|------|-----------|-----------|
| `/api/auth/spotify/start` | 302 → `accounts.spotify.com/authorize?…` | `curl -I http://localhost:8080/api/auth/spotify/start` |
| Spotify 동의 화면 | 앱 이름과 scopes(`playlist-read-private`, `playlist-read-collaborative`) 표시 | 브라우저 확인 |
| callback 수신 | `?code=…&state=…` 로 `/api/auth/spotify/callback` 도착 | 백엔드 로그 확인 |
| grant 발급 | `302 → http://localhost:3000/import?session=<UUID>` | 브라우저 리다이렉트 확인 |
| `/api/auth/spotify/me` | `200 {"grantId": "…", "expiresAt": "…"}` | `curl "http://localhost:8080/api/auth/spotify/me?grantId=<UUID>"` |

> scopes 출처: `application-prod.yml:24` — `playlist-read-private,playlist-read-collaborative`
> (`RealSpotifyClient.kt`의 `buildAuthorizationUrl`에서 공백으로 join하여 전달)

---

## §2 Apple MusicKit

### 2-1. Apple Developer 콘솔에서 키 발급

**MusicKit 전용 Media Services 키**를 발급한다 (Sign In with Apple 키와 다름).

1. [developer.apple.com/account/resources/authkeys/list](https://developer.apple.com/account/resources/authkeys/list) → **Create a Key**
2. Key Name 자유 입력; **Media Services (MusicKit)** 체크
3. **Continue** → **Register** → **Download** (.p8 파일) — **1회만 다운로드 가능**
4. 다운로드 후 Key ID 복사 (예: `ABCD123456`)
5. 팀 대시보드 우상단 → **Membership details** → **Team ID** 복사 (예: `TEAMID1234`)

### 2-2. .p8 파일을 env 형식으로 변환

Apple이 제공하는 `.p8` 파일은 PEM 포맷이다. env로 주입할 때는 개행을 `\n`으로 치환한다:

```bash
# macOS
APPLE_PRIVATE_KEY_PEM=$(awk 'NF{printf "%s\\n", $0}' AuthKey_ABCD123456.p8)
echo "$APPLE_PRIVATE_KEY_PEM"
```

`AppleDeveloperTokenProvider.kt:74`에서 `\\n` → `\n` 치환 후 PEM 파싱을 수행하므로, 개행 이스케이프된 형태로 주입해도 정상 동작한다.

### 2-3. env 주입

```bash
export APPLE_TEAM_ID="TEAMID1234"
export APPLE_KEY_ID="ABCD123456"
export APPLE_PRIVATE_KEY_PEM="$(awk 'NF{printf "%s\\n", $0}' AuthKey_ABCD123456.p8)"
```

### 2-4. 토큰 캐싱 및 예외 동작

`AppleDeveloperTokenProvider.kt` 기준:

| 동작 | 세부 내용 |
|------|-----------|
| 토큰 수명 | 180일 (Apple 정책 최대치) |
| 캐시 갱신 기준 | 만료 **30분 전** 자동 재서명 (`REFRESH_BEFORE = 30m`) |
| 캐시 저장소 | `AtomicReference` (JVM 인스턴스 내 인메모리, 재시작 시 초기화) |
| 시작 시 동작 | 필드 누락이어도 앱 기동은 성공, 첫 호출 시 `IllegalStateException("Apple credentials not configured")` 발생 |
| PEM 허용 형식 | `BEGIN PRIVATE KEY` / `BEGIN EC PRIVATE KEY` 둘 다 허용 |
| 적용 프로파일 | `@Profile("!demo")` — demo 프로파일에서는 이 빈이 생성되지 않는다 |

> 설정 바인딩 prefix: `apple-music` (`AppleMusicConfig.kt:13`)
> env 키: `APPLE_TEAM_ID` → `apple-music.team-id`, `APPLE_KEY_ID` → `apple-music.key-id`, `APPLE_PRIVATE_KEY_PEM` → `apple-music.private-key-pem`

---

## §3 Google 로그인 (소셜로그인) — 연동 준비 완료

> 코드/설정은 이미 배선돼 있다. **실 자격증명만 발급해 env 에 넣으면 동작한다.**
> 데모(localhost)는 자격증명 없이 `/auth/continue` 게이트가 원클릭 데모 세션으로 진입하므로
> 이 섹션은 **실 Google 로그인을 켤 때만** 필요하다.

### 발급 절차

1. [console.cloud.google.com](https://console.cloud.google.com) → 프로젝트 선택 → **APIs & Services → Credentials → Create Credentials → OAuth 2.0 Client ID**
2. Application type: **Web application**
3. **Authorized redirect URIs** (서버사이드 콜백 — BE 가 받는다):
   - 로컬: `http://localhost:8080/api/auth/google/callback`
   - 배포: `https://<api-도메인>/api/auth/google/callback`
   - (Authorized JavaScript origins 는 서버사이드 흐름이라 불필요)
4. **OAuth consent screen**: 외부(External) + 테스트 사용자 등록(미검증 앱은 ≤100 테스트 유저). 로그인 전용 스코프는 기본(`openid`, `email`, `profile`). YouTube 내보내기(점진 동의)는 `scope=youtube` 파라미터가 붙을 때만 추가 요청된다.
5. 발급된 **Client ID / Client Secret** 를 env 로 주입:

```bash
GOOGLE_CLIENT_ID=<client-id>.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=<client-secret>
# 콘솔의 redirect URI 와 정확히 일치해야 함(기본값과 다를 때만 지정):
GOOGLE_REDIRECT_URI=http://localhost:8080/api/auth/google/callback
```

키 이름은 `application-prod.yml`의 `google.client-id / client-secret / redirect-uri`(=`${GOOGLE_CLIENT_ID}` 등)와 `.env.example`(§ Google OAuth)에 이미 정의돼 있다.

### 흐름 (코드 기준)

- 진입: 어디서든 로그인 → `/auth/continue?returnTo=<경로>` 게이트.
  - **localhost & `?live` 없음** → 외부 화면 없이 `POST /api/auth/session`(데모 세션 쿠키) → `returnTo` 복귀.
  - **비-localhost 또는 `?live`** → BFF `/api/auth/google/start` → BE 가 Google 동의화면으로 302 → 콜백 `/api/auth/google/callback` → BE 가 user upsert(googleSubject 기준) + 서명 세션 토큰 발급 → `returnTo` 복귀.
- **로컬에서 실 흐름 테스트**: `http://localhost:3000/auth/continue?returnTo=/feed&live=1` (env 에 실 키가 있어야 함).
- demo 프로파일에선 `google.client-id=""`(application-demo.yml)이라 BFF start 가 502 를 반환한다 — 그래서 데모는 게이트의 원클릭 경로만 쓴다.

---

## §4 로컬 실행 매트릭스

### 프로파일별 필수/선택 env

| 환경 변수 | demo 프로파일 | prod 프로파일 |
|-----------|--------------|--------------|
| `SPOTIFY_CLIENT_ID` | 불필요 (mock 사용) | **필수** |
| `SPOTIFY_CLIENT_SECRET` | 불필요 | **필수** |
| `SPOTIFY_REDIRECT_URI` | 불필요 | 선택 (기본값: `http://localhost:8080/api/auth/spotify/callback`) |
| `FE_REDIRECT_URL` | 불필요 | 선택 (기본값: `http://localhost:3000/import`) |
| `APPLE_TEAM_ID` | 불필요 | 선택 (없으면 Apple MusicKit 기능만 비활성) |
| `APPLE_KEY_ID` | 불필요 | 선택 |
| `APPLE_PRIVATE_KEY_PEM` | 불필요 | 선택 |
| `APPLE_STOREFRONT` | 불필요 | 선택 (기본값: `us`) |
| `DATABASE_URL` | 불필요 (H2 인메모리) | 선택 (기본값: `jdbc:postgresql://localhost:5432/plshare`) |
| `DATABASE_USERNAME` | 불필요 | 선택 (기본값: `plshare`) |
| `DATABASE_PASSWORD` | 불필요 | 선택 (기본값: `plshare`) |
| `S3_BUCKET` | 불필요 | 선택 (기본값: `plshare-photos`) |
| `S3_ENDPOINT` | 불필요 | 선택 |
| `AWS_REGION` | 불필요 | 선택 (기본값: `us-east-1`) |
| `AWS_ACCESS_KEY_ID` | 불필요 | 선택 |
| `AWS_SECRET_ACCESS_KEY` | 불필요 | 선택 |
| `S3_PUBLIC_BASE_URL` | 불필요 | 선택 |
| `NEXT_PUBLIC_API_BASE_URL` | 불필요 | 선택 (기본값: `http://localhost:8080`) |

> demo 프로파일: `application-demo.yml` — H2 인메모리 DB, Mock Spotify/Apple 어댑터, flyway 비활성.
> prod 프로파일: `application-prod.yml` — PostgreSQL, 실 어댑터, flyway 활성.

### docker compose prod 블록 활성 절차

`docker-compose.yml` 하단 주석 블록에 절차가 명시되어 있다:

1. `docker-compose.yml` 내 `postgres` 서비스 블록 주석 해제
2. `backend.environment` 섹션을 아래로 교체:

   ```yaml
   SPRING_PROFILES_ACTIVE: prod
   DATABASE_URL: jdbc:postgresql://postgres:5432/plshare
   DATABASE_USERNAME: plshare
   DATABASE_PASSWORD: plshare
   SPOTIFY_CLIENT_ID: ${SPOTIFY_CLIENT_ID:-}
   SPOTIFY_CLIENT_SECRET: ${SPOTIFY_CLIENT_SECRET:-}
   APPLE_TEAM_ID: ${APPLE_TEAM_ID:-}
   APPLE_KEY_ID: ${APPLE_KEY_ID:-}
   APPLE_PRIVATE_KEY_PEM: ${APPLE_PRIVATE_KEY_PEM:-}
   ```

3. `backend.depends_on`에 `postgres: condition: service_healthy` 추가
4. 맨 아래 `volumes` 블록(`plshare-pg-data`) 주석 해제
5. 실행 전 쉘 env에 Spotify/Apple 자격증명 export (또는 `.env` 파일 사용)
6. `docker compose up --build`

---

## §5 보안 수칙

### .env 커밋 금지

`.gitignore` (리포 루트)에 다음 규칙이 이미 포함되어 있다:

```
.env
.env.*
!.env.example
!**/.env.example
```

- `.env` — 절대 커밋되지 않음
- `.env.example` — 빈 값 템플릿, 커밋 대상 (실 키 값 없음)

`git status`로 `.env`가 추적되고 있지 않은지 항상 확인:

```bash
git status --short | grep '\.env'
# 출력이 없어야 정상
```

### 키 로테이션

| 서비스 | 로테이션 방법 | 비고 |
|--------|--------------|------|
| Spotify | 대시보드 → Settings → **Rotate Secret** | 기존 refresh_token은 그대로 사용 가능 |
| Apple MusicKit | 기존 키 폐기 → 새 .p8 발급 → `APPLE_KEY_ID`/`APPLE_PRIVATE_KEY_PEM` 업데이트 | JVM 재시작 시 새 키로 토큰 재서명 |

### 데모/실 키 분리

- **절대 demo 환경에 실 Client ID/Secret을 주입하지 않는다.** demo 프로파일은 Mock 어댑터를 사용하므로 실 키가 불필요하다.
- CI/CD 파이프라인에서는 데모 빌드와 prod 빌드를 별도 Secret으로 관리한다.
- `SPOTIFY_CLIENT_SECRET`은 로그에 노출되지 않도록 주의 (Spring Boot auto-config은 `password`/`secret` 패턴 키를 마스킹하지만, 커스텀 env 키는 별도 마스킹 필요).
