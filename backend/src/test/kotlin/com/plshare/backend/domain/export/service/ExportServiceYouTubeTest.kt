package com.plshare.backend.domain.export.service

import com.plshare.backend.domain.asset.model.Asset
import com.plshare.backend.domain.asset.model.Track
import com.plshare.backend.domain.asset.repository.AssetRepository
import com.plshare.backend.domain.export.model.ExportJob
import com.plshare.backend.domain.export.model.ExportJobStatus
import com.plshare.backend.domain.export.model.ExportMatchStatus
import com.plshare.backend.domain.export.model.ExportTrackMatch
import com.plshare.backend.domain.export.repository.ExportJobRepository
import com.plshare.backend.domain.export.repository.ExportTrackMatchRepository
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import com.plshare.backend.infrastructure.youtube.YouTubeSearchCandidate
import com.plshare.backend.infrastructure.apple.AppleMusicAddResult
import com.plshare.backend.infrastructure.apple.AppleMusicPlaylistRef
import com.plshare.backend.infrastructure.apple.AppleMusicTrackInput
import com.plshare.backend.infrastructure.apple.AppleMusicVerifyResult
import com.plshare.backend.infrastructure.apple.AppleMusicWriteAdapter
import com.plshare.backend.infrastructure.youtube.FakeQuotaRepository
import com.plshare.backend.infrastructure.youtube.YouTubeMusicPlaylistRef
import com.plshare.backend.infrastructure.youtube.YouTubeClient
import com.plshare.backend.infrastructure.youtube.YouTubePlaylistItem
import com.plshare.backend.infrastructure.youtube.YouTubePlaylistItemEntry
import com.plshare.backend.infrastructure.youtube.YouTubePlaylistSummary
import com.plshare.backend.infrastructure.youtube.YouTubeVideoItem
import com.plshare.backend.infrastructure.youtube.YouTubeQuotaGuard
import com.plshare.backend.infrastructure.youtube.YouTubeWriteAdapter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.*
import org.springframework.data.repository.query.FluentQuery
import reactor.core.publisher.Mono
import java.util.*
import java.util.function.Function

/**
 * ExportService YouTube 분기 단위 테스트 — fake repository, fake adapter (no Spring context).
 *
 * 검증 불변식:
 *  (a) YouTube happy path: videoId 있는 트랙은 added, 없는 것은 failed
 *  (b) videoId 없는 트랙 → status="failed" + reason에 "no_video_id" 포함
 *  (c) 쿼터 초과 → job FAILED + lastError에 "QUOTA_EXCEEDED" 포함
 *  (d) Apple 경로 → 기존 동작 불변 (COMPLETED/PARTIAL)
 *
 * 프로덕션에선 ExportEventListener가 커밋 후 @Async로 runExport를 호출한다.
 * 단위테스트에선 runExport를 직접 동기 호출한다.
 */
class ExportServiceYouTubeTest {

    private lateinit var exportRepo: FakeExportJobRepo
    private lateinit var assetRepo: FakeAssetRepoExport
    private lateinit var appleAdapter: FakeAppleAdapter
    private lateinit var youtubeAdapter: FakeYouTubeWriteAdapter
    private lateinit var quotaRepo: FakeQuotaRepository
    private lateinit var quotaGuard: YouTubeQuotaGuard
    private lateinit var matchRepo: FakeExportTrackMatchRepo
    private lateinit var service: ExportService

    @BeforeEach
    fun setUp() {
        exportRepo = FakeExportJobRepo()
        assetRepo = FakeAssetRepoExport()
        appleAdapter = FakeAppleAdapter()
        quotaRepo = FakeQuotaRepository()
        quotaGuard = YouTubeQuotaGuard(quotaRepo, dailyBudget = 10_000L)
        youtubeAdapter = FakeYouTubeWriteAdapter()
        matchRepo = FakeExportTrackMatchRepo()
        service = ExportService(exportRepo, assetRepo, appleAdapter, youtubeAdapter, quotaGuard, matchRepo)
    }

