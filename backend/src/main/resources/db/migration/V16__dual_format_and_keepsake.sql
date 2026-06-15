-- ============================================================================
-- plshare V16: 듀얼 포맷(단일 무드영상/트랙리스트) + 선물 키프세이크 필드
--  - assets.asset_kind: TRACKLIST(기존, default) | MOOD_VIDEO(단일 유튜브 영상)
--  - assets.mood_*: MOOD_VIDEO 본체(영상 1개 → prod 재생 자동 보장, 수록곡은 자유 텍스트)
--  - gifts.dedication_to / occasion: 헌정·계기(포맷 불문, 봉투/언박싱/OG 재사용)
-- 기존 행은 asset_kind 기본값 'TRACKLIST' 로 전부 유효.
-- ============================================================================

ALTER TABLE assets ADD COLUMN asset_kind VARCHAR(16) NOT NULL DEFAULT 'TRACKLIST';
ALTER TABLE assets ADD COLUMN mood_video_id VARCHAR(32);
ALTER TABLE assets ADD COLUMN mood_channel_name VARCHAR(120);
ALTER TABLE assets ADD COLUMN mood_track_list_text TEXT;

ALTER TABLE gifts ADD COLUMN dedication_to VARCHAR(40);
ALTER TABLE gifts ADD COLUMN occasion VARCHAR(20);
