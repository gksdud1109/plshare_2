-- ============================================================================
-- plshare V15: conversations + messages — 1:1 쪽지(DM)
-- 참여자 쌍 정규화(a < b) + 유니크 인덱스로 대화방 1쌍=1개 보장.
-- 메시지는 (conversation_id, created_at) 복합 인덱스로 시간순 조회.
-- ============================================================================

CREATE TABLE conversations (
    id                      UUID         PRIMARY KEY,
    participant_a_id        UUID         NOT NULL,
    participant_b_id        UUID         NOT NULL,
    last_message_at         TIMESTAMP    NOT NULL,
    last_message_preview    VARCHAR(200),
    last_message_sender_id  UUID,
    a_last_read_at          TIMESTAMP,
    b_last_read_at          TIMESTAMP,
    created_at              TIMESTAMP    NOT NULL
);

-- 정규화된 참여자 쌍당 대화방 하나(중복 생성 방지 + get-or-create 결정성)
CREATE UNIQUE INDEX idx_conversations_pair ON conversations (participant_a_id, participant_b_id);
CREATE INDEX        idx_conversations_a    ON conversations (participant_a_id);
CREATE INDEX        idx_conversations_b    ON conversations (participant_b_id);

CREATE TABLE messages (
    id               UUID          PRIMARY KEY,
    conversation_id  UUID          NOT NULL,
    sender_id        UUID          NOT NULL,
    body             VARCHAR(2000) NOT NULL,
    created_at       TIMESTAMP     NOT NULL
);

-- 스레드 시간순 조회 + 안읽은 수 카운트를 받치는 복합 인덱스
CREATE INDEX idx_messages_conversation ON messages (conversation_id, created_at);
