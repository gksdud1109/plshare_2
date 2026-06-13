package com.plshare.backend.domain.track.repository

import com.plshare.backend.domain.asset.model.Track
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TrackRepository : JpaRepository<Track, UUID>
