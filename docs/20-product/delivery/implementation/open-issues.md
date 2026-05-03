# Open Issues

## UX→API required fields (Finalized for Backend Scaffolding)

이 문서는 시니어 백엔드 엔지니어의 리뷰를 거쳐 데이터 정합성과 멱등성이 보장되도록 확정된 API 계약입니다.

### 1. Dashboard / Asset List
- `assets`: Array
  - `id`: UUID (Primary Key)
  - `title`: String
  - `cover_url`: String
  - `tag_list`: Array<String>
  - `created_at`: ISO8601
  - `track_count`: Integer
  - `source_platform`: Enum (spotify)

### 2. Import Request (POST /api/v1/imports)
- **Request Header**:
  - `X-Idempotency-Key`: UUID (Required, 클라이언트 생성 멱등성 키)
- **Request Body**:
  - `spotify_playlist_id`: String
- **Response**:
  - `import_job_id`: UUID
  - `status`: Enum (queued)

### 3. Import Status (Real-time / Polling)
- `import_job_id`: UUID
- `status`: Enum (queued, running, completed, failed)
- `error_code`: String (optional, e.g., "EXTERNAL_API_ERROR", "AUTH_EXPIRED")
- `progress`: Object
  - `total_tracks`: Integer
  - `processed_tracks`: Integer
  - `current_track_name`: String
- `normalization_results`: Array (Summary only)
  - `match_rate`: Float (0.0 ~ 1.0)
  - `exact_match_count`: Integer

### 4. Asset Detail (The Card)
- `asset`: Object
  - (All fields from List)
  - `description`: String
  - `diary_text`: String
  - `tracks`: Array
    - `id`: UUID
    - `name`: String
    - `artist`: String
    - `isrc`: String (Unique identifier for normalization)
    - `platform_meta`: Object (spotify_id, apple_music_id 등 플랫폼별 메타데이터)

### 5. Export Request (POST /api/v1/exports)
- **Request Header**:
  - `X-Idempotency-Key`: UUID (Required)
- **Request Body**:
  - `asset_id`: UUID
  - `target_platform`: Enum (apple_music)
- **Response**:
  - `export_job_id`: UUID

### 6. Export Status
- `export_job_id`: UUID
- `target_platform`: Enum (apple_music)
- `status`: Enum (queued, running, completed, partially_completed, failed)
- `results`: Object
  - `success_count`: Integer
  - `fail_count`: Integer
  - `failed_tracks`: Array<Object> (name, isrc, reason)
  - `target_playlist_id`: String (Apple Music 내 생성된 ID)
  - `target_playlist_url`: String (Deep link)

---

## Backend Implementation Notes (from Toss Payments Review Skill)

1. **Transaction Isolation**: 
   - Spotify/Apple Music API 호출은 트랜잭션 외부에서 수행한다.
   - DB 저장은 API 호출 성공 후 별도 트랜잭션으로 처리하여 DB 커넥션 점유 시간을 최소화한다.
2. **Idempotency**: 
   - `X-Idempotency-Key`를 Redis 또는 DB에 저장하여 24시간 내 동일 요청 시 기존 `job_id`를 반환한다.
3. **Resilience**:
   - 외부 API 호출 시 반드시 Timeout(Default 5s)을 설정한다.
   - 429(Too Many Requests) 발생 시 지수 백오프(Exponential Backoff)를 적용한 재시도 큐를 활용한다.
4. **Data Integrity**:
   - ISRC가 없는 트랙은 별도 'Unmatched' 상태로 관리하며, 사용자에게 알림을 제공한다.
