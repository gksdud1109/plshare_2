# Adapter Architecture v0.1

## 문서 정보
- Date: `2026-05-03`
- Role: `Backend Engineer`
- Model: `Claude`
- Version: `v0.1`
- Status: `Draft for Review`
- Scope: `MVP - Spotify Read + Apple Music Write asymmetric bridge`
- Related Documents:
  - `docs/20-product/strategy/prd-v0.2.md`
  - `docs/20-product/strategy/product-strategy-v0.1.md`
  - `docs/20-product/strategy/platform-direction-review-v0.1.md`
  - `docs/20-product/requirements/functional-requirements-v0.2.md`
  - `docs/20-product/requirements/non-functional-requirements-v0.2.md`
  - `docs/20-product/data/canonical-track-normalization-v0.1.md`

> **본 문서는 설계 문서이며 코드 구현을 포함하지 않는다.** 실제 백엔드 구현은 본 문서가 사람 검토 승인 후 별도 태스크 (`be-spotify-adapter`, `be-apple-adapter`, `be-export-job-engine` 등)로 분할되어 진행된다.

---

## 1. Executive Summary

plshare2 백엔드는 PRD v0.2와 Platform Direction Review v0.1에 따라 **`Spotify Import → Emotional Context → Apple Music Export`** 의 비대칭 브릿지를 구현한다.

### 1.1 비대칭 구조의 의미
- **Spotify**는 **Read 전용 (Import 소스)** 으로 사용한다. 사용자의 플레이리스트와 트랙 메타데이터를 가져오는 데에만 쓴다.
- **Apple Music**은 **Write 전용 (Export 대상)** 으로 사용한다. 정규화된 Playlist Asset을 사용자의 Apple Music 라이브러리에 새 플레이리스트로 생성한다.
- 동일한 플랫폼이 양방향으로 동작할 필요가 없다는 점이 MVP에서의 핵심 단순화 가정이다. (P1에서 양방향으로 확장 예정)

### 1.2 어댑터 패턴을 채택하는 이유
NFR-031, NFR-033, NFR-071에 명시된 바와 같이:
- 플랫폼별 정책/rate limit/auth 모델이 서로 다르고 빈번하게 변한다.
- 향후 P1에 Apple Read / Spotify Write, P2에 YouTube Music이 추가될 가능성이 있다.
- 특정 어댑터 장애가 전체 자산 보기/편집 기능을 멈추게 해서는 안 된다 (NFR-082).

따라서 도메인 코어는 **`PlaylistReadAdapter`** 와 **`PlaylistWriteAdapter`** 라는 두 개의 추상 인터페이스에만 의존하고, 플랫폼별 구체 구현체는 인프라 레이어에서 주입된다.

### 1.3 핵심 아키텍처 결정 요약
| 결정 | 이유 | 근거 |
| :--- | :--- | :--- |
| ISRC를 primary normalization identifier로 사용 | YouTube `videoId` 기반 정규화는 무결성 위험. ISRC는 IFPI 표준이고 Spotify/Apple 모두 노출 | PRD §10, Platform Review §5.1 |
| Read/Write 인터페이스 분리 | 비대칭 구조를 코드 레벨에서 강제. 어댑터별 권한 범위 최소화 (NFR-032) | NFR-031, NFR-032 |
| Export Job은 Async + 상태 머신 | 외부 API 지연/부분 실패 허용 (NFR-022, NFR-023, FR-073) | FR-073, NFR-072 |
| `X-Idempotency-Key` 헤더 강제 | 중복 export 방지 (FR-074) 및 사용자가 안전하게 재시도 가능 (NFR-023) | FR-074, NFR-023 |
| NormalizationEngine은 Async + Transaction Isolation | 매칭 비용이 높고 외부 lookup 동반 → 응답 차단 회피 + canonical track row 충돌 방지 | NFR-011, NFR-072 |

---

## 2. 시스템 다이어그램

### 2.1 컴포넌트 레벨

