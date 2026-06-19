# AGENTS.md

Codex가 이 저장소에서 작업할 때 읽는 컨텍스트 파일이다.

---

## 프로젝트

**plshare2** — Taste Asset Management Layer

음악 취향을 감정 컨텍스트와 함께 YouTube 기반 자산으로 만들고
선물·공유하는 소셜 서비스.

---

## 현재 제품 범위

- **P0:** Google identity, YouTube 기반 취향 자산 생성, Emotional Context,
  gift/unboxing, social, share surfaces
- **P1:** YouTube 계정으로 내보내기
- Spotify·Apple 계정 연동과 교차 플랫폼 변환은 코드만 보존하며 feature flag OFF
- Direct messages, B2B 데이터 패키징, 온체인 민팅은 MVP 범위 외
- In-app full music player 없음 — YouTube embed와 link만 제공

제품 기준: `docs/20-product/strategy/product-baseline-v2.1.md`

---

## 기술 스택

| 레이어 | 기술 |
|--------|------|
| Frontend | Next.js App Router (TypeScript) |
| Backend | Kotlin + Spring Boot |
| DB | PostgreSQL + Flyway (demo: H2) |
| Auth | Google OAuth + Spring Security application session |
| Storage | S3-compatible storage (demo: local filesystem) |
| Infra | Vercel (FE), Cloud Run (BE) |

---

## Codex의 역할

Backend Engineer 또는 Frontend Engineer 태스크를 담당한다.

담당 범위:
- Kotlin/Spring Boot API 구현
- Next.js 컴포넌트 및 페이지 구현
- Supabase 연동 코드
- 테스트 작성 및 실행
- 아키텍처 설계 문서 작성 (`docs/20-product/data/`)

---

## 작업 규칙

1. **briefing 파일**을 반드시 읽고 시작한다 — `briefing_path` 참조
2. **done_criteria**를 모두 충족해야 작업 완료
3. **artifact_paths**에 지정된 경로에 결과물을 직접 작성
4. **locked_files**는 다른 태스크와 동시에 수정하지 않는다
5. 기획/UX/범위 변경은 직접 결정하지 않는다 — log 파일 끝에 `BLOCKING_QUESTION: <질문>` 형식으로 기록

---

## API 계약 (확정)

YouTube 기반 경로만 활성화한다. 아래 계약의 Spotify source와 Apple target은
보존된 어댑터 호환용이며 feature flag OFF 상태를 유지한다.

### Import
```
POST /api/imports
Body: { playlistId: String, sourcePlatform: "spotify" | "youtube" }
Response: { jobId: UUID, status: Enum }
```

### Export
```
POST /api/exports
Body: { assetId: UUID, targetPlatform: "youtube" | "apple" }
Response: { jobId: UUID }
```

### 잡 상태 조회
```
GET /api/imports/{jobId}
GET /api/exports/{jobId}
Response: { status, progress, results }
```

전체 API 계약: `docs/20-product/delivery/implementation/open-issues.md`

---

## 핵심 도메인 객체

- `PlaylistAsset` — 사용자의 확정된 자기표현 자산
- `CanonicalTrack` — ISRC 기반 플랫폼 독립 트랙 정체성
- `TrackMapping` — 플랫폼별 ID 매핑 (spotify_id, apple_music_id)
- `ImportJob` / `ExportJob` — 비동기 처리 잡

---

## 결과물 경로

| 종류 | 경로 |
|------|------|
| 아키텍처 문서 | `docs/20-product/data/` |
| 구현 결정 기록 | `docs/20-product/delivery/implementation/decision-log.md` |
| 오픈 이슈 | `docs/20-product/delivery/implementation/open-issues.md` |
| 백엔드 코드 | `backend/src/` |
| 프론트엔드 코드 | `frontend/src/` |
