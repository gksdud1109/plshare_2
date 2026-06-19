package com.plshare.backend.domain.gift.repository

import com.plshare.backend.domain.gift.model.Gift
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GiftRepository : JpaRepository<Gift, UUID> {
    fun findByToken(token: String): Gift?
    fun existsByAssetId(assetId: UUID): Boolean

    /** 받은(저장한) 선물 — 최근 연 순. */
    fun findBySavedByUserIdOrderByOpenedAtDesc(savedByUserId: UUID): List<Gift>

    /** 보낸 선물 — 최근 생성 순. */
    fun findBySenderIdOrderByCreatedAtDesc(senderId: UUID): List<Gift>
}