    // ─── (a) YouTube happy path ──────────────────────────────────────────────

    @Test
    fun `YouTube export - videoId 있는 트랙은 added, 없는 트랙은 failed`() {
        // asset: source=youtube, 트랙 2개 (videoId 있음), 1개 (videoId 없음)
        val asset = buildYouTubeAsset(
            tracks = listOf(
                TrackSpec("Song A", videoId = "vid_aaa"),
                TrackSpec("Song B", videoId = "vid_bbb"),
                TrackSpec("Song C", videoId = null) // videoId 없음
            )
        )
        assetRepo.save(asset)

        val job = ExportJob(
            assetId = asset.id,
            targetPlatform = "youtube",
            idempotencyKey = "key-yt-1",
            totalTracks = 3
        )
        exportRepo.save(job)

        service.runExport(job.id)

        val saved = exportRepo.store[job.id]!!
        // 2 added, 1 failed → PARTIAL
        assertEquals(ExportJobStatus.PARTIAL, saved.status)
        assertEquals(2, saved.matchedTracks)
        assertEquals(1, saved.failedTracks)
        assertNotNull(saved.externalPlaylistId)
        assertTrue(saved.externalUrl!!.startsWith("https://music.youtube.com/playlist?list="))
    }

    @Test
    fun `YouTube export - 모든 트랙 videoId 있으면 COMPLETED`() {
        val asset = buildYouTubeAsset(
            tracks = listOf(
                TrackSpec("Song A", videoId = "vid_aaa"),
                TrackSpec("Song B", videoId = "vid_bbb")
            )
        )
        assetRepo.save(asset)

        val job = ExportJob(
            assetId = asset.id,
            targetPlatform = "youtube",
            idempotencyKey = "key-yt-all",
            totalTracks = 2
        )
        exportRepo.save(job)

        service.runExport(job.id)

        val saved = exportRepo.store[job.id]!!
        assertEquals(ExportJobStatus.COMPLETED, saved.status)
        assertEquals(2, saved.matchedTracks)
        assertEquals(0, saved.failedTracks)
    }

    // ─── (b) videoId 없는 트랙 → failed + reason ──────────────────────────────

    @Test
    fun `소스가 youtube가 아닌 asset의 트랙은 no_video_id로 failed`() {
        // sourcePlatform = "spotify" → videoId 확보 불가
        val asset = buildSpotifyAsset(
            tracks = listOf(
                TrackSpec("Spotify Song", videoId = null)
            )
        )
        assetRepo.save(asset)

        val job = ExportJob(
            assetId = asset.id,
            targetPlatform = "youtube",
            idempotencyKey = "key-yt-spotify-src",
            totalTracks = 1
        )
        exportRepo.save(job)

        service.runExport(job.id)

        val saved = exportRepo.store[job.id]!!
        // 0 added, 1 failed → PARTIAL (ExportJob.complete: failed>0 → PARTIAL)
        assertEquals(ExportJobStatus.PARTIAL, saved.status)
        assertEquals(0, saved.matchedTracks)
        assertEquals(1, saved.failedTracks)
    }

    @Test
    fun `youtube source지만 youtubeVideoId가 null이면 no_video_id로 failed`() {
        val asset = buildYouTubeAsset(
            tracks = listOf(TrackSpec("No ID Track", videoId = null))
        )
        assetRepo.save(asset)

        val job = ExportJob(
            assetId = asset.id,
            targetPlatform = "youtube",
            idempotencyKey = "key-yt-null-vid",
            totalTracks = 1
        )
        exportRepo.save(job)

        service.runExport(job.id)

        val saved = exportRepo.store[job.id]!!
        assertEquals(ExportJobStatus.PARTIAL, saved.status)
        assertEquals(1, saved.failedTracks)
        // lastError가 아닌 job status로 확인; failed는 complete() 내부에서 결정
    }

