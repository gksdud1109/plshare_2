package com.plshare.backend.application.service

import com.plshare.backend.domain.entity.Asset
import com.plshare.backend.domain.entity.Track
import com.plshare.backend.domain.repository.AssetRepository
import com.plshare.backend.domain.repository.ImportJobRepository
import com.plshare.backend.infrastructure.spotify.SpotifyClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NormalizationEngine(
    private val importJobRepository: ImportJobRepository,
    private val assetRepository: AssetRepository,
    private val spotifyClient: SpotifyClient
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * 비동기 정규화 실행. 
     * 외부 API 호출과 DB 트랜잭션을 분리하여 장애 전파를 차단한다.
     */
    @Async
    fun runNormalization(jobId: java.util.UUID) {
        val job = importJobRepository.findById(jobId).orElseThrow()
        
        try {
            job.start()
            importJobRepository.save(job)

            // 1. 외부 API 호출 (Transaction 밖에서 수행 - Isolation 원칙)
            val accessToken = spotifyClient.getAccessToken().block() 
                ?: throw IllegalStateException("Failed to get Spotify access token")
            
            val playlist = spotifyClient.getPlaylist(job.spotifyPlaylistId!!, accessToken).block()
                ?: throw IllegalStateException("Failed to get Spotify playlist")

            // 2. 정규화 및 데이터 저장 (별도 트랜잭션으로 처리)
            saveNormalizedAsset(jobId, playlist)

        } catch (e: Exception) {
            log.error("Normalization failed for job $jobId", e)
            job.fail("IMPORT_FAILED", e.message ?: "Unknown error")
            importJobRepository.save(job)
        }
    }

    @Transactional
    fun saveNormalizedAsset(jobId: java.util.UUID, playlist: com.plshare.backend.infrastructure.spotify.SpotifyPlaylistResponse) {
        val job = importJobRepository.findById(jobId).orElseThrow()
        
        // Asset 생성
        val asset = Asset(
            title = playlist.name,
            coverUrl = playlist.images.firstOrNull()?.url,
            sourcePlatform = "spotify"
        )

        // Track 정규화 (ISRC 기준)
        val tracks = playlist.tracks.items.map { item ->
            Track(
                asset = asset,
                name = item.track.name,
                artist = item.track.artists.joinToString { it.name },
                isrc = item.track.isrc,
                spotifyId = item.track.id
            )
        }
        asset.tracks.addAll(tracks)
        
        assetRepository.save(asset)

        job.complete()
        job.updateProgress(tracks.size)
        importJobRepository.save(job)
    }
}
