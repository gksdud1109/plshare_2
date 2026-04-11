# Open Issues

## UX→API required fields

각 화면 구현을 위해 백엔드에서 제공해야 할 핵심 데이터 필드 계약 초안입니다.

### 1. Dashboard / Asset List
- `assets`: Array
  - `id`: UUID
  - `title`: String
  - `cover_url`: String
  - `tag_list`: Array<String>
  - `created_at`: ISO8601
  - `track_count`: Integer

### 2. Import Status (Real-time)
- `import_job_id`: UUID
- `status`: Enum (queued, running, completed, failed)
- `progress`: Object
  - `total_tracks`: Integer
  - `processed_tracks`: Integer
  - `current_track_name`: String
- `normalization_results`: Array
  - `track_name`: String
  - `isrc`: String
  - `match_status`: Enum (exact, high, low, unmatched)

### 3. Asset Detail (The Card)
- `asset`: Object
  - (All fields from List)
  - `description`: String
  - `diary_text`: String
  - `tracks`: Array
    - `id`: UUID
    - `name`: String
    - `artist`: String
    - `isrc`: String
    - `spotify_id`: String (optional)
    - `apple_music_id`: String (optional)

### 4. Export Status
- `export_job_id`: UUID
- `target_platform`: Enum (apple_music)
- `status`: Enum (queued, running, completed, partially_completed, failed)
- `results`: Object
  - `success_count`: Integer
  - `fail_count`: Integer
  - `failed_tracks`: Array<String>
  - `target_playlist_url`: String (optional deep link)
