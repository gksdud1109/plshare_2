-- ============================================================================
-- plshare V8: gifts — 감성 선물 플로우
-- senderId → assetId → message + wrapSkin → token(공개 링크) → status 전이.
-- V7은 다른 에이전트 영역 — 절대 건드리지 않음.
-- ============================================================================

CREATE TABLE gifts (
    id                UUID         PRIMARY KEY,
    sender_id         UUID         NOT NULL,
    asset_id          UUID         NOT NULL,
    message           VARCHAR(500) NOT NULL,
    wrap_skin         VARCHAR(64)  NOT NULL,
    token             VARCHAR(32)  NOT NULL,
    status            VARCHAR(16)  NOT NULL DEFAULT 'CREATED',
    created_at        TIMESTAMP    NOT NULL,
    opened_at         TIMESTAMP,
    saved_by_user_id  UUID
);

-- 공개 링크 토큰 고유 인덱스
CREATE UNIQUE INDEX idx_gifts_token     ON gifts (token);
CREATE INDEX        idx_gifts_sender_id ON gifts (sender_id);
