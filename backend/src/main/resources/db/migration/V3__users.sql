-- ============================================================================
-- plshare V3: users table
-- Stores plshare user identity. Google OAuth login resolves by google_subject.
-- email and handle have unique constraints but are deferred-nullable for legacy
-- anonymous paths (Spotify import without a Google login).
-- ============================================================================

CREATE TABLE users (
    id             UUID         PRIMARY KEY,
    email          VARCHAR(255) UNIQUE,
    display_name   VARCHAR(255) NOT NULL,
    handle         VARCHAR(64)  NOT NULL,
    avatar_url     VARCHAR(1024),
    google_subject VARCHAR(255) UNIQUE,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL,
    CONSTRAINT uk_users_handle UNIQUE (handle)
);

CREATE UNIQUE INDEX idx_users_email          ON users(email)          WHERE email IS NOT NULL;
CREATE UNIQUE INDEX idx_users_handle         ON users(handle);
CREATE UNIQUE INDEX idx_users_google_subject ON users(google_subject) WHERE google_subject IS NOT NULL;
