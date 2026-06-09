package com.plshare.backend.domain.asset.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "assets")
class Asset(
    @Id
    val id: UUID = UUID.randomUUID(),

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

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "tracks", indexes = [Index(name = "idx_track_isrc", columnList = "isrc")])
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

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