```
┌──────────────────────────────────────────────────────────────────────┐
│                         Client (Web / App)                           │
└─────────────────────────────┬────────────────────────────────────────┘
                              │ HTTPS + X-Idempotency-Key
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│                       plshare2 Backend (Spring Boot)                 │
│                                                                      │
│  ┌────────────────┐   ┌──────────────────┐   ┌────────────────────┐ │
│  │  API Layer     │──▶│  Application     │──▶│  Domain Core       │ │
│  │  (REST)        │   │  Services        │   │  (Asset, Track,    │ │
│  └────────────────┘   └──────────────────┘   │   ExportJob)       │ │
│                                │              └────────┬───────────┘ │
│                                ▼                       │             │
│                       ┌──────────────────┐             │             │
│                       │ Job Queue        │             │             │
│                       │ (Async Worker)   │             │             │
│                       └────────┬─────────┘             │             │
│                                │                       │             │
│       ┌────────────────────────┼───────────────────────┘             │
│       ▼                        ▼                       ▼             │
│  ┌────────────┐         ┌──────────────┐       ┌─────────────────┐   │
│  │ Read       │         │ Write        │       │ Normalization   │   │
│  │ Adapter    │         │ Adapter      │       │ Engine          │   │
│  │ Port       │         │ Port         │       │ (Async)         │   │
│  └─────┬──────┘         └──────┬───────┘       └────────┬────────┘   │
└────────┼───────────────────────┼────────────────────────┼────────────┘
         │                       │                        │
         ▼                       ▼                        ▼
   ┌──────────┐           ┌────────────┐          ┌───────────────┐
   │ Spotify  │           │ Apple      │          │ Ledger DB     │
   │ Web API  │           │ Music API  │          │ (canonical    │
   │ (Read)   │           │ (Write)    │          │  tracks,      │
   └──────────┘           └────────────┘          │  mappings)    │
                                                  └───────────────┘
```

### 2.2 데이터 흐름 (Import → Export)

```
[Import]                                                       [Export]
   │                                                              ▲
   ▼                                                              │
SpotifyReadAdapter                                  AppleMusicWriteAdapter
   │ fetchPlaylists/Tracks                                createPlaylist/addTracks
   ▼                                                              ▲
ImportedPlaylist ──▶ NormalizationEngine ──▶ CanonicalTrack ─────┘
                          │ (ISRC 우선)        TrackMapping
                          ▼                   (canonicalId↔appleSongId)
                     Ledger DB
```

---

## 3. Adapter Interface 추상화

도메인 코어는 다음 두 포트에만 의존한다. 모든 플랫폼별 구현체는 이 인터페이스를 구현한다.

### 3.1 `PlaylistReadAdapter` (현재: Spotify / P1: Apple Music / P2: YouTube)

```kotlin
package com.plshare.backend.domain.adapter

interface PlaylistReadAdapter {
    /** 플랫폼 식별자. 예: "spotify", "apple-music". */
    val platform: PlatformId

    /**
     * OAuth 인증 흐름 시작/완료 결과로 받은 토큰을 검증하고
     * 유효한 세션 컨텍스트를 반환한다. 만료된 토큰은 refresh를 시도한다.
     */
    suspend fun authenticate(connection: StreamingConnection): AuthContext

    /**
     * 사용자의 플레이리스트 목록을 페이지네이션으로 조회한다.
     * URL 기반 단일 가져오기에서는 fetchPlaylistByUrl을 사용한다.
     */
    suspend fun fetchPlaylists(
        ctx: AuthContext,
        page: PageCursor? = null
    ): PlaylistPage

    /**
     * 플레이리스트 한 개의 메타데이터(제목/설명/소유자/곡 수)와
     * 트랙 ID 시퀀스를 반환한다. 트랙 본문은 fetchTracks로 분리.
     */
    suspend fun fetchPlaylistByUrl(
        ctx: AuthContext,
        url: String
    ): ExternalPlaylist

    /**
     * 외부 플레이리스트의 트랙 메타데이터를 순서대로 가져온다.
     * 부분 실패는 PartialResult.failed에 누적되며 전체 호출을 중단시키지 않는다.
     */
    suspend fun fetchTracks(
        ctx: AuthContext,
        playlistRef: ExternalPlaylistRef,
        page: PageCursor? = null
    ): PartialResult<ExternalTrack>
}
```

