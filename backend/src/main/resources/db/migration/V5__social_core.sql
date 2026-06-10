-- ============================================================================
-- plshare V5: social core — posts, post_likes, comments, follows
-- polyenm_pan domain/{reaction,comment,follow} 테이블 구조 차용.
-- plshare 변경: UUID 기반 PK (polyenm BIGSERIAL 대신), LocalDateTime (Instant 대신).
-- V4는 다른 에이전트 영역 — 절대 건드리지 않음.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- posts
-- ----------------------------------------------------------------------------
CREATE TABLE posts (
    id         UUID         PRIMARY KEY,
    author_id  UUID         NOT NULL,
    text       VARCHAR(500) NOT NULL,
    asset_id   UUID,
    track_id   UUID,
    mood_tag   VARCHAR(50),
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL
);

-- 피드 쿼리용: author_id + created_at 복합 인덱스 (팔로잉 피드 author_id IN + 최신순)
CREATE INDEX idx_posts_author_created ON posts (author_id, created_at DESC);
-- 전체 피드용: created_at 단독 인덱스
CREATE INDEX idx_posts_created_at     ON posts (created_at DESC);
-- soft-delete 필터용
CREATE INDEX idx_posts_deleted        ON posts (deleted);

-- ----------------------------------------------------------------------------
-- post_likes
-- polyenm reactions 테이블의 (targetType, targetId, userId) unique 패턴을
-- post 전용 (post_id, user_id) unique로 단순화.
-- ----------------------------------------------------------------------------
CREATE TABLE post_likes (
    id         UUID      PRIMARY KEY,
    post_id    UUID      NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id    UUID      NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_post_likes_post_user UNIQUE (post_id, user_id)
);

CREATE INDEX idx_post_likes_post_id ON post_likes (post_id);
CREATE INDEX idx_post_likes_user_id ON post_likes (user_id);

-- ----------------------------------------------------------------------------
-- comments
-- polyenm comments 테이블 패턴 차용 — soft-delete(deleted), post_id/author_id 참조.
-- plshare 변경: answer_id → post_id, BIGINT → UUID, parent_id 미지원(MVP P0 외).
-- ----------------------------------------------------------------------------
CREATE TABLE comments (
    id         UUID         PRIMARY KEY,
    post_id    UUID         NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    author_id  UUID         NOT NULL,
    text       VARCHAR(300) NOT NULL,
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_comments_post_id   ON comments (post_id);
CREATE INDEX idx_comments_author_id ON comments (author_id);

-- ----------------------------------------------------------------------------
-- follows
-- polyenm follows 테이블 패턴 차용 — (follower_id, followee_id) unique 제약.
-- plshare 변경: BIGINT → UUID.
-- ----------------------------------------------------------------------------
CREATE TABLE follows (
    id          UUID      PRIMARY KEY,
    follower_id UUID      NOT NULL,
    followee_id UUID      NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    CONSTRAINT uk_follows_follower_followee UNIQUE (follower_id, followee_id)
);

CREATE INDEX idx_follows_follower_id ON follows (follower_id);
CREATE INDEX idx_follows_followee_id ON follows (followee_id);
