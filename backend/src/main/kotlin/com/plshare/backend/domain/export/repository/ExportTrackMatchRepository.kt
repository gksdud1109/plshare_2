package com.plshare.backend.domain.export.repository

import com.plshare.backend.domain.export.model.ExportTrackMatch
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ExportTrackMatchRepository : JpaRepository<ExportTrackMatch, UUID> {
    fun findByExportJobId(exportJobId: UUID): List<ExportTrackMatch>
    fun findByExportJobIdAndTrackId(exportJobId: UUID, trackId: UUID): ExportTrackMatch?
}