### 3.2 `PlaylistWriteAdapter` (현재: Apple Music / P1: Spotify)

```kotlin
package com.plshare.backend.domain.adapter

interface PlaylistWriteAdapter {
    val platform: PlatformId

    /** Read 어댑터와 동일한 인증 컨트랙트. 단 scope는 write 권한을 포함한다. */
    suspend fun authenticate(connection: StreamingConnection): AuthContext

    /**
     * 대상 플랫폼에 새로운 빈 플레이리스트를 생성한다.
     * 제목/설명만 전송하며, 플랫폼 제약으로 누락되는 필드는 결과에 표시한다 (FR-071).
     */
    suspend fun createPlaylist(
        ctx: AuthContext,
        request: CreatePlaylistRequest
    ): ExternalPlaylistRef

    /**
     * 정규화된 트랙 매핑을 사용해 트랙을 순서대로 추가한다.
     * 누락된 트랙은 AddTracksResult.skipped에 사유와 함께 기록된다 (FR-072).
     */
    suspend fun addTracks(
        ctx: AuthContext,
        target: ExternalPlaylistRef,
        tracks: List<ResolvedExternalTrack>,
        idempotencyKey: IdempotencyKey
    ): AddTracksResult

    /**
     * 생성/추가가 실제 반영되었는지 read-back을 통해 검증한다.
     * 부분 성공/누락 카운트를 반환해 ExportJob 상태 결정에 사용된다.
     */
    suspend fun verify(
        ctx: AuthContext,
        target: ExternalPlaylistRef
    ): VerifyResult
}
```

### 3.3 공용 타입 (요약)

```kotlin
enum class PlatformId { SPOTIFY, APPLE_MUSIC, YOUTUBE_MUSIC /* P2 */ }

data class AuthContext(
    val userId: UserId,
    val platform: PlatformId,
    val accessToken: String,      // 메모리 보관, 로그 금지 (NFR-042)
    val refreshToken: String?,
    val scope: Set<String>,
    val expiresAt: Instant
)

data class PartialResult<T>(
    val items: List<T>,
    val failed: List<FailureRecord>,
    val nextCursor: PageCursor?
)
```

---

## 4. SpotifyReadAdapter 상세

### 4.1 인증
- **Flow:** OAuth 2.0 **Authorization Code with PKCE** (Spotify 권장).
- **Client Secret:** 백엔드에서만 보관하며 클라이언트에 노출 금지 (NFR-042).
- **Token Storage:** access/refresh 토큰은 사용자별로 암호화 저장한다 (NFR-041).
- **Refresh:** access 토큰 만료 60초 전부터 백그라운드 refresh 시도. 실패 시 사용자에게 재연결 유도 (FR-003 수용 기준).

### 4.2 Scope (최소 권한 - NFR-032)
| Scope | 용도 |
| :--- | :--- |
| `playlist-read-private` | 사용자의 비공개 플레이리스트 가져오기 |
| `playlist-read-collaborative` | 협업 플레이리스트 가져오기 |
| `user-read-email` | 계정 식별용 (선택) |

> Write scope (`playlist-modify-*`)는 **요청하지 않는다.** Spotify 쓰기는 P1로 미뤘다.

### 4.3 Rate Limit 처리
- Spotify Web API는 분당 단위의 동적 limit을 사용한다. `429 Too Many Requests` + `Retry-After` 헤더 기반.
- 어댑터는 **token bucket** 방식의 사전 throttling을 적용하고, 429 수신 시 `Retry-After` 만큼 대기 후 재시도한다.
- 동일 사용자에 대해서는 동시 요청을 직렬화하여 quota 폭주를 막는다.

