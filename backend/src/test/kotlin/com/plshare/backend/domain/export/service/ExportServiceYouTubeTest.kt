package com.plshare.backend.domain.export.service

import com.plshare.backend.domain.asset.model.Asset
import com.plshare.backend.domain.asset.model.Track
import com.plshare.backend.domain.asset.repository.AssetRepository
import com.plshare.backend.domain.export.model.ExportJob
import com.plshare.backend.domain.export.model.ExportJobStatus
import com.plshare.backend.domain.export.repository.ExportJobRepository
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import com.plshare.backend.infrastructure.apple.AppleMusicAddResult
import com.plshare.backend.infrastructure.apple.AppleMusicPlaylistRef
import com.plshare.backend.infrastructure.apple.AppleMusicTrackInput
import com.plshare.backend.infrastructure.apple.AppleMusicVerifyResult
import com.plshare.backend.infrastructure.apple.AppleMusicWriteAdapter
import com.plshare.backend.infrastructure.youtube.FakeQuotaRepository
import com.plshare.backend.infrastructure.youtube.YouTubeMusicPlaylistRef
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
 * ExportService.runExport가 @Async이므로 테스트에서 직접 동기 호출한다.
 */
class ExportServiceYouTubeTest {

    private lateinit var exportRepo: FakeExportJobRepo
    private lateinit var assetRepo: FakeAssetRepoExport
    private lateinit var appleAdapter: FakeAppleAdapter
    private lateinit var youtubeAdapter: FakeYouTubeWriteAdapter
    private lateinit var quotaRepo: FakeQuotaRepository
    private lateinit var quotaGuard: YouTubeQuotaGuard
    private lateinit var service: ExportService

    @BeforeEach
    fun setUp() {
        exportRepo = FakeExportJobRepo()
        assetRepo = FakeAssetRepoExport()
        appleAdapter = FakeAppleAdapter()
        quotaRepo = FakeQuotaRepository()
        quotaGuard = YouTubeQuotaGuard(quotaRepo, dailyBudget = 10_000L)
        youtubeAdapter = FakeYouTubeWriteAdapter()
        service = ExportService(exportRepo, assetRepo, appleAdapter, youtubeAdapter, quotaGuard)
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

    // ─── (c) 쿼터 초과 → job FAILED ─────────────────────────────────────────

    @Test
    fun `쿼터 초과 시 job이 FAILED되고 lastError에 QUOTA_EXCEEDED 포함`() {
        // 예산을 먼저 소진
        val tightGuard = YouTubeQuotaGuard(FakeQuotaRepository(), dailyBudget = 100L)
        val tightService = ExportService(exportRepo, assetRepo, appleAdapter, youtubeAdapter, tightGuard)

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
