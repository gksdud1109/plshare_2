package com.plshare.backend.domain.repository

import com.plshare.backend.domain.entity.Asset
import com.plshare.backend.domain.entity.ExportJob
import com.plshare.backend.domain.entity.ImportJob
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ImportJobRepository : JpaRepository<ImportJob, UUID> {
    fun findByIdempotencyKey(idempotencyKey: String): ImportJob?
}

interface AssetRepository : JpaRepository<Asset, UUID> {
    fun findByShareToken(shareToken: String): Asset?
}

interface ExportJobRepository : JpaRepository<ExportJob, UUID> {
    fun findByIdempotencyKey(idempotencyKey: String): ExportJob?
}
