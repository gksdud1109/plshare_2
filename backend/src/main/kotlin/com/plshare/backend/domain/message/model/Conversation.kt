package com.plshare.backend.domain.message.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * 1:1 대화방(쪽지) 엔티티.
 *
 * 두 사용자 사이에는 대화방이 정확히 하나만 존재한다. 이를 보장하기 위해
 * 참여자 UUID 쌍을 **정규화**해서 저장한다 — 항상 작은 쪽을 [participantAId],
 * 큰 쪽을 [participantBId]에 둔다(UUID 자연 정렬). (a, b) 유니크 인덱스로
 * 같은 쌍의 중복 생성을 막고, get-or-create 를 결정적으로 만든다.
 *
 * 읽음 상태는 참여자별 [aLastReadAt]/[bLastReadAt] 로 관리한다.
 * 안 읽은 메시지 수 = (내가 보낸 게 아니면서) createdAt 이 내 lastReadAt 이후인 메시지 수.
 *
 * 마지막 메시지(미리보기/시각/발신자)는 목록 화면용으로 비정규화해 둔다 —
 * 대화 목록을 메시지 테이블 조인 없이 한 번에 정렬·렌더하기 위함.
 */
@Entity
@Table(
    name = "conversations",
    indexes = [
        Index(name = "idx_conversations_pair", columnList = "participant_a_id, participant_b_id", unique = true),
        Index(name = "idx_conversations_a", columnList = "participant_a_id"),
        Index(name = "idx_conversations_b", columnList = "participant_b_id"),
    ]
)
class Conversation(
    @Id
    val id: UUID = UUID.randomUUID(),

    /** 정규화된 참여자 쌍 중 작은 UUID. */
    @Column(name = "participant_a_id", nullable = false)
    val participantAId: UUID,

    /** 정규화된 참여자 쌍 중 큰 UUID. */
    @Column(name = "participant_b_id", nullable = false)
    val participantBId: UUID,

    @Column(name = "last_message_at", nullable = false)
    var lastMessageAt: LocalDateTime = LocalDateTime.now(),

    /** 목록용 미리보기(최대 200자). */
    @Column(name = "last_message_preview", length = 200)
    var lastMessagePreview: String? = null,

    @Column(name = "last_message_sender_id")
    var lastMessageSenderId: UUID? = null,

    /** participantA 가 마지막으로 읽은 시각. */
    @Column(name = "a_last_read_at")
    var aLastReadAt: LocalDateTime? = null,

    /** participantB 가 마지막으로 읽은 시각. */
    @Column(name = "b_last_read_at")
    var bLastReadAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    fun isParticipant(userId: UUID): Boolean =
        userId == participantAId || userId == participantBId

    /** 상대 참여자 UUID. 호출 전 [isParticipant] 가 참임을 전제한다. */
    fun otherParticipant(userId: UUID): UUID =
        if (userId == participantAId) participantBId else participantAId

    fun lastReadAtFor(userId: UUID): LocalDateTime? =
        if (userId == participantAId) aLastReadAt else bLastReadAt

    /** 해당 참여자의 읽음 시각을 갱신한다. */
    fun markReadFor(userId: UUID, at: LocalDateTime) {
        if (userId == participantAId) aLastReadAt = at else bLastReadAt = at
    }
}
