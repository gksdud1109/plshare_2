package com.plshare.backend.domain.export.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * Per-track export match outcome.
 *
 *  - MATCHED     : a target video was found with high confidence (>= threshold),
 *                  or the user manually confirmed/overrode it, or the source
 *                  already carried a YouTube videoId.
 *  - ALTERNATIVE : a video was found but the title/artist similarity is below the
 *                  confidence threshold — the user should review and confirm.
 *  - FAILED      : no candidate video could be resolved.
 */
enum class ExportMatchStatus {
    MATCHED, ALTERNATIVE, FAILED
}

/**
 * Persisted result of matching one source track to a target-platform video during
 * export. Previously the per-track outcome was computed in-memory and discarded
 * (only matched/failed counts survived on [ExportJob]); persisting it lets the
 * user review low-confidence (`ALTERNATIVE`) and `FAILED` matches and override
 * them with a chosen candidate.
 */
@Entity
@Table(
    name = "export_track_matches",
    indexes = [Index(name = "idx_export_track_match_job", columnList = "export_job_id")]
)
class ExportTrackMatch(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "export_job_id", nullable = false)
    val exportJobId: UUID,

    @Column(name = "track_id", nullable = false)
    val trackId: UUID,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false)
    val artist: String,

    @Column(name = "matched_video_id")
    var matchedVideoId: String? = null,

    @Column(name = "matched_title")
    var matchedTitle: String? = null,

    @Column(name = "confidence")
    var confidence: Double? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ExportMatchStatus = ExportMatchStatus.FAILED,

    @Column(nullable = false)
    var reviewed: Boolean = false,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    /** User picked a specific video for this track — treat as a confirmed match. */
    fun applyOverride(videoId: String, videoTitle: String?) {
        this.matchedVideoId = videoId
        this.matchedTitle = videoTitle
        this.status = ExportMatchStatus.MATCHED
        this.confidence = 1.0
        this.reviewed = true
        this.updatedAt = LocalDateTime.now()
    }
}
