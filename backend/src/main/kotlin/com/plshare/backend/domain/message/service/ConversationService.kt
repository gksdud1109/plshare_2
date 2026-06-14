package com.plshare.backend.domain.message.service

import com.plshare.backend.domain.message.dto.*
import com.plshare.backend.domain.message.model.Conversation
import com.plshare.backend.domain.message.model.Message
import com.plshare.backend.domain.message.repository.ConversationRepository
import com.plshare.backend.domain.message.repository.MessageRepository
import com.plshare.backend.domain.user.repository.UserRepository
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

/**
 * 1:1 쪽지 도메인 서비스.
 *
 * 불변식:
 * - 대화방은 참여자 쌍당 정확히 하나(정규화 쌍 + 유니크 인덱스). get-or-create 는 멱등.
 * - 자기 자신에게는 대화를 열 수 없다(VALIDATION_FAILED).
 * - 스레드 조회/전송은 참여자만 가능(아니면 FORBIDDEN, 없으면 NOT_FOUND).
 * - 스레드를 조회하거나 메시지를 보내면 호출자의 읽음 시각이 갱신된다.
 * - 안 읽은 수 = 내가 보낸 게 아니면서 createdAt > 내 lastReadAt 인 메시지 수.
 */
@Service
@Transactional(readOnly = true)
class ConversationService(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
) {
    /** lastReadAt 이 null 일 때(한 번도 안 읽음) 안 읽은 수 계산의 하한. */
    private val epoch = LocalDateTime.of(1970, 1, 1, 0, 0)

    /** UUID 자연 정렬로 (작은, 큰) 쌍을 만든다. */
    private fun canonicalPair(x: UUID, y: UUID): Pair<UUID, UUID> =
        if (x < y) x to y else y to x

    /**
     * 상대 handle 로 대화방을 가져오거나 만든다(멱등).
     * 동시 생성 경합 시 유니크 인덱스가 보호하므로, 충돌하면 재조회한다.
     */
    @Transactional
    fun startConversation(meId: UUID, recipientHandle: String): ConversationSummaryDto {
        val recipient = userRepository.findByHandle(recipientHandle)
            ?: throw ApiException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다: $recipientHandle")
        if (recipient.id == meId) {
            throw ApiException(ErrorCode.VALIDATION_FAILED, "자기 자신에게는 쪽지를 보낼 수 없어요")
        }
        val (a, b) = canonicalPair(meId, recipient.id)
        val conversation = conversationRepository.findByParticipantAIdAndParticipantBId(a, b)
            ?: try {
                conversationRepository.save(Conversation(participantAId = a, participantBId = b))
            } catch (e: DataIntegrityViolationException) {
                conversationRepository.findByParticipantAIdAndParticipantBId(a, b)
                    ?: throw e
            }
        return toSummary(conversation, meId)
    }

    /** 내가 참여한 대화 목록 — 최근 메시지 순. */
    fun listConversations(meId: UUID): List<ConversationSummaryDto> =
        conversationRepository
            .findByParticipantAIdOrParticipantBIdOrderByLastMessageAtDesc(meId, meId)
            .map { toSummary(it, meId) }

    /** 전체 안 읽은 메시지 수(네비 배지). */
    fun unreadTotal(meId: UUID): Long =
        conversationRepository
            .findByParticipantAIdOrParticipantBIdOrderByLastMessageAtDesc(meId, meId)
            .sumOf { unreadCountFor(it, meId) }

    /**
     * 스레드 조회 — 참여자 검증. `after` 가 있으면 그 시각 이후 메시지만(증분 폴링).
     *
     * 순수 읽기(사이드이펙트 없음). 읽음 처리는 [markRead] 로 분리했다 —
     * GET 의 부수효과로 읽음을 갱신하면 (a) 프리페치/중복요청에 취약하고
     * (b) 폴링→SSE→Realtime 전환 시 전송계층마다 읽음 시맨틱이 새는 것을 막는다.
     */
    fun getThread(conversationId: UUID, meId: UUID, after: LocalDateTime? = null): ThreadResponse {
        val conversation = requireParticipant(conversationId, meId)
        val messages = if (after != null) {
            messageRepository.findByConversationIdAndCreatedAtAfterOrderByCreatedAtAsc(conversationId, after)
        } else {
            messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
        }
        return ThreadResponse(
            conversation = toSummary(conversation, meId),
            messages = messages.map { MessageDto.from(it, meId) },
        )
    }

    /** 읽음 처리 — 멱등. 호출자의 lastReadAt 을 현재로 갱신(전송계층 무관 단일 진실). */
    @Transactional
    fun markRead(conversationId: UUID, meId: UUID) {
        val conversation = requireParticipant(conversationId, meId)
        conversation.markReadFor(meId, LocalDateTime.now())
        conversationRepository.save(conversation)
    }

    /** 메시지 전송 — 참여자 검증 후 저장 + 대화방 비정규화 필드 갱신. */
    @Transactional
    fun sendMessage(conversationId: UUID, meId: UUID, body: String): MessageDto {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) {
            throw ApiException(ErrorCode.VALIDATION_FAILED, "메시지 내용을 입력해주세요")
        }
        if (trimmed.length > 2000) {
            throw ApiException(ErrorCode.VALIDATION_FAILED, "메시지는 2000자를 초과할 수 없습니다")
        }
        val conversation = requireParticipant(conversationId, meId)

        val message = messageRepository.save(
            Message(conversationId = conversationId, senderId = meId, body = trimmed)
        )
        conversation.lastMessageAt = message.createdAt
        conversation.lastMessagePreview = trimmed.take(200)
        conversation.lastMessageSenderId = meId
        conversation.markReadFor(meId, message.createdAt) // 내가 보낸 건 읽은 것
        conversationRepository.save(conversation)

        return MessageDto.from(message, meId)
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private fun requireParticipant(conversationId: UUID, meId: UUID): Conversation {
        val conversation = conversationRepository.findById(conversationId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "대화를 찾을 수 없습니다: $conversationId")
        }
        if (!conversation.isParticipant(meId)) {
            throw ApiException(ErrorCode.FORBIDDEN, "이 대화에 접근할 권한이 없습니다")
        }
        return conversation
    }

    private fun unreadCountFor(conversation: Conversation, meId: UUID): Long {
        val since = conversation.lastReadAtFor(meId) ?: epoch
        return messageRepository.countByConversationIdAndSenderIdNotAndCreatedAtAfter(
            conversation.id, meId, since,
        )
    }

    private fun toSummary(conversation: Conversation, meId: UUID): ConversationSummaryDto {
        val partner = userRepository.findById(conversation.otherParticipant(meId))
            .map { ConversationPartnerDto.from(it) }
            .orElse(ConversationPartnerDto.UNKNOWN)
        return ConversationSummaryDto(
            id = conversation.id,
            partner = partner,
            lastMessagePreview = conversation.lastMessagePreview,
            lastMessageAt = conversation.lastMessageAt,
            lastMessageMine = conversation.lastMessageSenderId == meId,
            unreadCount = unreadCountFor(conversation, meId),
        )
    }
}
