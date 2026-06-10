package com.plshare.backend.domain.importing.service

import com.plshare.backend.domain.asset.model.Asset
import com.plshare.backend.domain.asset.model.Track
import com.plshare.backend.domain.asset.repository.AssetRepository
import com.plshare.backend.domain.importing.repository.ImportJobRepository
import com.plshare.backend.domain.track.service.MatchingEngine
import com.plshare.backend.infrastructure.spotify.SpotifyClient
import com.plshare.backend.infrastructure.spotify.SpotifyPlaylistResponse
import com.plshare.backend.infrastructure.youtube.YouTubeClient
import com.plshare.backend.infrastructure.youtube.YouTubePlaylistSummary
import com.plshare.backend.infrastructure.youtube.YouTubeTrackItem
import com.plshare.backend.infrastructure.youtube.normalizeVideoId
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NormalizationEngine(
    private val importJobRepository: ImportJobRepository,
    private val assetRepository: AssetRepository,
    private val spotifyClient: SpotifyClient,
    private val youTubeClient: YouTubeClient,
    private val matchingEngine: MatchingEngine
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * 비동기 정규화 실행.
     * 외부 API 호출과 DB 트랜잭션을 분리하여 장애 전파를 차단한다.
     *
     * 호출 측이 @Transactional 안에서 save 후 곧바로 이 메서드를 호출하면
     * outer transaction commit 전에 async thread가 findById를 시도해 실패할 수 있다.
     * 짧은 retry로 commit propagation을 흡수한다.
     */
    @Async
    fun runNormalization(jobId: java.util.UUID) {
        var attempt = 0
        var found: com.plshare.backend.domain.importing.model.ImportJob? = null
        while (attempt < 10 && found == null) {
            found = importJobRepository.findById(jobId).orElse(null)
            if (found == null) {
                Thread.sleep(100)
                attempt++
            }
        }
        val job = found ?: run {
            log.error("Normalization aborted: job $jobId not visible after $attempt retries")
            return
        }

        try {
            job.start()
            importJobRepository.save(job)

            when (job.sourcePlatform.lowercase()) {
                "youtube" -> {
                    // YouTube 경로: YouTubeClient에서 플레이리스트 읽기
                    val playlistId = job.sourcePlaylistId
                        ?: throw IllegalStateException("sourcePlaylistId is null for YouTube job $jobId")
                    val accessToken = job.youtubeAccessToken()
                        ?: throw IllegalStateException("YouTube accessToken not available for job $jobId")

                    val summary = youTubeClient.listUserPlaylists(accessToken).block()
                        ?.firstOrNull { it.id == playlistId }
                        ?: run {
                            // listUserPlaylists가 없으면 개별 플레이리스트 + items 조회
                            val playlistItem = youTubeClient.getPlaylist(playlistId, accessToken).block()
                                ?: throw IllegalStateException("YouTube playlist not found: $playlistId")
                            val items = youTubeClient.getPlaylistItems(playlistId, accessToken).block()
                                ?: emptyList()
                            val videoIds = items.map { it.snippet.resourceId.videoId }
                            val videoDetails = if (videoIds.isNotEmpty())
                                youTubeClient.getVideoDetails(videoIds, accessToken).block() ?: emptyList()
                            else emptyList()
                            val videoMap = videoDetails.associateBy { it.id }
                            YouTubePlaylistSummary(
                                id = playlistItem.id,
                                title = playlistItem.snippet.title,
                                coverUrl = playlistItem.snippet.coverUrl,
                                items = items.map { entry ->
                                    val vid = entry.snippet.resourceId.videoId
                                    val detail = videoMap[vid]
                                    YouTubeTrackItem(
                                        videoId = normalizeVideoId(vid),
                                        title = detail?.snippet?.title ?: entry.snippet.title,
                                        artistHint = detail?.snippet?.channelTitle ?: entry.snippet.channelTitle,
                                        durationMs = com.plshare.backend.infrastructure.youtube.parseDurationMs(
                                            detail?.contentDetails?.duration
                                        )
                                    )
                                }
                            )
                        }

                    saveNormalizedAssetFromYouTube(jobId, summary)
                }
                else -> {
                    // Spotify 경로 (기본, 기존 동작 불변)
                    val accessToken = spotifyClient.getAccessToken().block()
                        ?: throw IllegalStateException("Failed to get Spotify access token")

                    val playlistId = job.spotifyPlaylistId
                        ?: job.sourcePlaylistId
                        ?: throw IllegalStateException("No playlist ID for Spotify job $jobId")

                    val playlist = spotifyClient.getPlaylist(playlistId, accessToken).block()
                        ?: throw IllegalStateException("Failed to get Spotify playlist")

                    saveNormalizedAsset(jobId, playlist)
                }
            }

        } catch (e: Exception) {
            log.error("Normalization failed for job $jobId", e)
            job.fail("IMPORT_FAILED", e.message ?: "Unknown error")
            importJobRepository.save(job)
        }
    }

    // ─── Spotify 경로 (기존 동작 불변) ────────────────────────────────────────

    @Transactional
    fun saveNormalizedAsset(jobId: java.util.UUID, playlist: SpotifyPlaylistResponse) {
        val job = importJobRepository.findById(jobId).orElseThrow()

        // Asset 생성
        val asset = Asset(
            title = playlist.name,
            coverUrl = playlist.images.firstOrNull()?.url,
            sourcePlatform = "spotify"
        )

        // Track 정규화 (ISRC 기준) + MatchingEngine을 통해 canonical 연결
        val tracks = playlist.tracks.items.map { item ->
            val matchInput = MatchingEngine.MatchInput(
                isrc = item.track.isrc?.takeIf { it.isNotBlank() },
                title = item.track.name,
                artists = item.track.artists.map { it.name },
                durationMs = item.track.durationMs?.toLong() ?: 0L,
                sourcePlatform = "spotify",
                sourceId = item.track.id
            )
            val matchResult = matchingEngine.matchOrCreate(matchInput)

            Track(
                asset = asset,
                name = item.track.name,
                artist = item.track.artists.joinToString { it.name },
                durationMs = item.track.durationMs,
                isrc = item.track.isrc,
                spotifyId = item.track.id,
                canonicalTrackId = matchResult.canonical.canonicalId,
                matchConfidence = matchResult.confidence
            )
        }
        asset.tracks.addAll(tracks)

        val saved = assetRepository.save(asset)

        job.assetId = saved.id
        job.totalTracks = tracks.size
        job.complete()
        job.updateProgress(tracks.size)
        importJobRepository.save(job)
    }

    // ─── YouTube 경로 ─────────────────────────────────────────────────────────

    /**
     * YouTube 플레이리스트에서 Track + CanonicalTrack 생성/연결.
     *
     * YouTube Data API v3는 ISRC를 제공하지 않으므로 모든 트랙은 fuzzy fallback 경로를 사용한다.
     * fuzzy threshold(0.7) 미달 시 새 canonical을 생성하며, 낮은 confidence 그대로 저장한다.
     * videoId를 MatchInput.sourceId로 사용해 CanonicalTrack.sources에 "youtube"를 기록한다.
     *
     * 커버/라이브 제목 노이즈(예: "[Cover ver.]", "(Live at ...)"):
     *   normalizeTitle이 일부를 제거하지만 비표준 표기는 낮은 fuzzy score로 처리되어
     *   독립적인 canonical이 생성된다 — Low Confidence 신호로 저장.
     */
    @Transactional
    fun saveNormalizedAssetFromYouTube(jobId: java.util.UUID, summary: YouTubePlaylistSummary) {
        val job = importJobRepository.findById(jobId).orElseThrow()

        val asset = Asset(
            title = summary.title,
            coverUrl = summary.coverUrl,
            sourcePlatform = "youtube"
        )

        val tracks = summary.items.map { ytTrack ->
            val normalizedVideoId = normalizeVideoId(ytTrack.videoId)

            // artistHint는 channelTitle 기반으로 정확도가 낮다.
            // title에서 " - " 패턴으로 artist를 추출하는 휴리스틱을 시도한다.
            val (titleHint, artistHint) = parseYouTubeTitle(ytTrack.title, ytTrack.artistHint)

            val matchInput = MatchingEngine.MatchInput(
                isrc = null,  // YouTube API는 ISRC 미제공
                title = titleHint,
                artists = listOf(artistHint ?: "Unknown"),
                durationMs = ytTrack.durationMs,
                sourcePlatform = "youtube",
                sourceId = normalizedVideoId
            )
            val matchResult = matchingEngine.matchOrCreate(matchInput)

            Track(
                asset = asset,
                name = titleHint,
                artist = artistHint ?: "Unknown",
                durationMs = if (ytTrack.durationMs > 0) ytTrack.durationMs.toInt() else null,
                isrc = null,
                spotifyId = null,  // YouTube 트랙은 spotifyId 없음
                canonicalTrackId = matchResult.canonical.canonicalId,
                matchConfidence = matchResult.confidence
            )
        }
        asset.tracks.addAll(tracks)

        val saved = assetRepository.save(asset)

        job.assetId = saved.id
        job.totalTracks = tracks.size
        job.complete()
        job.updateProgress(tracks.size)
        importJobRepository.save(job)
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    /**
     * YouTube 비디오 제목에서 아티스트와 트랙 제목을 분리하는 휴리스틱.
     *
     * YouTube Music 제목은 "Artist - Song Title" 형태가 많다.
     * " - " 분리에 실패하면 전체를 title로, artistHint를 그대로 사용한다.
     *
     * @return Pair(title, artistOrNull)
     */
    private fun parseYouTubeTitle(rawTitle: String, channelHint: String?): Pair<String, String?> {
        val dashIdx = rawTitle.indexOf(" - ")
        return if (dashIdx > 0) {
            val artist = rawTitle.substring(0, dashIdx).trim()
            val title = rawTitle.substring(dashIdx + 3).trim()
            Pair(title, artist)
        } else {
            Pair(rawTitle, channelHint)
        }
    }

    /**
     * YouTube import job의 accessToken 조회.
     *
     * 현재는 demo(mock) 환경에서만 사용되며 mock은 토큰 무검증.
     * production에서는 GoogleOAuthSession 등에서 토큰을 주입해야 한다.
     * TODO: production YouTube token 조달 경로는 be-ytm-auth 태스크에서 구현.
     */
    private fun com.plshare.backend.domain.importing.model.ImportJob.youtubeAccessToken(): String? =
        "demo-youtube-token"  // demo에서는 MockYouTubeClient가 토큰 무검증
}
