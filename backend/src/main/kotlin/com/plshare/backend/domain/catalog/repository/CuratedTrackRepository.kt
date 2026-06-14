package com.plshare.backend.domain.catalog.repository

import com.plshare.backend.domain.catalog.model.CuratedTrack
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CuratedTrackRepository : JpaRepository<CuratedTrack, UUID> {
    fun findByMoodOrderByArtistAsc(mood: String): List<CuratedTrack>
    fun findAllByOrderByMoodAscArtistAsc(): List<CuratedTrack>
}
