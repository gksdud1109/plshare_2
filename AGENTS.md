# AGENTS.md

Codex가 이 저장소에서 작업할 때 읽는 컨텍스트 파일이다.

---

## 프로젝트

**plshare2** — Taste Asset Management Layer

Spotify 플레이리스트를 감정 컨텍스트(일기, 태그, 사진)와 함께 플랫폼 독립 자산으로 보존하고
Apple Music으로 이식 가능하게 하는 서비스.

---

## MVP 범위 (절대 변경 금지)

- **P0:** `Spotify import → Assetize (컨텍스트 추가) → Apple Music export`
- B2B 데이터 패키징은 MVP 범위 외
- In-app music player 없음 — deep link만 제공
- YouTube 어댑터는 MVP 대상 아님
- 온체인 민팅 없음 — 오프체인 자산 등록만

---

## 기술 스택

| 레이어 | 기술 |
|--------|------|
| Frontend | Next.js App Router (TypeScript) |
| Backend | Kotlin + Spring Boot |
| DB | Supabase (PostgreSQL) |
| Auth | Supabase Auth + Spring Security |
| Storage | Supabase Storage (cover images) |
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

### Import (Spotify)
```
POST /api/v1/imports
Body: { spotify_playlist_id: String }
Response: { import_job_id: UUID, status: Enum }
```

### Export (Apple Music)
```
POST /api/v1/exports
Body: { asset_id: UUID, target_platform: "apple_music" }
Response: { export_job_id: UUID }
```

### 잡 상태 조회
```
GET /api/v1/imports/{import_job_id}
GET /api/v1/exports/{export_job_id}
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