### 4.4 응답 매핑
| Spotify 필드 | 내부 도메인 필드 |
| :--- | :--- |
| `track.id` | `ExternalTrack.platformId` |
| `track.external_ids.isrc` | `ExternalTrack.isrc` (정규화 1순위 키) |
| `track.name` | `ExternalTrack.title` |
| `track.artists[].name` | `ExternalTrack.artists` |
| `track.album.name` | `ExternalTrack.album` |
| `track.duration_ms` | `ExternalTrack.durationMs` |
| `track.is_local`, `track.is_playable` | `ExternalTrack.flags` (지역 제한 fallback에 사용) |

ISRC가 누락된 트랙은 `unmatched` 후보로 분류되며 NormalizationEngine의 fuzzy 경로에 들어간다. 자세한 내용은 `canonical-track-normalization-v0.1.md` §2/§6 참조.

---

## 5. AppleMusicWriteAdapter 상세

### 5.1 인증 (이중 토큰 모델)
Apple Music API는 두 종류의 토큰을 요구한다.

1. **Developer Token (JWT)**
   - Apple Developer 계정에서 발급한 ES256 private key로 서명된 JWT.
   - 백엔드에서 in-memory 캐시 (TTL ≤ 6개월). 자동 회전.
   - 클라이언트에 절대 노출하지 않는다 (NFR-042).
2. **Music User Token**
   - 사용자가 MusicKit JS / native MusicKit으로 동의한 후 발급되는 사용자 토큰.
   - 사용자별로 암호화 저장. 만료 시 재인증 플로우.
3. **요청 헤더 조합**
   - `Authorization: Bearer {developerToken}`
   - `Music-User-Token: {userToken}`

### 5.2 Scope / Capabilities
Apple은 OAuth scope 개념 대신 **MusicKit capabilities**로 권한을 표현한다. MVP에서는:
- 사용자의 라이브러리에 플레이리스트 생성 (`POST /me/library/playlists`)
- 라이브러리 플레이리스트에 트랙 추가 (`POST /me/library/playlists/{id}/tracks`)
- 카탈로그 트랙 lookup (`GET /catalog/{storefront}/songs?filter[isrc]=...`)

읽기 권한 (사용자 플레이리스트 import) 은 **요청하지 않는다.** P1에서 별도 어댑터로 추가.

### 5.3 Rate Limit
- Apple Music API의 공식 분당 quota는 비공개이지만 경험적으로 사용자/앱 단위로 throttling이 작동한다.
- 어댑터는 **adaptive backoff** 적용: 429/503 수신 시 exponential backoff (1s → 2s → 4s → 8s, max 30s, jitter ±20%).
- Storefront mismatch 발생 시 (사용자 storefront ≠ 트랙 카탈로그 storefront) 곡 단위로 skip하고 결과에 사유 기록.

### 5.4 응답 매핑 (Write 결과)
| Apple Music 필드 | 내부 필드 |
| :--- | :--- |
| `data[0].id` (생성 결과) | `ExternalPlaylistRef.platformId` |
| `data[0].attributes.url` | `ExternalPlaylistRef.publicUrl` |
| `relationships.tracks.data[].id` | verify 단계에서 차감 검증 |
| `errors[]` | `AddTracksResult.skipped` 사유 |

### 5.5 ISRC 기반 카탈로그 lookup
Apple은 ISRC로 직접 카탈로그 검색을 지원한다. NormalizationEngine은 canonical track의 ISRC로 Apple `appleSongId`를 lookup하고 `TrackMapping`에 캐시한다. 자세한 내용은 정규화 문서 §8.

---

## 6. Job 상태 머신 (Export Job)

FR-073, FR-074, NFR-022, NFR-023을 만족하는 비동기 export 잡 상태 모델.

### 6.1 상태 전이도