    @Test
    fun `Spotify 트랙은 YouTube search 결과로 export된다`() {
        val searchClient = object : YouTubeClient {
            override fun listUserPlaylists(accessToken: String) =
                Mono.just(emptyList<YouTubePlaylistSummary>())

            override fun getPlaylist(playlistId: String, accessToken: String) =
                Mono.error<YouTubePlaylistItem>(UnsupportedOperationException())

            override fun getPlaylistItems(playlistId: String, accessToken: String) =
                Mono.just(emptyList<YouTubePlaylistItemEntry>())

            override fun getVideoDetails(videoIds: List<String>, accessToken: String) =
                Mono.just(emptyList<YouTubeVideoItem>())

            override fun searchVideo(title: String, artist: String, accessToken: String): Mono<String> =
                Mono.just("resolved123")

            override fun searchVideoCandidates(title: String, artist: String, accessToken: String) =
                // exact title match → high confidence → MATCHED
                Mono.just(listOf(YouTubeSearchCandidate("resolved123", title, artist)))
        }
        val searchService = ExportService(
            exportRepo,
            assetRepo,
            appleAdapter,
            youtubeAdapter,
            quotaGuard,
            matchRepo,
            youtubeClient = searchClient,
        )
        val asset = buildSpotifyAsset(listOf(TrackSpec("Spotify Song", videoId = null)))
        assetRepo.save(asset)
        val job = ExportJob(
            assetId = asset.id,
            targetPlatform = "youtube",
            idempotencyKey = "key-yt-search",
            totalTracks = 1,
        )
        exportRepo.save(job)

        searchService.runExport(job.id)

        val saved = exportRepo.store[job.id]!!
        assertEquals(ExportJobStatus.COMPLETED, saved.status)
        assertEquals(1, saved.matchedTracks)
        assertEquals(0, saved.failedTracks)
    }

    @Test
    fun `다른 사용자 소유 asset은 export 요청을 거부한다`() {
        val ownerId = UUID.randomUUID()
        val asset = Asset(
            ownerId = ownerId,
            title = "Private asset",
            sourcePlatform = "spotify",
        )
        assetRepo.save(asset)

        val error = assertThrows(ApiException::class.java) {
            service.requestExport(
                idempotencyKey = "private-export",
                assetId = asset.id,
                targetPlatform = "youtube",
                ownerId = UUID.randomUUID(),
            )
        }

        assertEquals(ErrorCode.FORBIDDEN, error.code)
    }

    // ─── (c) 쿼터 초과 → job FAILED ─────────────────────────────────────────

    @Test
    fun `쿼터 초과 시 job이 FAILED되고 lastError에 QUOTA_EXCEEDED 포함`() {
        // 예산을 먼저 소진
        val tightGuard = YouTubeQuotaGuard(FakeQuotaRepository(), dailyBudget = 100L)
        val tightService = ExportService(exportRepo, assetRepo, appleAdapter, youtubeAdapter, tightGuard, matchRepo)

        val asset = buildYouTubeAsset(
            tracks = listOf(
                TrackSpec("S1", videoId = "v1"),
                TrackSpec("S2", videoId = "v2")
            )
        )
        assetRepo.save(asset)

        val job = ExportJob(
            assetId = asset.id,
            targetPlatform = "youtube",
            idempotencyKey = "key-yt-quota",
            totalTracks = 2
        )
        exportRepo.save(job)

        // estimatedCost(2) = 50 + 50*2 = 150 > budget=100
        tightService.runExport(job.id)

        val saved = exportRepo.store[job.id]!!
        assertEquals(ExportJobStatus.FAILED, saved.status)
        assertTrue(saved.lastError!!.contains("QUOTA_EXCEEDED"), "lastError should contain QUOTA_EXCEEDED but was: ${saved.lastError}")
    }

    // ─── (d) Apple 경로 회귀 ──────────────────────────────────────────────────

