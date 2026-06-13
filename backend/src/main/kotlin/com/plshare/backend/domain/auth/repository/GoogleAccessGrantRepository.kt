package com.plshare.backend.domain.auth.repository

import com.plshare.backend.domain.auth.model.GoogleAccessGrant
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GoogleAccessGrantRepository : JpaRepository<GoogleAccessGrant, UUID> {
    fun findFirstByUserIdOrderByUpdatedAtDesc(userId: UUID): GoogleAccessGrant?
}
