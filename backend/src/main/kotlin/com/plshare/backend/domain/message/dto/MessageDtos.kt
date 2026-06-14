package com.plshare.backend.domain.message.dto

import com.plshare.backend.domain.message.model.Message
import com.plshare.backend.domain.user.model.User
import java.time.LocalDateTime
import java.util.UUID

/** POST /api/conversations 요청 — 상대 handle 로 대화방 get-or-create. */
data class StartConversationRequest(
    val recipientHandle: String,
)

/** POST /api/conversations/{id}/messages 요청. */
data class SendMessageRequest(
    val body: String,
)

/** 대화 상대(내가 아닌 참여자) 요약. */
data class ConversationPartnerDto(
    val handle: String,
    val displayName: String,
    val avatarUrl: String?,
) {
    companion object {
        fun from(user: User) = ConversationPartnerDto(
            handle = user.handle,
            displayName = user.displayName,
            avatarUrl = user.avatarUrl,
        )

        /** 참여자 레코드가 사라진 경우의 방어적 폴백(목록이 깨지지 않게). */
        val UNKNOWN = ConversationPartnerDto(
            handle = "unknown",
            displayName = "(알 수 없는 사용자)",
            avatarUrl = null,
        )
    }
}

/** 대화 목록/헤더용 요약. */
data class ConversationSummaryDto(
    val id: UUID,
    val partner: ConversationPartnerDto,
    val lastMessagePreview: String?,
    val lastMessageAt: LocalDateTime,
    /** 마지막 메시지를 내가 보냈는지(목록에서 "나: …" 접두 표시용). */
    val lastMessageMine: Boolean,
    val unreadCount: Long,
)

/** 스레드 내 단일 메시지. */
data class MessageDto(
    val id: UUID,
    val senderId: UUID,
    /** 내가 보낸 메시지인지(말풍선 좌/우 정렬용). */
    val mine: Boolean,
    val body: String,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(message: Message, viewerId: UUID) = MessageDto(
            id = message.id,
            senderId = message.senderId,
            mine = message.senderId == viewerId,
            body = message.body,
            createdAt = message.createdAt,
        )
    }
}

/** GET /api/conversations/{id}/messages 응답 — 헤더 + 메시지 전체. */
data class ThreadResponse(
    val conversation: ConversationSummaryDto,
    val messages: List<MessageDto>,
)

/** GET /api/conversations/unread-count 응답 — 네비 배지용. */
data class UnreadCountResponse(
    val total: Long,
)
