# plshare2 Demo Guide v0.1

- Date: `2026-05-03`
- Scope: 로컬에서 데모 실행 가능한 MVP 초안

---

## 1. 데모 실행

### 1.1 백엔드 (Spring Boot + Kotlin, H2 인메모리)

터미널 A:
```zsh
cd /Users/hanyoung-jeong/Development/plshare_2/backend
./gradlew bootRun --args='--spring.profiles.active=demo'
```
- 포트: `8080`
- DB: H2 인메모리 (`jdbc:h2:mem:plshare`, console at `http://localhost:8080/h2-console`)
- 부팅 시 `Sample - Sunset Walks` Asset 1개 자동 시드
- 외부 자격증명 불필요. Mock Spotify/Apple 어댑터 사용

### 1.2 프론트엔드 (Next.js App Router)

터미널 B:
```zsh
cd /Users/hanyoung-jeong/Development/plshare_2/frontend
npm install   # 최초 1회
npm run dev
```
- 포트: `3000`
- 환경변수 `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080` (`.env.local`)
- 백엔드 미가동 시 자동 fixture 폴백 (UI에 "Demo data" 배지 노출)

### 1.3 정리

```zsh
# 백엔드 종료
pkill -f "BackendApplicationKt"
```

---

## 2. 데모 시나리오

브라우저에서 `http://localhost:3000` 접속:

| 단계 | 화면 | 액션 | 검증 포인트 |
|:---|:---|:---|:---|
| 1 | `/` 랜딩 | "Spotify로 시작하기" 클릭 | Refined 톤, 1 CTA 원칙 |
| 2 | `/auth/spotify` | 자동 진행 (1.5s) | Mock OAuth 페이드 |
| 3 | `/import` | "Late Night Drives" 선택 | 3개 mock 플레이리스트 표시 |
| 4 | `/import/.../progress` | 자동 폴링 → 완료 | ProgressNarrative (no spinner) |
| 5 | `/assets/{id}` | diary + 감정 태그 입력 → 저장 | 8개 태그 multiselect |
| 6 | `/assets/{id}` | "Apple Music으로 내보내기" | |
| 7 | `/assets/.../export` | 매핑 결과 (5/6 매칭) | 자동 polling |
| 8 | `/assets/.../export/result` | "Apple Music에서 열기" | externalUrl 노출 |
| 9 | `/assets/{id}` | "공유 링크 만들기" → 클립보드 복사 | shareToken 발급 |
| 10 | `/share/{token}` | 인증 없이 접근 가능 | Public 감상 페이지 |

---

## 3. API 검증 (curl)

### 3.1 Import 플로우

```zsh
# Spotify 플레이리스트 목록
curl -s http://localhost:8080/api/playlists | python3 -m json.tool

# Import 요청 (멱등성 키 필수)
JOB=$(curl -s -X POST http://localhost:8080/api/imports \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: demo-import-1" \
  -d '{"playlistId":"pl-late-night"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['jobId'])")

# 진행 상태 폴링
curl -s "http://localhost:8080/api/imports/$JOB" | python3 -m json.tool
# → status: completed, assetId: <UUID>
```

### 3.2 Emotional Context 부착

```zsh
ASSET=<assetId from import>

curl -s -X PATCH "http://localhost:8080/api/assets/$ASSET" \
  -H "Content-Type: application/json" \
  -d '{"diaryText":"새벽 4시, 텅 빈 도로","emotionTags":["새벽","드라이브"]}'
```

### 3.3 Apple Music Export

```zsh
EXP=$(curl -s -X POST http://localhost:8080/api/exports \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: demo-export-1" \
  -d "{\"assetId\":\"$ASSET\",\"targetPlatform\":\"apple\"}" \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['jobId'])")

sleep 3

curl -s "http://localhost:8080/api/exports/$EXP/result" | python3 -m json.tool
# → matchedTracks: 5, failedTracks: 1, externalUrl: https://music.apple.com/...
```

### 3.4 Share

```zsh
SHARE=$(curl -s -X POST "http://localhost:8080/api/assets/$ASSET/share" \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['shareToken'])")

curl -s "http://localhost:8080/api/share/$SHARE" | python3 -m json.tool
```

---

## 4. 검증된 결정사항

### 백엔드
- **SpotifyClient 인터페이스화**: `RealSpotifyClient(@Profile("!demo"))` + `MockSpotifyClient(@Profile("demo") @Primary)`로 데모/프로덕션 어댑터 분리
- **Emotional Context 인라인**: Asset 엔티티에 `emotionTags` / `photoUrls` (`@ElementCollection`) 추가, 별도 엔티티 미생성
- **Spring Security permitAll** (데모 한정): `SecurityConfig`에서 모든 요청 통과, CSRF off, H2 frame 허용
- **CORS**: `WebMvcConfigurer`로 `localhost:3000` + `X-Idempotency-Key` 헤더 명시 허용
- **Async**: `@EnableAsync` + `AsyncConfig` ThreadPool 빈 (cores 4, queue 100)
- **Export Job 상태**: queued → matching → ready → executing → completed / partial / failed

### 프론트엔드
- **ProgressNarrative**: spinner/shimmer 금지. "플레이리스트를 읽고 있어요…" 같은 문장형 cycle (Refined 톤)
- **Fixture 폴백**: 모든 fetch catch에서 demo 데이터로 폴백 + "Demo data" 배지. BE 미가동에서도 9개 라우트 데모 가능
- **Idempotency 자동 헤더**: `apiFetch`가 `idempotencyKey` 옵션 받으면 `X-Idempotency-Key` 자동 부착, BE 멱등성 계약 일치

---

## 5. 알려진 한계 (P1으로 이월)

| 항목 | 현재 상태 | P1 작업 |
|:---|:---|:---|
| Spotify OAuth | Mock | 실제 PKCE 플로우 + 토큰 갱신 |
| Apple MusicKit | Mock + 랜덤 매칭 | Developer Token + User Token 통합 |
| 트랙 매칭 알고리즘 | mock ISRC 기반 | 실제 fuzzy(title+artist+duration) |
| 사용자 인증 | 없음 | NextAuth + Spring Security JWT |
| 공유 페이지 SEO | 미적용 | OpenGraph + 정적 ISR |
| Persistence | H2 인메모리 | PostgreSQL prod profile (이미 yml 분리됨) |

---

## 6. 다음 태스크 후보

1. `be-spotify-oauth-001` — Spotify PKCE OAuth 플로우 구현 (Real Adapter)
2. `be-apple-musickit-001` — Apple MusicKit Developer/User token 발급
3. `be-matching-engine-001` — 정규화 엔진 fuzzy 매칭 알고리즘 강화
4. `fe-auth-flow-001` — NextAuth 도입 + 로그인/세션 화면
5. `fe-share-page-seo-001` — 공유 페이지 OpenGraph + 미디어 카드
6. `ops-docker-compose-001` — Postgres + 백엔드 + 프론트 통합 docker-compose
