package com.plshare.backend.domain.export.repository

import com.plshare.backend.domain.export.model.ExportJob
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ExportJobRepository : JpaRepository<ExportJob, UUID> {
    fun findByIdempotencyKey(idempotencyKey: String): ExportJob?
}
