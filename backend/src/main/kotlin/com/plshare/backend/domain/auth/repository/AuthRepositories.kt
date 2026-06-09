package com.plshare.backend.domain.auth.repository

import com.plshare.backend.domain.auth.model.OauthHandshake
import com.plshare.backend.domain.auth.model.SpotifyAccessGrant
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OauthHandshakeRepository : JpaRepository<OauthHandshake, UUID> {
    fun findByState(state: String): OauthHandshake?
}

interface SpotifyAccessGrantRepository : JpaRepository<SpotifyAccessGrant, UUID>
