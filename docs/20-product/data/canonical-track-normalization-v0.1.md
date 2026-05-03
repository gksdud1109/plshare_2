# Canonical Track Normalization v0.1

## 문서 정보
- Date: `2026-05-03`
- Role: `Backend Engineer`
- Model: `Claude`
- Version: `v0.1`
- Status: `Draft for Review`
- Scope: `MVP - ISRC-first canonical track identity for Spotify→Apple bridge`
- Related Documents:
  - `docs/20-product/strategy/prd-v0.2.md`
  - `docs/20-product/requirements/functional-requirements-v0.2.md`
  - `docs/20-product/requirements/non-functional-requirements-v0.2.md`
  - `docs/20-product/strategy/platform-direction-review-v0.1.md`
  - `docs/20-product/data/adapter-architecture-v0.1.md`

> **본 문서는 데이터 모델/알고리즘 설계 문서이며 실제 코드 구현을 포함하지 않는다.** 구현은 사람 검토 승인 후 별도 태스크 (`be-domain-canonical`, `be-normalization-engine`)에서 진행된다.

---

## 1. Canonical Track Identity 데이터 모델

### 1.1 설계 원칙
- **NFR-070** "Canonical data 우선 구조": 내부 도메인 모델은 어떤 단일 플랫폼 ID에도 종속되지 않는다.
- **PRD §10**: Canonical Track Identity = `[ISRC / Spotify ID / Apple Music ID / YouTube videoId / 제목 / 아티스트 / 매칭 신뢰도]`.
- **Platform Review §5.1**: ISRC가 매칭 무결성의 1순위 키. `videoId` 의존은 무결성을 해친다.

### 1.2 Kotlin 도메인 정의 (개념적)

```kotlin
package com.plshare.backend.domain.canonical

import java.time.Instant
import java.util.UUID

/**
 * 플랫폼 비종속 트랙 정체성. 동일한 음원에 대한 단일 진리원천.
 * Spotify/Apple/YouTube의 트랙 ID들은 TrackMapping에 분리 저장된다.
 */
data class CanonicalTrack(
    val canonicalId: UUID,            // 내부 primary key
    val isrc: String?,                // 1순위 정규화 키 (없을 수 있음)
    val title: String,                // 표시용 표준 제목
    val artists: List<String>,        // 순서 보존
    val album: String?,
    val durationMs: Long?,            // 매칭 보조 키
    val matchConfidence: Double,      // 0.0 ~ 1.0
    val sources: Set<PlatformId>,     // 어떤 플랫폼에서 봤는지 (관측 이력)
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * canonicalId ↔ 플랫폼별 ID 매핑. canonical 1 : N platforms.
 * Apple write 시 lookup의 캐시 역할도 겸한다 (§8).
 */
data class TrackMapping(
    val canonicalId: UUID,
    val platform: PlatformId,
    val platformTrackId: String,      // Spotify trackId / Apple songId / YouTube videoId
    val matchTier: MatchTier,         // EXACT_ISRC | HIGH_FUZZY | LOW_FUZZY | MANUAL | UNMATCHED
    val confidence: Double,
    val verifiedAt: Instant?,         // 마지막 read-back 검증 시각
    val createdAt: Instant
)

enum class MatchTier { EXACT_ISRC, HIGH_FUZZY, LOW_FUZZY, MANUAL, UNMATCHED }
```

### 1.3 SQL DDL (개념적, 실제 마이그레이션은 별도 태스크)

