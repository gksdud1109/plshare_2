-- Per-track export match outcomes (manual review for low-confidence matches).
-- The aggregate matched/failed counts already live on export_jobs; this table
-- persists the per-track detail (which video matched, with what confidence and
-- status) so users can review ALTERNATIVE/FAILED matches and override them.

CREATE TABLE export_track_matches (
    id               UUID PRIMARY KEY,
    export_job_id    UUID NOT NULL REFERENCES export_jobs(id) ON DELETE CASCADE,
    track_id         UUID NOT NULL,
    title            VARCHAR(512) NOT NULL,
    artist           VARCHAR(512) NOT NULL,
    matched_video_id VARCHAR(64),
    matched_title    VARCHAR(512),
    confidence       DOUBLE PRECISION,
    status           VARCHAR(32) NOT NULL,
    reviewed         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP NOT NULL
);

CREATE INDEX idx_export_track_match_job ON export_track_matches(export_job_id);
