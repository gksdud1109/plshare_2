package com.plshare.backend.domain.gift.repository

import com.plshare.backend.domain.gift.model.Gift
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GiftRepository : JpaRepository<Gift, UUID> {
    fun findByToken(token: String): Gift?
}
