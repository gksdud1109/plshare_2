-- 큐레이션 카탈로그 — 재생 검증된 YouTube videoId 가 박힌 곡 풀.
-- 데모(H2 ddl-auto)는 엔티티가 스키마 소스이므로 이 마이그레이션은 prod(Postgres/Flyway) 전용.
CREATE TABLE curated_tracks (
    id               UUID PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    artist           VARCHAR(255) NOT NULL,
    youtube_video_id VARCHAR(16)  NOT NULL,
    duration_ms      INT,
    mood             VARCHAR(32)  NOT NULL,
    cover_url        VARCHAR(512)
);
CREATE INDEX idx_curated_mood ON curated_tracks (mood);
