package com.plshare.backend.domain.message.repository

import com.plshare.backend.domain.message.model.Message
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.UUID

interface MessageRepository : JpaRepository<Message, UUID> {

    /** 스레드 시간순 메시지(초기 로드). */
    fun findByConversationIdOrderByCreatedAtAsc(conversationId: UUID): List<Message>

    /** 커서 이후 신규 메시지만(증분 폴링·Realtime 재사용). after = 클라가 가진 마지막 createdAt. */
    fun findByConversationIdAndCreatedAtAfterOrderByCreatedAtAsc(
        conversationId: UUID,
        after: LocalDateTime,
    ): List<Message>

    /**
     * 안 읽은 메시지 수: 내가 보낸 게 아니면서(senderId <>) createdAt 이 after 이후인 메시지.
     * after 에는 호출자의 lastReadAt(없으면 epoch)을 넘긴다.
     */
    fun countByConversationIdAndSenderIdNotAndCreatedAtAfter(
        conversationId: UUID,
        senderId: UUID,
        after: LocalDateTime,
    ): Long
}
