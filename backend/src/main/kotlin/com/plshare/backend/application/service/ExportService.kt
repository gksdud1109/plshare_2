package com.plshare.backend.application.service

import com.plshare.backend.domain.entity.ExportJob
import com.plshare.backend.domain.repository.AssetRepository
import com.plshare.backend.domain.repository.ExportJobRepository
import com.plshare.backend.infrastructure.apple.AppleMusicTrackInput
import com.plshare.backend.infrastructure.apple.AppleMusicWriteAdapter
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ExportService(
    private val exportJobRepository: ExportJobRepository,
    private val assetRepository: AssetRepository,
    private val appleAdapter: AppleMusicWriteAdapter
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun requestExport(idempotencyKey: String, assetId: UUID, targetPlatform: String): UUID {
        val existing = exportJobRepository.findByIdempotencyKey(idempotencyKey)
        if (existing != null) return existing.id

        val asset = assetRepository.findById(assetId).orElseThrow {
            IllegalArgumentException("Asset not found: $assetId")
        }

        val job = ExportJob(
            assetId = asset.id,
            targetPlatform = targetPlatform,
            idempotencyKey = idempotencyKey,
            totalTracks = asset.tracks.size
        )
        exportJobRepository.save(job)

        runExport(job.id)

        return job.id
    }

    @Async
    fun runExport(jobId: UUID) {
        val job = exportJobRepository.findById(jobId).orElse(null) ?: run {
            log.error("Export job not found: $jobId")
            return
        }
        try {
            job.start()
            exportJobRepository.save(job)

            val asset = assetRepository.findById(job.assetId).orElseThrow()
            val inputs = asset.tracks.map {
                AppleMusicTrackInput(isrc = it.isrc, title = it.name, artist = it.artist)
            }

            // Create playlist
            val ref = appleAdapter.createPlaylist(asset.title, asset.description).block()
                ?: throw IllegalStateException("Apple playlist creation failed")

            job.markReady()
            exportJobRepository.save(job)

            job.markExecuting()
            exportJobRepository.save(job)

            val addResult = appleAdapter.addTracks(ref, inputs).block()
                ?: throw IllegalStateException("Apple addTracks failed")

            // verify
            appleAdapter.verify(ref, addResult.matched).block()

            job.complete(
                externalPlaylistId = ref.externalId,
                externalUrl = ref.externalUrl,
                matched = addResult.matched,
                failed = addResult.skipped
            )
            exportJobRepository.save(job)
        } catch (e: Exception) {
            log.error("Export failed for job $jobId", e)
            job.fail(e.message ?: "unknown_error")
            exportJobRepository.save(job)
        }
    }
}
