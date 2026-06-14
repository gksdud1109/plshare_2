package com.plshare.backend.domain.message.repository

import com.plshare.backend.domain.message.model.Conversation
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ConversationRepository : JpaRepository<Conversation, UUID> {

    /** 정규화된 참여자 쌍으로 조회(get-or-create 용). a < b 전제. */
    fun findByParticipantAIdAndParticipantBId(a: UUID, b: UUID): Conversation?

    /**
     * 특정 사용자가 참여한 모든 대화 — 최근 메시지 순.
     * userId 를 두 슬롯(a, b) 모두에 넘긴다.
     */
    fun findByParticipantAIdOrParticipantBIdOrderByLastMessageAtDesc(a: UUID, b: UUID): List<Conversation>
}
