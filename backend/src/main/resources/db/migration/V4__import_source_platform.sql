-- ============================================================================
-- V4: import_jobs 에 multi-source 필드 추가
--
-- 설계 결정:
--   - source_platform (NOT NULL DEFAULT 'spotify'): 기존 Spotify-only 레코드와 하위호환.
--   - source_playlist_id: 범용 플레이리스트 ID. Spotify의 경우 spotify_playlist_id와 동일.
--     spotify_playlist_id 컬럼은 기존 쿼리/코드 호환을 위해 유지한다.
--   - tracks 테이블: spotifyId 컬럼(spotify_id)은 YouTube videoId 저장에 재사용 가능.
--     Track 엔티티의 spotifyId 필드는 sourcePlatform="youtube" 시 null이 되므로
--     스키마 추가 없이 기존 컬럼 의미를 확장한다 — 컬럼 추가 최소화.
--     (video_id 전용 컬럼은 향후 tracks 테이블 확장 시 추가 가능. 현재는 불필요.)
-- ============================================================================

ALTER TABLE import_jobs
    ADD COLUMN source_platform   VARCHAR(50)  NOT NULL DEFAULT 'spotify',
    ADD COLUMN source_playlist_id VARCHAR(255);

-- 기존 레코드에 source_playlist_id 를 spotify_playlist_id 로 채운다
UPDATE import_jobs
SET source_playlist_id = spotify_playlist_id
WHERE spotify_playlist_id IS NOT NULL
  AND source_playlist_id IS NULL;
