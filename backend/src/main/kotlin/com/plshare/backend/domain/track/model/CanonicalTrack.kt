package com.plshare.backend.domain.track.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

/**
 * Canonical Track — cross-platform normalized track identity.
 *
 * 동일한 곡(예: Spotify track + Apple Music song)이 여러 플랫폼에서 표현될 때
 * ISRC 또는 fuzzy matching으로 하나의 canonical 레코드에 매핑된다.
 *
 * 참고: docs/20-product/data/canonical-track-normalization-v0.1.md
 *
 * 통합 메모(다음 단계, 별도 PR):
 *   NormalizationEngine.saveNormalizedAsset에서 MatchingEngine.matchOrCreate를
 *   호출해 Track ↔ CanonicalTrack 연결을 수행하도록 통합한다.
 */
@Entity
@Table(
    name = "canonical_tracks",
    indexes = [
        Index(name = "idx_canonical_isrc", columnList = "isrc", unique = true),
        Index(name = "idx_canonical_spotify_id", columnList = "spotify_track_id"),
        Index(name = "idx_canonical_apple_id", columnList = "apple_song_id"),
        Index(name = "idx_canonical_title_prefix", columnList = "title")
    ]
)
class CanonicalTrack(
    @Id
    @Column(name = "canonical_id")
    val canonicalId: UUID = UUID.randomUUID(),

    @Column(name = "isrc", unique = true)
    var isrc: String? = null,

    @Column(name = "spotify_track_id")
    var spotifyTrackId: String? = null,

    @Column(name = "apple_song_id")
    var appleSongId: String? = null,

    @Column(name = "title", nullable = false)
    var title: String,

    @Column(name = "artists", nullable = false)
    var artists: String,

    @Column(name = "album")
    var album: String? = null,

    @Column(name = "duration_ms", nullable = false)
    var durationMs: Long,

    @Column(name = "match_confidence", nullable = false)
    var matchConfidence: Double = 1.0,

    @Column(name = "sources", nullable = false)
    var sources: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
