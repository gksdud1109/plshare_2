package com.plshare.backend.domain.importing.repository

import com.plshare.backend.domain.importing.model.ImportJob
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ImportJobRepository : JpaRepository<ImportJob, UUID> {
    fun findByIdempotencyKey(idempotencyKey: String): ImportJob?
}