```
        ┌────────┐
        │ queued │
        └───┬────┘
            │ worker pickup
            ▼
       ┌──────────┐    canonical lookup 실패
       │ matching │──────────────────────────▶ ┌────────┐
       └────┬─────┘                            │ failed │
            │ 매칭률 ≥ 임계값                    └────────┘
            ▼
       ┌────────┐    user cancel
       │ ready  │────────────────▶ ┌──────────┐
       └────┬───┘                  │ canceled │
            │ user confirm         └──────────┘
            ▼
     ┌─────────────┐  Apple API 치명 오류
     │  executing  │────────────────────────▶ ┌────────┐
     └──────┬──────┘                          │ failed │
            │                                 └────────┘
            ├─── all tracks ok ──▶ ┌───────────┐
            │                      │ completed │
            │                      └───────────┘
            └─── partial ok ─────▶ ┌──────────────────┐
                                   │ partial-success  │
                                   └──────────────────┘
```

### 6.2 상태 정의 및 전이 조건

| 상태 | 정의 | 진입 조건 | 종료 조건 |
| :--- | :--- | :--- | :--- |
| `queued` | export 요청이 큐에 등록됨 | API에서 export 요청 수신 + idempotency 통과 | 워커가 잡을 pick |
| `matching` | canonical track ↔ Apple appleSongId 매핑 진행 중 | 워커 pickup | 모든 트랙에 대해 mapping 시도 완료 |
| `ready` | 매핑 결과를 사용자에게 미리보기 표시 | matching 완료 + 매칭률 ≥ 임계값 (기본 50%) | 사용자가 confirm 또는 cancel |
| `executing` | Apple Music에 실제 createPlaylist + addTracks 수행 | 사용자 confirm | 모든 batch 완료 또는 치명 오류 |
| `completed` | 모든 트랙이 Apple에 정상 추가됨 | executing 후 verify 결과가 100% match | (terminal) |
| `partial-success` | 일부 트랙만 추가됨 | executing 후 verify 결과가 0 < match < 100% | (terminal) |
| `failed` | 매핑 임계값 미달 또는 Apple API 치명 오류 | matching 임계값 미달 / executing에서 4xx/5xx 누적 한도 초과 | (terminal) |
| `canceled` | 사용자가 ready 단계에서 취소 | 사용자 명시적 취소 | (terminal) |

### 6.3 Retry 정책
- **Transient errors (429, 5xx, network):** exponential backoff (§5.3 동일 패턴), 잡 단위로 최대 3회.
- **Permanent errors (4xx 인증/권한):** 재시도하지 않고 즉시 failed로 전이. 사용자에게 재연결 안내.
- **Track 단위 실패:** 잡을 실패시키지 않는다. `partial-success`로 처리하고 사유 기록 (NFR-022).
- **Idempotency:** 동일 `X-Idempotency-Key`로 재시도 시 기존 잡 상태를 그대로 반환 (executing 도중이면 진행 중 상태 반환). §8 참조.

---

## 7. 실패 처리 전략

### 7.1 트랙 매칭 실패
FR-022, FR-023, NFR-060을 따른다.

| 실패 유형 | 처리 |
| :--- | :--- |
| ISRC 누락 → fuzzy 매칭도 신뢰도 낮음 | 사용자에게 `unmatched`로 표시. **skip** 기본 + **alternative 후보 제안** 옵션 |
| ISRC는 있으나 Apple 카탈로그에 없음 (지역 제한) | `low-confidence` 상태로 alternative 후보 제안 |
| 사용자가 수동 매칭 (P1) | `manual` 상태로 `TrackMapping`에 기록. 신뢰도 1.0 (사용자 의도) |
| 모든 후보 거절 | export에서 해당 트랙 제외. `partial-success` 가능 |

