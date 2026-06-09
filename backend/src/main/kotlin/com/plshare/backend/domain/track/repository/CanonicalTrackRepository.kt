package com.plshare.backend.domain.track.repository

import com.plshare.backend.domain.track.model.CanonicalTrack
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface CanonicalTrackRepository : JpaRepository<CanonicalTrack, UUID> {
    fun findByIsrc(isrc: String): CanonicalTrack?
    fun findBySpotifyTrackId(id: String): CanonicalTrack?
    fun findByAppleSongId(id: String): CanonicalTrack?
    fun findByTitleAndArtists(title: String, artists: String): List<CanonicalTrack>

    /**
     * Fuzzy 매칭 후보 조회용. 정규화된 title의 짧은 prefix로 LIKE 'prefix%' 검색.
     * Bloom-filter 등 본격 인덱스가 도입되기 전까지 후보군 좁히기 용도.
     */
    @Query("SELECT c FROM CanonicalTrack c WHERE c.title LIKE CONCAT(:titlePrefix, '%')")
    fun findCandidatesForFuzzyMatch(@Param("titlePrefix") titlePrefix: String): List<CanonicalTrack>
}
