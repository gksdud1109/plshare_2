package com.plshare.backend.domain.catalog.service

import com.plshare.backend.domain.asset.model.Asset
import com.plshare.backend.domain.asset.model.AssetKind
import com.plshare.backend.domain.asset.model.Track
import com.plshare.backend.domain.asset.repository.AssetRepository
import com.plshare.backend.domain.catalog.dto.ComposeAssetRequest
import com.plshare.backend.domain.catalog.dto.ComposedAssetResponse
import com.plshare.backend.domain.catalog.dto.CreateMoodVideoRequest
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
    private val youTubeSelectionTokenCodec: YouTubeCatalogSelectionTokenCodec,
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
    fun compose(
        req: ComposeAssetRequest,
        ownerId: UUID,
        idempotencyKey: String,
    ): ComposedAssetResponse {
        existingResponse(ownerId, idempotencyKey)?.let { return it }
        val title = req.title.trim()
        if (title.isEmpty() || title.length > 100) {
            throw ApiException(ErrorCode.VALIDATION_FAILED, "제목은 1~100자여야 합니다")
        }
        val selections = resolveSelections(req)
        if (selections.isEmpty() || selections.size > MAX_TRACKS) {
            throw ApiException(ErrorCode.VALIDATION_FAILED, "트랙은 1~${MAX_TRACKS}곡이어야 합니다")
        }

        val cover = req.coverUrl?.takeIf { it.isNotBlank() }
            ?: selections.firstNotNullOfOrNull { it.coverUrl }
            ?: "https://picsum.photos/seed/${ownerId.hashCode().toUInt().toString(16)}/600/600"

        val asset = Asset(
            ownerId = ownerId,
            title = title,
            coverUrl = cover,
            description = req.description?.takeIf { it.isNotBlank() },
            sourcePlatform = "catalog",
            emotionTags = req.emotionTags.filter { it.isNotBlank() }.distinct().take(8).toMutableList(),
            composeIdempotencyKey = validateIdempotencyKey(idempotencyKey),
        )
        // 선택 순서 보존. Curated UUID는 DB에서, YouTube selectionId는 HMAC 검증된
        // 서버 payload에서 복원하므로 임의 client metadata를 신뢰하지 않는다.
        selections.forEach { selection ->
            asset.tracks.add(
                Track(
                    asset = asset,
                    name = selection.title,
                    artist = selection.artist,
                    durationMs = selection.durationMs,
                    youtubeVideoId = selection.youtubeVideoId,
                )
            )
        }
        assetRepository.save(asset)
        return ComposedAssetResponse(id = asset.id, title = asset.title, trackCount = asset.tracks.size)
    }

    /**
     * 단일 유튜브 무드영상을 MOOD_VIDEO 자산으로. tracks[] 없이 영상 1개라 prod 재생 자동 보장.
     * 수록곡은 자유 텍스트(trackListText)로 보존 — Track 으로 쪼개지 않는다.
     */
    @Transactional
    fun composeMoodVideo(
        req: CreateMoodVideoRequest,
        ownerId: UUID,
        idempotencyKey: String,
    ): ComposedAssetResponse {
        existingResponse(ownerId, idempotencyKey)?.let { return it }
        val title = req.title.trim()
        if (title.isEmpty() || title.length > 100) {
            throw ApiException(ErrorCode.VALIDATION_FAILED, "제목은 1~100자여야 합니다")
        }
        val videoId = extractVideoId(req.videoUrlOrId)
            ?: throw ApiException(ErrorCode.VALIDATION_FAILED, "유효한 YouTube 영상 URL 또는 ID가 아니에요")

        val cover = req.coverUrl?.takeIf { it.isNotBlank() }
            ?: "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"

        val asset = Asset(
            ownerId = ownerId,
            title = title,
            coverUrl = cover,
            sourcePlatform = "youtube",
            emotionTags = req.emotionTags.filter { it.isNotBlank() }.distinct().take(8).toMutableList(),
            assetKind = AssetKind.MOOD_VIDEO,
            moodVideoId = videoId,
            moodChannelName = req.channelName?.takeIf { it.isNotBlank() },
            moodTrackListText = req.trackListText?.takeIf { it.isNotBlank() },
            composeIdempotencyKey = validateIdempotencyKey(idempotencyKey),
        )
        assetRepository.save(asset)
        return ComposedAssetResponse(id = asset.id, title = asset.title, trackCount = 0)
    }

    /** YouTube 전체 URL(watch/youtu.be/embed/shorts) 또는 11자 raw id 에서 videoId 추출. */
    private fun extractVideoId(input: String): String? {
        val s = input.trim()
        val patterns = listOf(
            Regex("""[?&]v=([A-Za-z0-9_-]{11})"""),
            Regex("""youtu\.be/([A-Za-z0-9_-]{11})"""),
            Regex("""/embed/([A-Za-z0-9_-]{11})"""),
            Regex("""/shorts/([A-Za-z0-9_-]{11})"""),
        )
        for (p in patterns) p.find(s)?.let { return it.groupValues[1] }
        if (Regex("""^[A-Za-z0-9_-]{11}$""").matches(s)) return s
        return null
    }

    private fun existingResponse(ownerId: UUID, idempotencyKey: String): ComposedAssetResponse? {
        val key = validateIdempotencyKey(idempotencyKey)
        return assetRepository.findByOwnerIdAndComposeIdempotencyKey(ownerId, key)?.let {
            ComposedAssetResponse(id = it.id, title = it.title, trackCount = it.tracks.size)
        }
    }

    private fun resolveSelections(req: ComposeAssetRequest): List<ComposableTrack> {
        if (req.trackIds.isNotEmpty() && req.selectionIds.isNotEmpty()) {
            throw ApiException(
                ErrorCode.VALIDATION_FAILED,
                "trackIds와 selectionIds는 동시에 보낼 수 없습니다",
            )
        }
        val requestedCount = if (req.selectionIds.isNotEmpty()) {
            req.selectionIds.distinct().size
        } else {
            req.trackIds.distinct().size
        }
        if (requestedCount == 0 || requestedCount > MAX_TRACKS) {
            throw ApiException(ErrorCode.VALIDATION_FAILED, "트랙은 1~${MAX_TRACKS}곡이어야 합니다")
        }

        if (req.selectionIds.isEmpty()) {
            val ids = req.trackIds.distinct()
            val byId = curatedTrackRepository.findAllById(ids).associateBy { it.id }
            if (byId.size != ids.size) {
                throw ApiException(ErrorCode.NOT_FOUND, "선택한 카탈로그 트랙을 찾을 수 없습니다")
            }
            return ids.map { id -> byId.getValue(id).toComposableTrack() }
        }

        val orderedIds = req.selectionIds.map { it.trim() }.distinct()
        if (orderedIds.any { it.isEmpty() }) {
            throw ApiException(ErrorCode.VALIDATION_FAILED, "빈 selectionId는 사용할 수 없습니다")
        }
        val curatedIds = orderedIds.mapNotNull { value ->
            runCatching { UUID.fromString(value) }.getOrNull()
        }
        val curatedById = curatedTrackRepository.findAllById(curatedIds).associateBy { it.id }
        if (curatedById.size != curatedIds.distinct().size) {
            throw ApiException(ErrorCode.NOT_FOUND, "선택한 카탈로그 트랙을 찾을 수 없습니다")
        }

        return orderedIds.map { selectionId ->
            val curatedId = runCatching { UUID.fromString(selectionId) }.getOrNull()
            if (curatedId != null) {
                curatedById.getValue(curatedId).toComposableTrack()
            } else {
                youTubeSelectionTokenCodec.verify(selectionId).let { selection ->
                    ComposableTrack(
                        title = selection.title,
                        artist = selection.channelTitle?.takeIf(String::isNotBlank) ?: "YouTube",
                        youtubeVideoId = selection.videoId,
                        durationMs = null,
                        coverUrl = selection.thumbnailUrl,
                    )
                }
            }
        }
    }

    private fun com.plshare.backend.domain.catalog.model.CuratedTrack.toComposableTrack() =
        ComposableTrack(
            title = title,
            artist = artist,
            youtubeVideoId = youtubeVideoId,
            durationMs = durationMs,
            coverUrl = coverUrl,
        )

    private fun validateIdempotencyKey(raw: String): String {
        val key = raw.trim()
        if (key.isEmpty() || key.length > 160) {
            throw ApiException(ErrorCode.VALIDATION_FAILED, "X-Idempotency-Key는 1~160자여야 합니다")
        }
        return key
    }

    companion object {
        private const val MAX_TRACKS = 30
    }

    private data class ComposableTrack(
        val title: String,
        val artist: String,
        val youtubeVideoId: String,
        val durationMs: Int?,
        val coverUrl: String?,
    )
}