### 7.2 외부 API 실패
| 실패 유형 | 정책 |
| :--- | :--- |
| 429 Rate Limit | `Retry-After` 또는 exponential backoff |
| 5xx | exponential backoff (1s, 2s, 4s, 8s, 16s; max 30s; jitter) |
| 401/403 토큰 만료 | refresh 시도 → 실패 시 즉시 failed로 전이, 사용자에게 재연결 유도 |
| 네트워크 타임아웃 | 잡 단위 retry 한도 내 재시도 |
| 어댑터 자체 장애 | NFR-082에 따라 해당 플랫폼 어댑터만 비활성. 자산 보기/편집은 계속 동작 |

---

## 8. Idempotency 패턴

### 8.1 적용 범위
다음 mutating 엔드포인트는 `X-Idempotency-Key` 헤더를 **필수**로 받는다.

- `POST /imports` (Spotify import 시작)
- `POST /assets` (Playlist Asset 생성)
- `POST /exports` (Apple Music export 시작)
- `POST /exports/{id}/confirm` (ready → executing 전이)

### 8.2 Dedup Window
- 키 형식: 클라이언트 생성 UUID v4 권장. 길이 16~64자 검증.
- 저장: `idempotency_records` 테이블 (key, user_id, request_hash, response_snapshot, created_at).
- **Window: 24시간.** 그 안에 동일 키 + 동일 user_id로 들어온 요청은 저장된 응답을 그대로 반환한다.
- request body가 다르면 `409 Conflict`로 거절 (요청 불일치).
- 24시간 후 키는 만료되어 재사용 가능.

### 8.3 Export 잡과의 결합
Export의 경우 idempotency key는 단순 응답 캐시를 넘어 **잡 자체의 고유 식별자**로도 쓰인다. 동일 키로 재요청 시:
- 잡이 terminal 상태면: 기존 결과 반환.
- 잡이 in-flight면: 현재 진행 상태 반환 (새 잡 생성 금지).

이로써 FR-074의 "직전 내보내기 대상과 시각을 표시" 요구와 NFR-023의 안전한 재시도 요구를 동시에 만족한다.

---

## 9. Observability

NFR-080, NFR-081 충족을 위한 관측 포인트.

### 9.1 구조화 로그 필드 (공통)
모든 어댑터/잡 로그는 다음 필드를 포함한다.
- `traceId`, `spanId` (W3C trace context)
- `userId` (해시)
- `platform` (`spotify` | `apple-music`)
- `adapterOp` (`fetchPlaylists` | `addTracks` | ...)
- `idempotencyKey` (export 관련 시)
- `jobId` (잡 관련 시)
- `outcome` (`ok` | `partial` | `error`)
- `httpStatus`, `errorCode`, `retryCount`, `latencyMs`
- 토큰/시크릿/일기 본문은 **절대 로그에 남기지 않는다** (NFR-041, NFR-042).

### 9.2 메트릭 (RED + 매칭 품질)
| 메트릭 | 차원 | 용도 |
| :--- | :--- | :--- |
| `adapter_request_total` | platform, op, outcome | 호출량/실패율 |
| `adapter_latency_ms` (histogram) | platform, op | p50/p95/p99 |
| `adapter_rate_limit_total` | platform | 429 빈도 |
| `export_job_state_total` | from_state, to_state | 상태 전이 분포 |
| `export_job_duration_ms` | terminal_state | 전체 잡 소요 시간 |
| `match_outcome_total` | source_platform, target_platform, tier (`exact`/`high`/`low`/`unmatched`) | 매칭 품질 (NFR-091) |

### 9.3 Trace Span
- API 요청 진입 → `application.exportJob.create` → `adapter.apple.createPlaylist` → `adapter.apple.addTracks` (배치별 child) → `adapter.apple.verify` 의 단일 trace.
- NormalizationEngine 호출은 별도 root trace + parent linkage (async 경계).

---

## 10. 향후 확장