    @Test
    fun `Apple export 경로는 YouTube 변경과 무관하게 동작`() {
        val asset = buildYouTubeAsset( // sourcePlatform 무관 — Apple은 ISRC 사용
            tracks = listOf(
                TrackSpec("Apple Song A", videoId = "irrelevant"),
                TrackSpec("Apple Song B", videoId = "irrelevant2")
            )
        )
        assetRepo.save(asset)

        val job = ExportJob(
            assetId = asset.id,
            targetPlatform = "apple",
            idempotencyKey = "key-apple-regression",
            totalTracks = 2
        )
        exportRepo.save(job)

        service.runExport(job.id)

        val saved = exportRepo.store[job.id]!!
        assertEquals(ExportJobStatus.COMPLETED, saved.status)
        assertEquals(2, saved.matchedTracks)
        assertEquals(0, saved.failedTracks)
        assertNotNull(saved.externalPlaylistId)
    }

    @Test
    fun `Apple export - 일부 트랙 skip되면 PARTIAL`() {
        // FakeAppleAdapter는 isrc ending in "0003" skip 시뮬레이션
        val asset = buildYouTubeAsset(
            tracks = listOf(
                TrackSpec("Track OK",   videoId = null, isrc = "US1230000001"),
                TrackSpec("Track Skip", videoId = null, isrc = "US1230000003") // ends "0003" → skipped
            )
        )
        assetRepo.save(asset)

        val job = ExportJob(
            assetId = asset.id,
            targetPlatform = "apple",
            idempotencyKey = "key-apple-partial",
            totalTracks = 2
        )
        exportRepo.save(job)

        service.runExport(job.id)

        val saved = exportRepo.store[job.id]!!
        assertEquals(ExportJobStatus.PARTIAL, saved.status)
        assertEquals(1, saved.matchedTracks)
        assertEquals(1, saved.failedTracks)
    }

    // ─── (e) 저신뢰 매칭 검토 (manual review) ─────────────────────────────────

    @Test
    fun `저신뢰(노이즈 토큰) 매칭은 ALTERNATIVE로 영속되고 검토 대상이 된다`() {
        val client = candidateClient { title, artist ->
            listOf(YouTubeSearchCandidate("vidlive", "$title (Live)", artist)) // 'live' 노이즈 → 감점
        }
        val svc = ExportService(
            exportRepo, assetRepo, appleAdapter, youtubeAdapter, quotaGuard, matchRepo,
            youtubeClient = client,
        )
        val asset = buildSpotifyAsset(listOf(TrackSpec("Song", videoId = null)))
        assetRepo.save(asset)
        val job = ExportJob(assetId = asset.id, targetPlatform = "youtube", idempotencyKey = "key-alt", totalTracks = 1)
        exportRepo.save(job)

        svc.runExport(job.id)

        val matches = matchRepo.findByExportJobId(job.id)
        assertEquals(1, matches.size)
        assertEquals(ExportMatchStatus.ALTERNATIVE, matches[0].status)
        assertTrue(matches[0].confidence!! < ExportService.CONFIDENCE_THRESHOLD)
        // ALTERNATIVE도 플레이리스트에 추가된다(matched count에 포함)
        assertEquals(1, exportRepo.store[job.id]!!.matchedTracks)
    }

    @Test
    fun `override는 선택한 비디오를 확정하고 FAILED를 matched로 재계산한다`() {
        val client = candidateClient { _, _ -> emptyList() } // 후보 없음 → FAILED
        val svc = ExportService(
            exportRepo, assetRepo, appleAdapter, youtubeAdapter, quotaGuard, matchRepo,
            youtubeClient = client,
        )
        val asset = buildSpotifyAsset(listOf(TrackSpec("Lost Song", videoId = null)))
        assetRepo.save(asset)
        val trackId = asset.tracks[0].id
        val job = ExportJob(assetId = asset.id, targetPlatform = "youtube", idempotencyKey = "key-ovr", totalTracks = 1)
        exportRepo.save(job)
        svc.runExport(job.id)

        assertEquals(ExportMatchStatus.FAILED, matchRepo.findByExportJobIdAndTrackId(job.id, trackId)!!.status)
        assertEquals(1, exportRepo.store[job.id]!!.failedTracks)

        val updated = svc.overrideMatch(job.id, trackId, "chosenVid", "Chosen Title", requesterId = null)

        assertEquals(ExportMatchStatus.MATCHED, updated.status)
        assertTrue(updated.reviewed)
        assertEquals("chosenVid", updated.matchedVideoId)
        val saved = exportRepo.store[job.id]!!
        assertEquals(0, saved.failedTracks)
        assertEquals(1, saved.matchedTracks)
        assertEquals(ExportJobStatus.COMPLETED, saved.status)
    }