```sql
CREATE TABLE canonical_track (
    canonical_id        UUID         PRIMARY KEY,
    isrc                VARCHAR(15)  UNIQUE,        -- nullable; ISRC 표준 12자, 여유 15
    title               VARCHAR(500) NOT NULL,
    artists             JSONB        NOT NULL,
    album               VARCHAR(500),
    duration_ms         BIGINT,
    match_confidence    DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    sources             JSONB        NOT NULL DEFAULT '[]',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_canonical_track_title_lower ON canonical_track (LOWER(title));
CREATE INDEX idx_canonical_track_artists_gin ON canonical_track USING GIN (artists);

CREATE TABLE track_mapping (
    canonical_id        UUID         NOT NULL REFERENCES canonical_track(canonical_id),
    platform            VARCHAR(32)  NOT NULL,
    platform_track_id   VARCHAR(128) NOT NULL,
    match_tier          VARCHAR(32)  NOT NULL,
    confidence          DOUBLE PRECISION NOT NULL,
    verified_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (canonical_id, platform),
    UNIQUE (platform, platform_track_id)
);
CREATE INDEX idx_track_mapping_platform_lookup ON track_mapping (platform, platform_track_id);
```

> ISRC 컬럼에 `UNIQUE`를 두는 것이 정규화 중복을 막는 핵심 제약. ISRC 누락(`NULL`)은 unique 제약 회피 (PostgreSQL 기본 동작).

---

## 2. 정규화 우선순위 트리

FR-021을 다음과 같이 구체화한다.

```
Incoming ExternalTrack
        │
        ▼
[1] ISRC 존재?  ───── No ──▶ [2] 플랫폼 ID 직접 매핑 존재?
        │                            │
        Yes                       Yes / No
        ▼                            │
ISRC로 canonical_track 조회           ▼ No
        │                     [3] Fuzzy 매칭
        ├── 존재 → 기존 row 사용     (artist + title + duration ±1s)
        │   sources에 platform 추가      │
        │                                ├── confidence ≥ 0.90 → high-confidence
        └── 부재 → 새 row 생성            ├── 0.70 ≤ confidence < 0.90 → low-confidence
                                          └── confidence < 0.70 → unmatched (manual P1)
```

| 순위 | 방법 | MatchTier | 비고 |
| :---: | :--- | :--- | :--- |
| 1 | ISRC 정확 매칭 | `EXACT_ISRC` | confidence = 1.0. ISRC는 IFPI 표준이며 동일 음원 동일 ISRC. |
| 2 | 플랫폼 ID 직접 매핑 (캐시 hit) | 기존 tier 유지 | `track_mapping` 캐시에서 조회. ISRC가 없어도 이미 알고 있는 트랙. |
| 3 | Fuzzy 매칭 (artist + title + duration ±1s) | `HIGH_FUZZY` / `LOW_FUZZY` / `UNMATCHED` | §3에서 스코어 룰 정의. |
| 4 (P1) | 사용자 수동 매칭 | `MANUAL` | confidence = 1.0 (사용자 의도 신뢰). FR-023. |

---

## 3. matchConfidence 산정 (0.0 ~ 1.0)

### 3.1 베이스 룰
| 시나리오 | confidence |
| :--- | :--- |
| ISRC 정확 일치 | **1.00** |
| 캐시된 platform ID 매핑 (이전에 검증됨) | **0.99** |
| 사용자 수동 매핑 | **1.00** |
| Fuzzy 매칭 | 아래 가중합 |
| 매칭 후보 없음 | **0.00** (UNMATCHED) |

### 3.2 Fuzzy 매칭 스코어 (3순위 경로)

세 개의 부분 점수를 가중합으로 결합한다.

```
score = 0.50 * titleSim
      + 0.30 * artistSim
      + 0.20 * durationSim
```

| 부분 점수 | 정의 |
| :--- | :--- |
| `titleSim` | 제목 정규화(소문자, 괄호 안 부가 표기 제거, 한/영 punctuation 제거) 후 token-set ratio (0~1) |
| `artistSim` | 아티스트 배열 간 Jaccard similarity (정규화 후) |
| `durationSim` | `1.0 if abs(durMsA - durMsB) ≤ 1000`, `linear decay to 0 over 5000ms`, `0 beyond` |

