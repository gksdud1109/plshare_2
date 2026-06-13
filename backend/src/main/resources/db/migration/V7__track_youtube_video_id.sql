-- V7: tracks.youtube_video_id — YouTube 소스 트랙의 videoId 전용 컬럼.
-- import(YouTube read)가 저장하고 export(YouTube write)가 사용한다.
-- demo(H2)는 Track 엔티티 ddl-auto로 자동 생성되며, 이 마이그레이션은 prod(PostgreSQL)용.
ALTER TABLE tracks ADD COLUMN youtube_video_id VARCHAR(64);
CREATE INDEX idx_track_youtube_video_id ON tracks (youtube_video_id);