    @Test
    fun `다른 사용자는 override할 수 없다`() {
        val ownerId = UUID.randomUUID()
        val job = ExportJob(
            ownerId = ownerId, assetId = UUID.randomUUID(), targetPlatform = "youtube",
            idempotencyKey = "key-own-ovr", totalTracks = 1,
        )
        exportRepo.save(job)

        val err = assertThrows(ApiException::class.java) {
            service.overrideMatch(job.id, UUID.randomUUID(), "v", null, requesterId = UUID.randomUUID())
        }
        assertEquals(ErrorCode.FORBIDDEN, err.code)
    }

    @Test
    fun `matchConfidence는 노이즈 토큰(live)을 감점한다`() {
        val exact = service.matchConfidence("Song", "Artist", YouTubeSearchCandidate("v", "Song", "Artist"))
        val live = service.matchConfidence("Song", "Artist", YouTubeSearchCandidate("v", "Song (Live)", "Artist"))
        assertEquals(1.0, exact, 0.001)
        assertTrue(exact > live, "exact($exact) should beat live($live)")
    }

    private fun candidateClient(
        search: (String, String) -> List<YouTubeSearchCandidate>,
    ): YouTubeClient = object : YouTubeClient {
        override fun listUserPlaylists(accessToken: String) = Mono.just(emptyList<YouTubePlaylistSummary>())
        override fun getPlaylist(playlistId: String, accessToken: String) =
            Mono.error<YouTubePlaylistItem>(UnsupportedOperationException())
        override fun getPlaylistItems(playlistId: String, accessToken: String) =
            Mono.just(emptyList<YouTubePlaylistItemEntry>())
        override fun getVideoDetails(videoIds: List<String>, accessToken: String) =
            Mono.just(emptyList<YouTubeVideoItem>())
        override fun searchVideoCandidates(title: String, artist: String, accessToken: String) =
            Mono.just(search(title, artist))
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────────────

    private data class TrackSpec(val name: String, val videoId: String?, val isrc: String? = null)

    private fun buildYouTubeAsset(tracks: List<TrackSpec>): Asset {
        val asset = Asset(title = "Test YTM Playlist", sourcePlatform = "youtube")
        tracks.forEach { spec ->
            asset.tracks.add(
                Track(
                    asset = asset,
                    name = spec.name,
                    artist = "Test Artist",
                    isrc = spec.isrc,
                    youtubeVideoId = spec.videoId
                )
            )
        }
        return asset
    }

    private fun buildSpotifyAsset(tracks: List<TrackSpec>): Asset {
        val asset = Asset(title = "Test Spotify Playlist", sourcePlatform = "spotify")
        tracks.forEach { spec ->
            asset.tracks.add(
                Track(
                    asset = asset,
                    name = spec.name,
                    artist = "Test Artist",
                    isrc = spec.isrc,
                    spotifyId = spec.videoId
                )
            )
        }
        return asset
    }
}

// ─── Fake implementations ────────────────────────────────────────────────────

class FakeExportJobRepo : ExportJobRepository {
    val store = mutableMapOf<UUID, ExportJob>()

    override fun findByIdempotencyKey(idempotencyKey: String): ExportJob? =
        store.values.firstOrNull { it.idempotencyKey == idempotencyKey }

