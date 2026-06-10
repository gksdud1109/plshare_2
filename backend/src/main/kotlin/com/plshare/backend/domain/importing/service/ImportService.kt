package com.plshare.backend.domain.importing.service

import com.plshare.backend.domain.importing.model.ImportJob
import com.plshare.backend.domain.importing.repository.ImportJobRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class ImportService(
    private val importJobRepository: ImportJobRepository,
    private val normalizationEngine: NormalizationEngine
) {
    /**
     * 멱등성(Idempotency) 보장 로직을 포함한 Import 요청 처리.
     *
     * [sourcePlatform] 기본값 "spotify" — 기존 FE/E2E 호출 하위호환.
     * Spotify import 시 [spotifyPlaylistId] 도 동시에 채워 기존 NormalizationEngine과의 호환 유지.
     */
    @Transactional
    fun requestImport(
        idempotencyKey: String,
        playlistId: String,
        sourcePlatform: String = "spotify"
    ): UUID {
        // 1. 기존 멱등성 키 존재 여부 확인
        val existingJob = importJobRepository.findByIdempotencyKey(idempotencyKey)
        if (existingJob != null) {
            return existingJob.id // 기존 작업 ID 반환 (중복 생성 방지)
        }

        // 2. 새 작업 생성 (Queued 상태)
        val newJob = ImportJob(
            idempotencyKey = idempotencyKey,
            sourcePlatform = sourcePlatform,
            sourcePlaylistId = playlistId,
            // spotifyPlaylistId는 기존 NormalizationEngine spotify 경로와의 호환을 위해 유지
            spotifyPlaylistId = if (sourcePlatform == "spotify") playlistId else null
        )
        importJobRepository.save(newJob)

        // 3. 비동기 정규화 작업 시작
        normalizationEngine.runNormalization(newJob.id)

        return newJob.id
    }
}
