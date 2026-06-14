package com.plshare.backend.domain.catalog.service

import com.plshare.backend.domain.asset.model.Asset
import com.plshare.backend.domain.asset.model.Track
import com.plshare.backend.domain.asset.repository.AssetRepository
import com.plshare.backend.domain.catalog.dto.ComposeAssetRequest
import com.plshare.backend.domain.catalog.dto.ComposedAssetResponse
import com.plshare.backend.domain.catalog.dto.CuratedTrackDto
import com.plshare.backend.domain.catalog.repository.CuratedTrackRepository
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 큐레이션 카탈로그 서비스 — 검증된 트랙 풀 조회 + 트랙 선택을 내 플레이리스트(Asset)로 조립.
 * 조립 시 youtubeVideoId 를 그대로 복사해 prod 재생을 보장한다(검색/쿼터 불필요).
 */
@Service
@Transactional(readOnly = true)
class CatalogService(
    private val curatedTrackRepository: CuratedTrackRepository,
    private val assetRepository: AssetRepository,
) {

    fun listTracks(mood: String?): List<CuratedTrackDto> {
        val tracks = if (mood.isNullOrBlank()) {
            curatedTrackRepository.findAllByOrderByMoodAscArtistAsc()
        } else {
            curatedTrackRepository.findByMoodOrderByArtistAsc(mood)
        }
        return tracks.map { CuratedTrackDto.from(it) }
    }

    @Transactional
    fun compose(req: ComposeAssetRequest, ownerId: UUID): ComposedAssetResponse {
        val title = req.title.trim()
        if (title.isEmpty() || title.length > 100) {
            throw ApiException(ErrorCode.VALIDATION_FAILED, "제목은 1~100자여야 합니다")
        }
        val ids = req.trackIds.distinct()
        if (ids.isEmpty() || ids.size > MAX_TRACKS) {
            throw ApiException(ErrorCode.VALIDATION_FAILED, "트랙은 1~${MAX_TRACKS}곡이어야 합니다")
        }
        val byId = curatedTrackRepository.findAllById(ids).associateBy { it.id }
        if (byId.size != ids.size) {
            throw ApiException(ErrorCode.NOT_FOUND, "선택한 카탈로그 트랙을 찾을 수 없습니다")
        }

        val cover = req.coverUrl?.takeIf { it.isNotBlank() }
            ?: ids.firstNotNullOfOrNull { byId[it]?.coverUrl }
            ?: "https://picsum.photos/seed/${ownerId.hashCode().toUInt().toString(16)}/600/600"

        val asset = Asset(
            ownerId = ownerId,
            title = title,
            coverUrl = cover,
            description = req.description?.takeIf { it.isNotBlank() },
            sourcePlatform = "catalog",
            emotionTags = req.emotionTags.filter { it.isNotBlank() }.distinct().take(8).toMutableList(),
        )
        // 선택 순서 보존하며 카탈로그 → Track 복사(youtubeVideoId 그대로 → 재생 보장).
        ids.forEach { tid ->
            val c = byId.getValue(tid)
            asset.tracks.add(
                Track(
                    asset = asset,
                    name = c.title,
                    artist = c.artist,
                    durationMs = c.durationMs,
                    youtubeVideoId = c.youtubeVideoId,
                )
            )
        }
        assetRepository.save(asset)
        return ComposedAssetResponse(id = asset.id, title = asset.title, trackCount = asset.tracks.size)
    }

    companion object {
        private const val MAX_TRACKS = 30
    }
}