### 3.3 Tier 분기점
| Tier | 범위 |
| :--- | :--- |
| `HIGH_FUZZY` | `score ≥ 0.90` |
| `LOW_FUZZY` | `0.70 ≤ score < 0.90` |
| `UNMATCHED` | `score < 0.70` |

### 3.4 보정 규칙
- ISRC가 한 쪽만 있고 다른 쪽이 없는 경우: ISRC가 있는 후보를 +0.02 가산.
- duration이 양쪽 모두 결측이면 `durationSim = 0.5` 중립값.
- 다중 후보 동률 시 가장 최근에 verified된 매핑을 선택.

---

## 4. 매핑 플로우

### 4.1 Spotify import 시점 (canonical 생성)

```
[ExternalTrack from Spotify]
          │
          ▼
   isrc != null ?
   ┌──────┴──────┐
   Yes           No
   │             │
   ▼             ▼
SELECT canonical_track   Fuzzy 매칭
WHERE isrc = ?           (artists + title + duration)
   │                     │
   ├─ hit                ├─ score ≥ 0.90 → 기존 canonical에 부착
   │   sources에 추가     ├─ 0.70~0.89 → low-confidence 후보 보관
   │                     └─ < 0.70 → 새 canonical 생성 (UNMATCHED 표시)
   └─ miss
       │
       ▼
   INSERT canonical_track (isrc, title, artists, ...)
       │
       ▼
   UPSERT track_mapping (canonical_id, platform=SPOTIFY, platformTrackId, tier=EXACT_ISRC)
```

### 4.2 Apple export 시점 (Apple ID lookup)

```
[CanonicalTrack to export]
          │
          ▼
   track_mapping.platform = APPLE_MUSIC 캐시 hit?
   ┌────────────┴────────────┐
   Yes                        No
   │                          │
   ▼                          ▼
   appleSongId 사용     isrc 존재?
                       ┌──────┴──────┐
                       Yes           No
                       │             │
                       ▼             ▼
                 GET Apple        Apple 카탈로그
                 catalog/songs    텍스트 검색
                 ?filter[isrc]    (artist + title)
                       │             │
                       ├─ hit → mapping 캐시
                       └─ miss → low-confidence 후보 또는 UNMATCHED
```

---

## 5. 충돌 처리 (동일 ISRC, 다른 메타)

ISRC는 unique 제약을 가지지만, 동일 ISRC에 대해 외부 플랫폼이 다른 메타데이터(예: 리마스터 표기, 아티스트 표기 차이)를 줄 수 있다.

### 5.1 Merge 정책
- **Identity는 ISRC로 고정.** 동일 ISRC에 대한 새 관측이 들어오면 새 canonical row를 만들지 않고 기존 row의 `sources`에 platform을 추가한다.
- **Title/Artists/Album 갱신 규칙:**
  - 첫 관측이 곧 표시용 표준값.
  - 후속 관측에서 차이가 있으면 갱신하지 않고 `track_mapping` 테이블에 platform-specific 표기를 보관한다 (P1: `track_mapping_metadata` 별도 컬럼 가능).
  - 단, 첫 관측이 명백히 깨진 경우(예: 빈 문자열, 모자이크/`???`)는 다음 관측으로 덮어쓴다.
- **Duration 갱신:** 새 관측의 duration이 기존과 1초 초과 차이면 무시. 두 값을 모두 보관해야 할 경우 `track_mapping`에 platform별로 별도 저장.

### 5.2 Edge case: 한 트랙이 두 ISRC를 갖는 경우
- 리마스터, 라이브 버전, 컴필레이션 재발매 등에서 발생.
- 정책: **별도 canonical row로 취급한다.** 사용자가 명시적으로 "같은 곡이다"라고 표시하지 않는 한 합치지 않는다.
- P1에서 사용자가 "same recording" 표시를 할 수 있는 UX 도입 검토.

---

## 6. ISRC 누락 / 지역 제한 트랙 fallback

### 6.1 ISRC 누락
대상: 인디 업로드, 일부 카탈로그.

