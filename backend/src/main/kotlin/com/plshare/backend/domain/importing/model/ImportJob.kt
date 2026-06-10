package com.plshare.backend.domain.importing.model

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

    /**
     * 하위호환 필드: Spotify import 초기부터 사용된 컬럼.
     * 신규 코드는 [sourcePlatform] + [sourcePlaylistId] 범용 필드를 사용한다.
     * sourcePlatform="spotify" 인 경우 sourcePlaylistId 와 동일한 값이 저장된다.
     */
    var spotifyPlaylistId: String? = null,

    /**
     * 소스 플랫폼 식별자. 기본값 "spotify" (기존 FE/E2E 요청 하위호환).
     * 지원값: "spotify", "youtube"
     */
    @Column(nullable = false)
    var sourcePlatform: String = "spotify",

    /**
     * 범용 소스 플레이리스트 ID. 플랫폼에 따라 Spotify playlistId 또는 YouTube playlistId.
     * spotifyPlaylistId 와 중복이지만 multi-source 확장을 위한 범용 필드로 분리한다.
     * Spotify import 시 spotifyPlaylistId 도 함께 채워 기존 코드와의 호환을 유지한다.
     */
    var sourcePlaylistId: String? = null,

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
