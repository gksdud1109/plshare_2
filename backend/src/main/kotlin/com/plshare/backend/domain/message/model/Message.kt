package com.plshare.backend.domain.message.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * 대화방 내 단일 쪽지.
 *
 * (conversation_id, created_at) 복합 인덱스로 스레드 시간순 조회를 받친다.
 * 본문은 최대 2000자(짧은 안부 ~ 긴 답장).
 */
@Entity
@Table(
    name = "messages",
    indexes = [
        Index(name = "idx_messages_conversation", columnList = "conversation_id, created_at"),
    ]
)
class Message(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "conversation_id", nullable = false)
    val conversationId: UUID,

    @Column(name = "sender_id", nullable = false)
    val senderId: UUID,

    @Column(nullable = false, length = 2000)
    val body: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
