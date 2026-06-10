-- ============================================================================
-- V2: tracks 테이블에 canonical_track_id, match_confidence 컬럼 추가
-- canonical_track_id 는 canonical_tracks(canonical_id) 를 논리적으로 참조하지만
-- 도메인 경계(asset ↔ track domain)를 넘는 FK 제약은 두지 않는다.
-- ============================================================================

ALTER TABLE tracks
    ADD COLUMN canonical_track_id UUID,
    ADD COLUMN match_confidence   DOUBLE PRECISION;

CREATE INDEX idx_track_canonical_track_id ON tracks(canonical_track_id);