1. Fuzzy 매칭으로 canonical을 시도. `HIGH_FUZZY`면 부착, 아니면 새 canonical 생성 (ISRC NULL).
2. 이후 동일 음원의 다른 플랫폼 트랙이 ISRC와 함께 들어오면 **재매칭 잡** (NormalizationEngine 백그라운드)이 fuzzy로 두 row를 후보로 보고 사용자/운영자 검토 큐에 넣는다 (P1).
3. MVP에서는 자동 머지하지 않는다. 잘못된 머지 비용이 분리 비용보다 크다.

### 6.2 지역 제한 (Apple 카탈로그에 없음)
- Apple 카탈로그 lookup이 ISRC로 miss하는 경우.
- 처리:
  1. fuzzy 텍스트 검색을 시도 (artist + title).
  2. 결과가 `HIGH_FUZZY`면 매핑 + 사용자에게 "지역 카탈로그 차이로 다른 버전이 매칭되었음" 표시 (NFR-060, NFR-062).
  3. 결과가 약하면 **export에서 skip** + 사용자에게 사유 표시. 잡 상태는 `partial-success`.

### 6.3 사용자에게 노출
- 매칭 신뢰도 (`exact` / `high` / `low` / `unmatched`)와 사유를 자산 상세 페이지와 export 미리보기에 표시한다 (FR-022, FR-073, NFR-060).

---

## 7. 정규화 엔진 트랜잭션/동시성 정책

### 7.1 결정: **Async + Transaction Isolation (READ COMMITTED + Advisory Lock)**

#### 근거
- **Async (큐 기반):**
  - 정규화는 외부 lookup (Apple ISRC catalog 등) 을 동반해 latency가 가변적이다. NFR-011은 100곡 이하 import를 30초 내에 처리할 것을 요구하지만, 이는 사용자 응답 차단 없이 백그라운드 처리로 달성한다.
  - NFR-072가 "큐 기반 비동기 작업으로 분리 가능해야 한다"를 명시.
  - Import API는 잡을 큐에 등록하고 즉시 응답. 워커가 NormalizationEngine을 실행.
- **Transaction Isolation:**
  - 동시에 같은 ISRC를 가진 트랙이 두 import에서 들어오면 race condition으로 canonical이 중복 생성될 수 있다. ISRC `UNIQUE` 제약이 1차 방어이지만, **insert 충돌 시 graceful fallback**을 위해 트랜잭션 내에서 다음 패턴을 사용한다:
    1. ISRC 단위로 PostgreSQL `pg_advisory_xact_lock(hashtext(isrc))`로 직렬화.
    2. SELECT → 없으면 INSERT → 있으면 mapping 추가.
    3. READ COMMITTED 격리 수준 (PostgreSQL 기본). 더 강한 격리는 부하 대비 이득이 적다.
  - ISRC 없는 트랙은 `(LOWER(title), artists_normalized, duration_bucket)` 합성 키로 동일한 advisory lock을 적용한다.

### 7.2 백프레셔
- 큐 길이가 임계 초과 시 새 import는 `queued` 상태로만 받고 사용자에게 대기 시간을 표시한다 (NFR-011 보호).

---

## 8. Apple 매핑 lookup (canonicalId → appleSongId)

### 8.1 Lookup 전략 (우선순위)
1. `track_mapping` 캐시 hit (`canonical_id`, `platform=APPLE_MUSIC`).
2. ISRC가 있으면 Apple `GET /catalog/{storefront}/songs?filter[isrc]={isrc}`. (배치 가능, 최대 25 ISRC/호출 권장.)
3. ISRC 없거나 storefront miss 시 텍스트 검색 `GET /catalog/{storefront}/search?term=...&types=songs`.
4. 모든 후보 실패 → UNMATCHED 처리.

