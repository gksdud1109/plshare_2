package com.plshare.backend.domain.catalog.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.UUID

/**
 * 큐레이션 카탈로그 트랙 — **재생 검증된(oEmbed 통과) YouTube videoId가 미리 박혀 있다.**
 *
 * 유저는 이 카탈로그에서 곡을 골라 플레이리스트(Asset)를 조립한다. 조립 시 Track.youtubeVideoId 로
 * 그대로 복사되므로 [TrackPlaybackService] 가 검색 없이 단락 재생한다 → prod 에서 YouTube Data API
 * 검색/쿼터/키 없이 100% 재생. (임의 곡 import 의 prod 재생 구멍을 우회하는 MVP 기반.)
 */
@Entity
@Table(name = "curated_tracks", indexes = [Index(name = "idx_curated_mood", columnList = "mood")])
class CuratedTrack(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false)
    val artist: String,

    @Column(name = "youtube_video_id", nullable = false, length = 16)
    val youtubeVideoId: String,

    @Column(name = "duration_ms")
    val durationMs: Int? = null,

    @Column(nullable = false, length = 32)
    val mood: String,

    @Column(name = "cover_url", length = 512)
    val coverUrl: String? = null,
)