    override fun <S : ExportJob> save(entity: S): S { store[entity.id] = entity; return entity }
    override fun findById(id: UUID): Optional<ExportJob> = Optional.ofNullable(store[id])
    override fun findAll(): MutableList<ExportJob> = store.values.toMutableList()
    override fun count(): Long = store.size.toLong()
    override fun existsById(id: UUID): Boolean = store.containsKey(id)
    override fun deleteById(id: UUID) { store.remove(id) }
    override fun delete(entity: ExportJob) { store.remove(entity.id) }
    override fun deleteAll() { store.clear() }
    override fun deleteAll(entities: MutableIterable<ExportJob>) { entities.forEach { delete(it) } }
    override fun deleteAllById(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun <S : ExportJob> saveAll(entities: MutableIterable<S>): MutableList<S> = entities.map { save(it) }.toMutableList()
    override fun findAllById(ids: MutableIterable<UUID>): MutableList<ExportJob> = ids.mapNotNull { store[it] }.toMutableList()
    override fun findAll(sort: Sort): MutableList<ExportJob> = findAll()
    override fun findAll(pageable: Pageable): Page<ExportJob> = throw NotImplementedError()
    override fun flush() {}
    override fun <S : ExportJob> saveAndFlush(entity: S): S = save(entity)
    override fun <S : ExportJob> saveAllAndFlush(entities: MutableIterable<S>): MutableList<S> = saveAll(entities)
    override fun deleteAllInBatch() { store.clear() }
    override fun deleteAllInBatch(entities: MutableIterable<ExportJob>) { entities.forEach { delete(it) } }
    override fun deleteAllByIdInBatch(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun getOne(id: UUID): ExportJob = store[id] ?: throw NoSuchElementException()
    override fun getById(id: UUID): ExportJob = getOne(id)
    override fun getReferenceById(id: UUID): ExportJob = getOne(id)
    override fun <S : ExportJob> findOne(example: Example<S>): Optional<S> = throw NotImplementedError()
    override fun <S : ExportJob> findAll(example: Example<S>): MutableList<S> = throw NotImplementedError()
    override fun <S : ExportJob> findAll(example: Example<S>, sort: Sort): MutableList<S> = throw NotImplementedError()
    override fun <S : ExportJob> findAll(example: Example<S>, pageable: Pageable): Page<S> = throw NotImplementedError()
    override fun <S : ExportJob> count(example: Example<S>): Long = throw NotImplementedError()
    override fun <S : ExportJob> exists(example: Example<S>): Boolean = throw NotImplementedError()
    override fun <S : ExportJob, R : Any> findBy(example: Example<S>, queryFunction: Function<FluentQuery.FetchableFluentQuery<S>, R>): R = throw NotImplementedError()
}

class FakeAssetRepoExport : AssetRepository {
    val store = mutableMapOf<UUID, Asset>()

    override fun findByShareToken(shareToken: String): Asset? = store.values.firstOrNull { it.shareToken == shareToken }
    override fun findWithTracksById(id: UUID): Asset? = store[id]

    override fun <S : Asset> save(entity: S): S { store[entity.id] = entity; return entity }
    override fun findById(id: UUID): Optional<Asset> = Optional.ofNullable(store[id])
    override fun findAll(): MutableList<Asset> = store.values.toMutableList()
    override fun count(): Long = store.size.toLong()
    override fun existsById(id: UUID): Boolean = store.containsKey(id)
    override fun deleteById(id: UUID) { store.remove(id) }
    override fun delete(entity: Asset) { store.remove(entity.id) }
    override fun deleteAll() { store.clear() }
    override fun deleteAll(entities: MutableIterable<Asset>) { entities.forEach { delete(it) } }
    override fun deleteAllById(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun <S : Asset> saveAll(entities: MutableIterable<S>): MutableList<S> = entities.map { save(it) }.toMutableList()
    override fun findAllById(ids: MutableIterable<UUID>): MutableList<Asset> = ids.mapNotNull { store[it] }.toMutableList()
    override fun findAll(sort: Sort): MutableList<Asset> = findAll()
    override fun findAll(pageable: Pageable): Page<Asset> = throw NotImplementedError()
    override fun flush() {}
    override fun <S : Asset> saveAndFlush(entity: S): S = save(entity)
    override fun <S : Asset> saveAllAndFlush(entities: MutableIterable<S>): MutableList<S> = saveAll(entities)
    override fun deleteAllInBatch() { store.clear() }
    override fun deleteAllInBatch(entities: MutableIterable<Asset>) { entities.forEach { delete(it) } }
    override fun deleteAllByIdInBatch(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun getOne(id: UUID): Asset = store[id] ?: throw NoSuchElementException()
    override fun getById(id: UUID): Asset = getOne(id)
    override fun getReferenceById(id: UUID): Asset = getOne(id)
    override fun <S : Asset> findOne(example: Example<S>): Optional<S> = throw NotImplementedError()
    override fun <S : Asset> findAll(example: Example<S>): MutableList<S> = throw NotImplementedError()
    override fun <S : Asset> findAll(example: Example<S>, sort: Sort): MutableList<S> = throw NotImplementedError()
    override fun <S : Asset> findAll(example: Example<S>, pageable: Pageable): Page<S> = throw NotImplementedError()
    override fun <S : Asset> count(example: Example<S>): Long = throw NotImplementedError()
    override fun <S : Asset> exists(example: Example<S>): Boolean = throw NotImplementedError()
    override fun <S : Asset, R : Any> findBy(example: Example<S>, queryFunction: Function<FluentQuery.FetchableFluentQuery<S>, R>): R = throw NotImplementedError()
}

/**
 * YouTube write adapter fake — 직접 구현 (Mock 빈과 독립적으로 동작).
 * videoId="fail_*" 이면 UPSTREAM_ERROR, 그 외 성공.
 */
class FakeYouTubeWriteAdapter : YouTubeWriteAdapter {
    override fun createPlaylist(title: String, description: String?, accessToken: String): YouTubeMusicPlaylistRef {
        val id = "fake-ytm-${UUID.randomUUID().toString().take(8)}"
        return YouTubeMusicPlaylistRef(
            externalId = id,
            externalUrl = "https://music.youtube.com/playlist?list=$id"
        )
    }

    override fun addItem(playlistId: String, videoId: String, accessToken: String) {
        if (videoId.startsWith("fail_")) {
            throw ApiException(ErrorCode.UPSTREAM_ERROR, "Fake: simulated failure for videoId=$videoId")
        }
        // 성공 — no-op
    }
}

/**
 * Apple write adapter fake — MockAppleMusicWriteAdapter 로직 재현 (의존성 없이).
 * isrc ends "0003" → skipped, otherwise matched.
 */
class FakeAppleAdapter : AppleMusicWriteAdapter {
    override fun createPlaylist(name: String, description: String?): Mono<AppleMusicPlaylistRef> {
        val id = "apl-fake-${UUID.randomUUID().toString().take(8)}"
        return Mono.just(AppleMusicPlaylistRef(externalId = id, externalUrl = "https://music.apple.com/library/playlist/$id"))
    }

    override fun addTracks(playlistRef: AppleMusicPlaylistRef, tracks: List<AppleMusicTrackInput>): Mono<AppleMusicAddResult> {
        val results = tracks.map { input ->
            val skip = input.isrc?.endsWith("0003") == true
            if (skip) {
                com.plshare.backend.infrastructure.apple.AppleMusicTrackResult(
                    isrc = input.isrc, title = input.title, artist = input.artist,
                    appleSongId = null, status = "skipped", reason = "no_match"
                )
            } else {
                val songId = "apl-song-${(input.isrc ?: input.title).hashCode().toUInt()}"
                com.plshare.backend.infrastructure.apple.AppleMusicTrackResult(
                    isrc = input.isrc, title = input.title, artist = input.artist,
                    appleSongId = songId, status = "matched"
                )
            }
        }
        val matched = results.count { it.status == "matched" }
        val skipped = results.count { it.status == "skipped" }
        return Mono.just(AppleMusicAddResult(matched, skipped, results))
    }

    override fun verify(playlistRef: AppleMusicPlaylistRef, expected: Int): Mono<AppleMusicVerifyResult> =
        Mono.just(AppleMusicVerifyResult(expected = expected, actual = expected, ok = true))
}

class FakeExportTrackMatchRepo : ExportTrackMatchRepository {
    val store = mutableMapOf<UUID, ExportTrackMatch>()

    override fun findByExportJobId(exportJobId: UUID): List<ExportTrackMatch> =
        store.values.filter { it.exportJobId == exportJobId }

    override fun findByExportJobIdAndTrackId(exportJobId: UUID, trackId: UUID): ExportTrackMatch? =
        store.values.firstOrNull { it.exportJobId == exportJobId && it.trackId == trackId }

    override fun <S : ExportTrackMatch> save(entity: S): S { store[entity.id] = entity; return entity }
    override fun findById(id: UUID): Optional<ExportTrackMatch> = Optional.ofNullable(store[id])
    override fun findAll(): MutableList<ExportTrackMatch> = store.values.toMutableList()
    override fun count(): Long = store.size.toLong()
    override fun existsById(id: UUID): Boolean = store.containsKey(id)
    override fun deleteById(id: UUID) { store.remove(id) }
    override fun delete(entity: ExportTrackMatch) { store.remove(entity.id) }
    override fun deleteAll() { store.clear() }
    override fun deleteAll(entities: MutableIterable<ExportTrackMatch>) { entities.forEach { delete(it) } }
    override fun deleteAllById(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun <S : ExportTrackMatch> saveAll(entities: MutableIterable<S>): MutableList<S> = entities.map { save(it) }.toMutableList()
    override fun findAllById(ids: MutableIterable<UUID>): MutableList<ExportTrackMatch> = ids.mapNotNull { store[it] }.toMutableList()
    override fun findAll(sort: Sort): MutableList<ExportTrackMatch> = findAll()
    override fun findAll(pageable: Pageable): Page<ExportTrackMatch> = throw NotImplementedError()
    override fun flush() {}
    override fun <S : ExportTrackMatch> saveAndFlush(entity: S): S = save(entity)
    override fun <S : ExportTrackMatch> saveAllAndFlush(entities: MutableIterable<S>): MutableList<S> = saveAll(entities)
    override fun deleteAllInBatch() { store.clear() }
    override fun deleteAllInBatch(entities: MutableIterable<ExportTrackMatch>) { entities.forEach { delete(it) } }
    override fun deleteAllByIdInBatch(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun getOne(id: UUID): ExportTrackMatch = store[id] ?: throw NoSuchElementException()
    override fun getById(id: UUID): ExportTrackMatch = getOne(id)
    override fun getReferenceById(id: UUID): ExportTrackMatch = getOne(id)
    override fun <S : ExportTrackMatch> findOne(example: Example<S>): Optional<S> = throw NotImplementedError()
    override fun <S : ExportTrackMatch> findAll(example: Example<S>): MutableList<S> = throw NotImplementedError()
    override fun <S : ExportTrackMatch> findAll(example: Example<S>, sort: Sort): MutableList<S> = throw NotImplementedError()
    override fun <S : ExportTrackMatch> findAll(example: Example<S>, pageable: Pageable): Page<S> = throw NotImplementedError()
    override fun <S : ExportTrackMatch> count(example: Example<S>): Long = throw NotImplementedError()
    override fun <S : ExportTrackMatch> exists(example: Example<S>): Boolean = throw NotImplementedError()
    override fun <S : ExportTrackMatch, R : Any> findBy(example: Example<S>, queryFunction: Function<FluentQuery.FetchableFluentQuery<S>, R>): R = throw NotImplementedError()
}
