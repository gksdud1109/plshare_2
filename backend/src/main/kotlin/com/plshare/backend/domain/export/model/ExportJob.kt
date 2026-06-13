package com.plshare.backend.domain.export.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

enum class ExportJobStatus {
    QUEUED, MATCHING, READY, EXECUTING, COMPLETED, PARTIAL, FAILED
}

@Entity
@Table(
    name = "export_jobs",
    indexes = [Index(name = "idx_export_idempotency_key", columnList = "idempotencyKey", unique = true)]
)
class ExportJob(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "owner_id")
    val ownerId: UUID? = null,

    @Column(nullable = false)
    val assetId: UUID,

    @Column(nullable = false)
    val targetPlatform: String = "youtube",

    @Column(nullable = false, unique = true)
    val idempotencyKey: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ExportJobStatus = ExportJobStatus.QUEUED,

    var externalPlaylistId: String? = null,
    var externalUrl: String? = null,

    var totalTracks: Int = 0,
    var matchedTracks: Int = 0,
    var failedTracks: Int = 0,

    var startedAt: LocalDateTime? = null,
    var finishedAt: LocalDateTime? = null,
    var lastError: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun start() {
        this.status = ExportJobStatus.MATCHING
        this.startedAt = LocalDateTime.now()
        this.updatedAt = LocalDateTime.now()
    }

    fun markMatching() {
        this.status = ExportJobStatus.MATCHING
        this.updatedAt = LocalDateTime.now()
    }

    fun markReady() {
        this.status = ExportJobStatus.READY
        this.updatedAt = LocalDateTime.now()
    }

    fun markExecuting() {
        this.status = ExportJobStatus.EXECUTING
        this.updatedAt = LocalDateTime.now()
    }

    fun complete(externalPlaylistId: String, externalUrl: String?, matched: Int, failed: Int) {
        this.externalPlaylistId = externalPlaylistId
        this.externalUrl = externalUrl
        this.matchedTracks = matched
        this.failedTracks = failed
        this.status = if (failed == 0) ExportJobStatus.COMPLETED else ExportJobStatus.PARTIAL
        this.finishedAt = LocalDateTime.now()
        this.updatedAt = LocalDateTime.now()
    }

    fun partial(matched: Int, failed: Int) {
        this.matchedTracks = matched
        this.failedTracks = failed
        this.status = ExportJobStatus.PARTIAL
        this.finishedAt = LocalDateTime.now()
        this.updatedAt = LocalDateTime.now()
    }

    fun fail(message: String) {
        this.status = ExportJobStatus.FAILED
        this.lastError = message
        this.finishedAt = LocalDateTime.now()
        this.updatedAt = LocalDateTime.now()
    }
}
