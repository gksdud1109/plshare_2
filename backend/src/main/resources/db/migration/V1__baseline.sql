-- ============================================================================
-- plshare baseline schema (V1)
-- 생성된 H2 ddl-auto=create-drop 스키마와 동일한 구조를 PostgreSQL용으로 작성.
-- 컬럼명/타입/제약/인덱스는 JPA 엔티티(Asset, Track, ImportJob, ExportJob,
-- OauthHandshake, SpotifyAccessGrant, CanonicalTrack)와 정확히 일치한다.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- assets
-- ----------------------------------------------------------------------------
CREATE TABLE assets (
    id                UUID         PRIMARY KEY,
    title             VARCHAR(255) NOT NULL,
    cover_url         VARCHAR(255),
    description       VARCHAR(255),
    diary_text        TEXT,
    source_platform   VARCHAR(255) NOT NULL DEFAULT 'spotify',
    share_token       VARCHAR(255) UNIQUE,
    created_at        TIMESTAMP    NOT NULL
);

-- ElementCollection: Asset.emotionTags
CREATE TABLE asset_emotion_tags (
    asset_id UUID         NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    tag      VARCHAR(255)
);
CREATE INDEX idx_asset_emotion_tags_asset_id ON asset_emotion_tags(asset_id);

-- ElementCollection: Asset.photoUrls
CREATE TABLE asset_photo_urls (
    asset_id UUID         NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    url      VARCHAR(255)
);
CREATE INDEX idx_asset_photo_urls_asset_id ON asset_photo_urls(asset_id);

-- ----------------------------------------------------------------------------
-- tracks
-- ----------------------------------------------------------------------------
CREATE TABLE tracks (
    id              UUID         PRIMARY KEY,
    asset_id        UUID         REFERENCES assets(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    artist          VARCHAR(255) NOT NULL,
    duration_ms     INTEGER,
    isrc            VARCHAR(255),
    spotify_id      VARCHAR(255),
    apple_music_id  VARCHAR(255),
    created_at      TIMESTAMP    NOT NULL
);
CREATE INDEX idx_track_isrc ON tracks(isrc);

-- ----------------------------------------------------------------------------
-- import_jobs
-- ----------------------------------------------------------------------------
CREATE TABLE import_jobs (
    id                  UUID         PRIMARY KEY,
    idempotency_key     VARCHAR(255) NOT NULL,
    status              VARCHAR(255) NOT NULL,
    spotify_playlist_id VARCHAR(255),
    asset_id            UUID,
    total_tracks        INTEGER      NOT NULL DEFAULT 0,
    processed_tracks    INTEGER      NOT NULL DEFAULT 0,
    error_code          VARCHAR(255),
    error_message       VARCHAR(255),
    created_at          TIMESTAMP    NOT NULL,
    updated_at          TIMESTAMP    NOT NULL,
    CONSTRAINT uk_import_jobs_idempotency_key UNIQUE (idempotency_key)
);
CREATE UNIQUE INDEX idx_import_idempotency_key ON import_jobs(idempotency_key);

-- ----------------------------------------------------------------------------
-- export_jobs
-- ----------------------------------------------------------------------------
CREATE TABLE export_jobs (
    id                    UUID         PRIMARY KEY,
    asset_id              UUID         NOT NULL,
    target_platform       VARCHAR(255) NOT NULL,
    idempotency_key       VARCHAR(255) NOT NULL,
    status                VARCHAR(255) NOT NULL,
    external_playlist_id  VARCHAR(255),
    external_url          VARCHAR(255),
    total_tracks          INTEGER      NOT NULL DEFAULT 0,
    matched_tracks        INTEGER      NOT NULL DEFAULT 0,
    failed_tracks         INTEGER      NOT NULL DEFAULT 0,
    started_at            TIMESTAMP,
    finished_at           TIMESTAMP,
    last_error            VARCHAR(255),
    created_at            TIMESTAMP    NOT NULL,
    updated_at            TIMESTAMP    NOT NULL,
    CONSTRAINT uk_export_jobs_idempotency_key UNIQUE (idempotency_key)
);
CREATE UNIQUE INDEX idx_export_idempotency_key ON export_jobs(idempotency_key);

-- ----------------------------------------------------------------------------
-- oauth_handshakes
-- ----------------------------------------------------------------------------
CREATE TABLE oauth_handshakes (
    id            UUID         PRIMARY KEY,
    state         VARCHAR(128) NOT NULL,
    code_verifier VARCHAR(256) NOT NULL,
    redirect_uri  VARCHAR(512) NOT NULL,
    created_at    TIMESTAMP    NOT NULL,
    consumed_at   TIMESTAMP,
    CONSTRAINT uk_oauth_handshakes_state UNIQUE (state)
);
CREATE UNIQUE INDEX idx_oauth_handshake_state ON oauth_handshakes(state);

-- ----------------------------------------------------------------------------
-- spotify_access_grants
-- ----------------------------------------------------------------------------
CREATE TABLE spotify_access_grants (
    id            UUID          PRIMARY KEY,
    user_id       UUID          NOT NULL,
    access_token  VARCHAR(2048) NOT NULL,
    refresh_token VARCHAR(2048),
    expires_at    TIMESTAMP     NOT NULL,
    scope         VARCHAR(512),
    created_at    TIMESTAMP     NOT NULL,
    updated_at    TIMESTAMP     NOT NULL
);
CREATE INDEX idx_spotify_grant_user ON spotify_access_grants(user_id);

-- ----------------------------------------------------------------------------
-- canonical_tracks
-- ----------------------------------------------------------------------------
CREATE TABLE canonical_tracks (
    canonical_id      UUID             PRIMARY KEY,
    isrc              VARCHAR(255),
    spotify_track_id  VARCHAR(255),
    apple_song_id     VARCHAR(255),
    title             VARCHAR(255)     NOT NULL,
    artists           VARCHAR(255)     NOT NULL,
    album             VARCHAR(255),
    duration_ms       BIGINT           NOT NULL,
    match_confidence  DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    sources           VARCHAR(255)     NOT NULL,
    created_at        TIMESTAMP        NOT NULL,
    updated_at        TIMESTAMP        NOT NULL
);
CREATE UNIQUE INDEX idx_canonical_isrc         ON canonical_tracks(isrc);
CREATE INDEX        idx_canonical_spotify_id   ON canonical_tracks(spotify_track_id);
CREATE INDEX        idx_canonical_apple_id     ON canonical_tracks(apple_song_id);
CREATE INDEX        idx_canonical_title_prefix ON canonical_tracks(title);