### 10.1 P1: Apple Read / Spotify Write
- 새 인터페이스가 아니라 **기존 `PlaylistReadAdapter`/`PlaylistWriteAdapter` 의 추가 구현체**로 만든다.
- Apple Read는 user library scope를 추가로 요구한다. Spotify Write는 `playlist-modify-public/private` scope 필요.
- 도메인 코어 변경은 없어야 한다 (NFR-071).

### 10.2 P2: YouTube Music
- YouTube Music은 ISRC 노출이 불완전하고 `videoId`가 라이브/팬업로드 등 변형을 가진다 (Platform Review §5.1).
- 따라서 P2 진입 전제 조건:
  1. NormalizationEngine의 fuzzy 매칭 신뢰도가 안정화 (목표 평균 confidence ≥ 0.85).
  2. `ExternalTrack` 모델에 `videoVariantHint` 필드를 도입해 official audio/MV/live를 구분.
  3. YouTube의 read/write 정책 quota를 별도 throttling 정책으로 분리.
- 동일한 두 인터페이스 위에서 어댑터가 추가되며, 구체 클래스 `YouTubeMusicReadAdapter`/`YouTubeMusicWriteAdapter`를 도입한다.

### 10.3 어댑터 비활성화 / 기능 플래그
- NFR-033, NFR-092에 따라 모든 어댑터는 feature flag로 enable/disable이 가능해야 한다.
- 특정 플랫폼 정책 변경 시 해당 어댑터만 OFF 해도 자산 보기/편집은 정상 동작한다 (NFR-082).

---

## 11. 다음 구현 태스크 분할 제안

본 문서가 검토 승인되면 아래 태스크로 분할해 구현을 진행한다.

| Task ID | 범위 | 산출 (코드) | 선행 조건 |
| :--- | :--- | :--- | :--- |
| `be-domain-canonical` | Canonical Track / TrackMapping / ExportJob 도메인 엔티티 + 마이그레이션 | `domain/`, `db/migration/V*__canonical.sql` | 본 문서 + 정규화 문서 승인 |
| `be-adapter-port` | `PlaylistReadAdapter`, `PlaylistWriteAdapter` 인터페이스 + 공용 DTO | `domain/adapter/` | `be-domain-canonical` |
| `be-spotify-adapter` | SpotifyReadAdapter 구현 (OAuth PKCE, fetchPlaylists/Tracks) | `infrastructure/adapter/spotify/` | `be-adapter-port` |
| `be-apple-adapter` | AppleMusicWriteAdapter 구현 (JWT, createPlaylist/addTracks/verify) | `infrastructure/adapter/apple/` | `be-adapter-port` |
| `be-normalization-engine` | ISRC + fuzzy 매칭 엔진 (Async, isolation) | `application/normalization/` | `be-domain-canonical` |
| `be-export-job-engine` | Job 상태 머신 + 워커 + retry 정책 | `application/export/` | `be-apple-adapter`, `be-normalization-engine` |
| `be-idempotency` | `X-Idempotency-Key` 미들웨어 + 저장소 | `web/idempotency/` | (선행 없음, 병렬 가능) |
| `be-observability` | 구조화 로그 + 메트릭 + trace 컨텍스트 전파 | cross-cutting | 어댑터들과 병렬 가능 |
| `be-admin-failure-logs` | 운영자 실패 로그/매칭 실패 조회 (FR-101, FR-102) | `web/admin/` | `be-export-job-engine` |

---

## 부록 A. 본 문서가 다루지 않는 것
- B2B 데이터 패키징/판매 워크플로 (Future Expansion).
- 강한 센서 기반 인간성 검증 엔진 (P2 이후, NFR-061과 충돌 회피).
- 실제 음원 스트리밍 / 재생 (NFR-030, 비재생 중심 구조).
- Spotify의 양방향 export 또는 YouTube Music 어댑터 (각각 P1, P2).

## 부록 B. 변경 이력
| Version | Date | Author | Note |
| :--- | :--- | :--- | :--- |
| v0.1 | 2026-05-03 | Backend Engineer (Claude) | 최초 작성. PO의 Apple 유지 결정 반영. |
