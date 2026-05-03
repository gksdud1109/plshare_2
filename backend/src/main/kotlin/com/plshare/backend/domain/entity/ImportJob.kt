package com.plshare.backend.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

enum class ImportJobStatus {
    QUEUED, RUNNING, COMPLETED, FAILED
}

@Entity
@Table(name = "import_jobs", indexes = [Index(name = "idx_import_idempotency_key", columnList = "idempotencyKey", unique = true)])
class ImportJob(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    val idempotencyKey: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ImportJobStatus = ImportJobStatus.QUEUED,

    var spotifyPlaylistId: String? = null,
    var assetId: UUID? = null,
    var totalTracks: Int = 0,
    var processedTracks: Int = 0,
    var errorCode: String? = null,
    var errorMessage: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun start() {
        if (this.status != ImportJobStatus.QUEUED) throw IllegalStateException("Job is already started or finished")
        this.status = ImportJobStatus.RUNNING
        this.updatedAt = LocalDateTime.now()
    }

    fun complete() {
        this.status = ImportJobStatus.COMPLETED
        this.updatedAt = LocalDateTime.now()
    }

    fun fail(errorCode: String, errorMessage: String) {
        this.status = ImportJobStatus.FAILED
        this.errorCode = errorCode
        this.errorMessage = errorMessage
        this.updatedAt = LocalDateTime.now()
    }

    fun updateProgress(processed: Int) {
        this.processedTracks = processed
        this.updatedAt = LocalDateTime.now()
    }
}
