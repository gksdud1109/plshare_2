package com.plshare.backend.domain.asset.repository

import com.plshare.backend.domain.asset.model.Asset
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AssetRepository : JpaRepository<Asset, UUID> {
    fun findByShareToken(shareToken: String): Asset?
}