### 8.2 Storefront
- 사용자 Music User Token에서 storefront 추출 (예: `kr`, `us`).
- 캐시 키에 storefront를 포함한다 (`(canonical_id, apple, storefront)`). 한국 카탈로그에 없는 곡이 미국 카탈로그에는 있을 수 있다.

### 8.3 캐싱
| 레이어 | TTL | 무효화 |
| :--- | :--- | :--- |
| In-memory (LRU, 캐시 사이즈 ≤ 50k) | 10분 | 없음 (LRU) |
| `track_mapping` (영속) | TTL 없음 | `verified_at`이 30일 경과 시 read-back 재검증 |
| Apple 카탈로그 변경 감지 | 명시적 invalidation API (운영자) | 정책 변경 대응 (NFR-033) |

### 8.4 배치
- 동일 export 잡 내 트랙들을 ISRC 단위로 묶어 25개씩 배치 lookup. rate limit 효율과 latency 모두 개선.

---

## 9. 매칭 품질 모니터링 메트릭

NFR-091, NFR-081을 지원한다.

| 메트릭 | 차원 | 알람 임계 (제안) |
| :--- | :--- | :--- |
| `match_outcome_total{tier}` | source_platform, target_platform, tier | unmatched 비율 > 15% / 24h |
| `match_confidence_avg` | target_platform | 평균 confidence < 0.85 / 24h |
| `apple_isrc_lookup_latency_ms` (histogram) | storefront | p95 > 2000ms |
| `apple_isrc_lookup_miss_total` | storefront | miss 비율 > 30% / 1h |
| `canonical_track_created_total` | source | 비정상 급증 감시 (중복 생성 의심) |
| `manual_match_count` | (P1) | 운영자 검토 큐 크기 |

운영자 대시보드는 위 메트릭을 platform / storefront 별로 펼쳐 볼 수 있어야 한다 (NFR-081, FR-102).

---

## 10. 알려진 한계 및 P1/P2 개선

### MVP 한계 (수용)
- ISRC가 없는 트랙은 fuzzy에 의존 → 동명 이곡(remaster, live)이 같은 canonical로 묶일 위험.
- 사용자 수동 매칭 UX는 P1로 미뤘다 (FR-023의 "대체 트랙 후보 선택"은 후보 제안만 MVP에서 제공).
- Apple 카탈로그 storefront 차이로 인한 누락은 사용자에게 사유만 표시하고 자동 보정하지 않는다.
- YouTube `videoId`는 정규화 대상이 아니다 (Platform Review §5.1).

### P1 개선
- 사용자 수동 매칭 (`MANUAL` tier 본격 활용, FR-023).
- 비ISRC 트랙의 백그라운드 재매칭 잡 (ISRC가 나중에 들어왔을 때 머지 후보 제안).
- Apple Music import 어댑터 추가 → canonical에 Apple-origin 트랙 합류.
- 매칭 신뢰도가 낮은 트랙에 대해 사용자에게 alternative 후보 N개 표시.

### P2 개선
- YouTube Music 어댑터 도입 시 `videoVariantHint` 필드 추가하여 official audio/MV/live 구분.
- 운영자가 두 canonical row를 머지/스플릿할 수 있는 관리자 도구.
- 라이선스/감사 추적이 강화된 canonical 변경 이력 테이블.

---

## 부록 A. 본 문서가 다루지 않는 것
- Spotify/Apple 어댑터 자체 구현 상세 → `adapter-architecture-v0.1.md` 참조.
- B2B 데이터 패키징 스키마 (Future Expansion).
- 강한 인간성 검증/proof signal과 canonical 데이터의 결합 (NFR-061, P2 이후).
- 실제 음원 파일/스트리밍 처리 (NFR-030, 비재생 중심 구조).

## 부록 B. 변경 이력
| Version | Date | Author | Note |
| :--- | :--- | :--- | :--- |
| v0.1 | 2026-05-03 | Backend Engineer (Claude) | 최초 작성. ISRC primary 결정 + Async/Isolation 정책 반영. |
