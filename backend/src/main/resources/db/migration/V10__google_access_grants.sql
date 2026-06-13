CREATE TABLE google_access_grants (
    id            UUID          PRIMARY KEY,
    user_id       UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    access_token  VARCHAR(2048) NOT NULL,
    refresh_token VARCHAR(2048),
    expires_at    TIMESTAMP     NOT NULL,
    scope         VARCHAR(1024),
    created_at    TIMESTAMP     NOT NULL,
    updated_at    TIMESTAMP     NOT NULL
);

CREATE UNIQUE INDEX idx_google_grant_user ON google_access_grants(user_id);
