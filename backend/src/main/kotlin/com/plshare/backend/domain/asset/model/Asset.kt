package com.plshare.backend.domain.asset.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "assets")
class Asset(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "owner_id")
    val ownerId: UUID? = null,

    @Column(nullable = false)
    var title: String,

    var coverUrl: String? = null,
    var description: String? = null,

    @Column(columnDefinition = "TEXT")
    var diaryText: String? = null,

    @Column(nullable = false)
    val sourcePlatform: String = "spotify",

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "asset_emotion_tags", joinColumns = [JoinColumn(name = "asset_id")])
    @Column(name = "tag")
    var emotionTags: MutableList<String> = mutableListOf(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "asset_photo_urls", joinColumns = [JoinColumn(name = "asset_id")])
    @Column(name = "url")
    var photoUrls: MutableList<String> = mutableListOf(),

    @Column(unique = true)
    var shareToken: String? = null,

    @OneToMany(mappedBy = "asset", cascade = [CascadeType.ALL], orphanRemoval = true)
    var tracks: MutableList<Track> = mutableListOf(),

    /**
     * 듀얼 포맷 분기. TRACKLIST(기존 — 개별 트랙 묶음) | MOOD_VIDEO(단일 유튜브 무드영상 한 단위).
     * 기본 TRACKLIST 라 기존 자산은 전부 유효. 단일 영상을 '트랙 1개짜리'로 욱여넣지 않는다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "asset_kind", nullable = false, length = 16)
    var assetKind: AssetKind = AssetKind.TRACKLIST,

    /** MOOD_VIDEO 일 때만: 끊김없는 믹스 영상의 youtubeVideoId. 임베드 1개라 prod 재생 자동 보장. */
    @Column(name = "mood_video_id", length = 32)
    var moodVideoId: String? = null,

    /** MOOD_VIDEO 크레딧용 채널명(예: 후알유). */
    @Column(name = "mood_channel_name", length = 120)
    var moodChannelName: String? = null,

    /** MOOD_VIDEO 수록곡 자유 텍스트(타임스탬프 허용). Track 으로 쪼개지 않는다. */
    @Column(name = "mood_track_list_text", columnDefinition = "TEXT")
    var moodTrackListText: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

/** 자산 포맷 — 듀얼 포맷 디스크리미네이터. */
enum class AssetKind {
    /** 개별 트랙 묶음(기존). 트랙별 YouTube 임베드. */
    TRACKLIST,

    /** 단일 유튜브 무드영상 한 단위. 임베드 1개 + 수록곡 텍스트. */
    MOOD_VIDEO,
}

@Entity
@Table(
    name = "tracks",
    indexes = [
        Index(name = "idx_track_isrc", columnList = "isrc"),
        Index(name = "idx_track_canonical_track_id", columnList = "canonical_track_id")
    ]
)
class Track(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    val asset: Asset,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val artist: String,

    var durationMs: Int? = null,

    val isrc: String? = null, // Normalization key

    // Platform specific metadata
    var spotifyId: String? = null,
    var appleMusicId: String? = null,

    /**
     * YouTube videoId (youtube 소스로 import된 트랙). YouTube write export가 이 값을 사용한다.
     * spotifyId에 끼워넣지 않고 전용 컬럼으로 둔다(식별자 의미 혼동 방지).
     */
    @Column(name = "youtube_video_id")
    var youtubeVideoId: String? = null,

    /**
     * MatchingEngine이 결정한 canonical_tracks 레코드의 PK.
     * nullable: import 직후에는 null이 될 수 없으나, 향후 재처리 전 임시 상태 허용.
     * canonical_tracks 테이블과 FK 없이 UUID만 보관 — 도메인 경계(asset vs track)를 넘는
     * 강결합 없이 canonical identity를 참조한다.
     */
    @Column(name = "canonical_track_id")
    var canonicalTrackId: UUID? = null,

    /**
     * MatchingEngine.matchOrCreate가 반환한 매칭 신뢰도(0.0–1.0).
     * ISRC exact match → 1.0, fuzzy fallback → 0.7 이상, 신규 생성 → 1.0.
     */
    @Column(name = "match_confidence")
    var matchConfidence: Double? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
