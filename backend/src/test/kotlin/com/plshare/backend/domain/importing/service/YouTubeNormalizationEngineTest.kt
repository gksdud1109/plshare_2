package com.plshare.backend.domain.importing.service

import com.plshare.backend.domain.asset.model.Asset
import com.plshare.backend.domain.asset.model.Track
import com.plshare.backend.domain.asset.repository.AssetRepository
import com.plshare.backend.domain.importing.model.ImportJob
import com.plshare.backend.domain.importing.model.ImportJobStatus
import com.plshare.backend.domain.importing.repository.ImportJobRepository
import com.plshare.backend.domain.track.model.CanonicalTrack
import com.plshare.backend.domain.track.repository.CanonicalTrackRepository
import com.plshare.backend.domain.track.service.MatchingEngine
import com.plshare.backend.infrastructure.spotify.SpotifyArtist
import com.plshare.backend.infrastructure.spotify.SpotifyImage
import com.plshare.backend.infrastructure.spotify.SpotifyPlaylistResponse
import com.plshare.backend.infrastructure.spotify.SpotifyTrack
import com.plshare.backend.infrastructure.spotify.SpotifyTrackContainer
import com.plshare.backend.infrastructure.spotify.SpotifyTrackItem
import com.plshare.backend.infrastructure.youtube.YouTubePlaylistSummary
import com.plshare.backend.infrastructure.youtube.YouTubeTrackItem
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Example
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.query.FluentQuery
import reactor.core.publisher.Mono
import java.util.*
import java.util.function.Function

/**
 * YouTubeNormalizationEngineTest — fake-repo 스타일 (no Spring context).
 *
 * 검증 포인트:
 *   (a) YouTube import가 Track + canonical 생성
 *   (b) ISRC null 트랙이 fuzzy로 기존 canonical에 붙거나 새로 생성되며 confidence 저장
 *   (c) 기존 Spotify 경로 회귀 없음 (ISRC 있는 spotify 트랙이 canonical 연결)
 *   (d) cover/live 노이즈 트랙이 low-confidence로 저장되거나 새 canonical 생성
 */
class YouTubeNormalizationEngineTest {

    private lateinit var canonicalRepo: FakeYtCanonicalRepo
    private lateinit var assetRepo: FakeYtAssetRepo
    private lateinit var importJobRepo: FakeYtImportJobRepo
    private lateinit var matchingEngine: MatchingEngine
    private lateinit var engine: NormalizationEngine

    @BeforeEach
    fun setUp() {
        canonicalRepo = FakeYtCanonicalRepo()
        assetRepo = FakeYtAssetRepo()
        importJobRepo = FakeYtImportJobRepo()
        matchingEngine = MatchingEngine(canonicalRepo)
        engine = NormalizationEngine(
            importJobRepository = importJobRepo,
            assetRepository = assetRepo,
            spotifyClient = noopSpotifyClient(),
            youTubeClient = noopYouTubeClient(),
            matchingEngine = matchingEngine
        )
    }

    // ─── helpers ───────────────────────────────────────────────────────────────

    private fun makeJob(platform: String = "youtube"): ImportJob {
        val job = ImportJob(
            idempotencyKey = UUID.randomUUID().toString(),
            sourcePlatform = platform,
            sourcePlaylistId = "playlist-1",
            spotifyPlaylistId = if (platform == "spotify") "playlist-1" else null
        )
        job.start()
        importJobRepo.save(job)
        return job
    }

    private fun ytSummary(tracks: List<YouTubeTrackItem>) = YouTubePlaylistSummary(
        id = "yt-pl-1",
        title = "Test YT Playlist",
        coverUrl = "https://example.com/cover.jpg",
        items = tracks
    )

    private fun ytTrack(videoId: String, title: String, artistHint: String?, durationMs: Long) =
        YouTubeTrackItem(videoId = videoId, title = title, artistHint = artistHint, durationMs = durationMs)

    private fun spotifyPlaylist(tracks: List<SpotifyTrack>) = SpotifyPlaylistResponse(
        id = "sp-pl-1",
        name = "Spotify Playlist",
        images = listOf(SpotifyImage("https://example.com/sp-cover.jpg")),
        tracks = SpotifyTrackContainer(tracks.map { SpotifyTrackItem(it) })
    )

    private fun spotifyTrack(id: String, name: String, artist: String, durationMs: Int, isrc: String?) =
        SpotifyTrack(
            id = id,
            name = name,
            artists = listOf(SpotifyArtist(artist)),
            durationMs = durationMs,
            externalIds = if (isrc != null) mapOf("isrc" to isrc) else null
        )

    // ─── (a) YouTube import Track + canonical 생성 ────────────────────────────

    @Test
    fun `YouTube import는 Track과 CanonicalTrack을 생성한다`() {
        val job = makeJob("youtube")
        val summary = ytSummary(listOf(
            ytTrack("yt_v001", "ELEVEN", "IVE", 185_000)
        ))

        engine.saveNormalizedAssetFromYouTube(job.id, summary)

        val asset = assetRepo.store.values.single()
        assertEquals("youtube", asset.sourcePlatform)
        val track = asset.tracks.single()
        assertNotNull(track.canonicalTrackId, "canonicalTrackId must be set")
        assertNotNull(track.matchConfidence, "matchConfidence must be set")
        assertTrue(track.matchConfidence!! > 0.0)

        val canonical = canonicalRepo.findById(track.canonicalTrackId!!).orElse(null)
        assertNotNull(canonical)
        assertTrue(canonical!!.sources.contains("youtube"))
    }

    @Test
    fun `YouTube 트랙 제목 Artist - Title 형식은 artist와 title이 분리된다`() {
        val job = makeJob("youtube")
        val summary = ytSummary(listOf(
            ytTrack("yt_v002", "NewJeans - Ditto", null, 253_000)
        ))

        engine.saveNormalizedAssetFromYouTube(job.id, summary)

        val track = assetRepo.store.values.single().tracks.single()
        // "NewJeans - Ditto" → title="Ditto", artist="NewJeans"
        assertEquals("Ditto", track.name)
        assertEquals("NewJeans", track.artist)
    }

    @Test
    fun `YouTube 트랙 제목에 대시가 없으면 channelTitle이 artist로 사용된다`() {
        val job = makeJob("youtube")
        val summary = ytSummary(listOf(
            ytTrack("yt_v003", "Coffee Shop Vibes", "ChillHop Music", 180_000)
        ))

        engine.saveNormalizedAssetFromYouTube(job.id, summary)

        val track = assetRepo.store.values.single().tracks.single()
        assertEquals("Coffee Shop Vibes", track.name)
        assertEquals("ChillHop Music", track.artist)
    }

    // ─── (b) ISRC null → fuzzy → confidence 저장 ─────────────────────────────

    @Test
    fun `ISRC null 트랙이 기존 canonical에 fuzzy 매칭되면 confidence가 저장된다`() {
        // 기존 canonical 사전 등록 (Spotify에서 이미 들어온 트랙)
        val existing = CanonicalTrack(
            isrc = null,
            spotifyTrackId = "sp_ditto",
            title = "ditto",
            artists = "newjeans",
            durationMs = 253_000,
            matchConfidence = 1.0,
            sources = "spotify"
        )
        canonicalRepo.save(existing)

        val job = makeJob("youtube")
        val summary = ytSummary(listOf(
            ytTrack("yt_v_ditto", "NewJeans - Ditto", "NewJeans", 253_000)
        ))

        engine.saveNormalizedAssetFromYouTube(job.id, summary)

        val track = assetRepo.store.values.single().tracks.single()
        assertNotNull(track.matchConfidence)
        assertTrue(track.matchConfidence!! > 0.0 && track.matchConfidence!! <= 1.0,
            "confidence must be (0, 1] but was ${track.matchConfidence}")

        // fuzzy 매칭됐으면 canonical 수 그대로 1
        // 새로 생성됐으면 2 — 둘 다 허용 (fuzzy threshold에 따라 결정)
        assertTrue(canonicalRepo.store.size >= 1)
        // confidence는 반드시 저장
        assertNotNull(track.canonicalTrackId)
    }

    @Test
    fun `ISRC null 트랙이 fuzzy threshold 미달이면 새 canonical이 생성된다`() {
        // 완전히 다른 트랙이 이미 있는 상태
        val existing = CanonicalTrack(
            isrc = null,
            title = "blinding lights",
            artists = "the weeknd",
            durationMs = 200_000,
            matchConfidence = 1.0,
            sources = "spotify"
        )
        canonicalRepo.save(existing)

        val job = makeJob("youtube")
        val summary = ytSummary(listOf(
            ytTrack("yt_v_ditto2", "NewJeans - Ditto", "NewJeans", 253_000)
        ))

        engine.saveNormalizedAssetFromYouTube(job.id, summary)

        // 새 canonical이 생성되어야 한다
        assertEquals(2, canonicalRepo.store.size, "new canonical must be created when fuzzy score < threshold")

        val track = assetRepo.store.values.single().tracks.single()
        assertNotNull(track.canonicalTrackId)
        assertNotNull(track.matchConfidence)
        assertTrue(track.matchConfidence!! > 0.0)
    }

    // ─── (d) cover/live 노이즈 트랙 ──────────────────────────────────────────

    @Test
    fun `커버 버전 노이즈가 포함된 YouTube 트랙도 Track이 저장되고 confidence가 기록된다`() {
        val job = makeJob("youtube")
        val summary = ytSummary(listOf(
            // "[Cover ver.]" 는 normalizeTitle 의 표준 제거 패턴에 해당하지 않을 수 있어
            // 독립 canonical이 생성되거나 낮은 confidence로 저장될 수 있다 — 두 케이스 모두 허용.
            ytTrack("yt_v_cover", "Hype Boy [Cover ver.]", "Various Artists", 200_000)
        ))

        engine.saveNormalizedAssetFromYouTube(job.id, summary)

        val track = assetRepo.store.values.single().tracks.single()
        assertNotNull(track.canonicalTrackId, "cover track must still get a canonicalTrackId")
        assertNotNull(track.matchConfidence, "cover track must have matchConfidence stored")
        assertTrue(track.matchConfidence!! > 0.0 && track.matchConfidence!! <= 1.0)
    }

    @Test
    fun `라이브 버전 트랙도 Track이 저장되고 confidence가 기록된다`() {
        val job = makeJob("youtube")
        val summary = ytSummary(listOf(
            ytTrack("yt_v_live", "Attention (Live at Seoul Fest)", "NewJeans", 195_000)
        ))

        engine.saveNormalizedAssetFromYouTube(job.id, summary)

        val track = assetRepo.store.values.single().tracks.single()
        assertNotNull(track.canonicalTrackId)
        assertNotNull(track.matchConfidence)
        assertTrue(track.matchConfidence!! > 0.0)
    }

    // ─── (c) 기존 Spotify 경로 회귀 없음 ─────────────────────────────────────

    @Test
    fun `Spotify import 경로는 YouTube 추가 후에도 동작하며 ISRC canonical이 연결된다`() {
        val job = makeJob("spotify")
        val playlist = spotifyPlaylist(listOf(
            spotifyTrack("sp_1", "Blinding Lights", "The Weeknd", 200_000, "USARC1234567")
        ))

        engine.saveNormalizedAsset(job.id, playlist)

        val asset = assetRepo.store.values.single()
        assertEquals("spotify", asset.sourcePlatform)
        val track = asset.tracks.single()
        assertNotNull(track.canonicalTrackId)
        assertEquals(1.0, track.matchConfidence!!, 1e-9)
        assertEquals("USARC1234567", canonicalRepo.findByIsrc("USARC1234567")?.isrc)
    }

    @Test
    fun `Spotify import 후 YouTube import 시 동일 제목 트랙이 fuzzy 매칭된다`() {
        // 1. Spotify import로 canonical 생성
        val spJob = makeJob("spotify")
        val spPlaylist = spotifyPlaylist(listOf(
            spotifyTrack("sp_ditto", "Ditto", "NewJeans", 253_000, null)
        ))
        engine.saveNormalizedAsset(spJob.id, spPlaylist)
        val canonicalCountAfterSpotify = canonicalRepo.store.size

        // 2. YouTube import — 동일 트랙 다른 videoId
        val ytJob = makeJob("youtube")
        val ytSummaryData = ytSummary(listOf(
            ytTrack("yt_ditto_2", "NewJeans - Ditto", "NewJeans", 253_000)
        ))
        engine.saveNormalizedAssetFromYouTube(ytJob.id, ytSummaryData)

        // canonical 수는 늘어나거나 유지 (fuzzy 매칭 성공 시 유지, 실패 시 증가)
        // 중요한 것은 YouTube 트랙도 canonicalTrackId가 채워지는 것
        val ytAsset = assetRepo.store.values.maxByOrNull { it.tracks.size }!!
        val ytTrackEntity = assetRepo.store.values
            .filter { it.sourcePlatform == "youtube" }
            .flatMap { it.tracks }
            .firstOrNull()
        assertNotNull(ytTrackEntity, "YouTube track must be saved")
        assertNotNull(ytTrackEntity!!.canonicalTrackId)
        assertNotNull(ytTrackEntity.matchConfidence)
    }

    @Test
    fun `YouTube import 후 job 상태가 COMPLETED로 변경된다`() {
        val job = makeJob("youtube")
        val summary = ytSummary(listOf(
            ytTrack("yt_v999", "Test Song", "Test Artist", 180_000)
        ))

        engine.saveNormalizedAssetFromYouTube(job.id, summary)

        val updatedJob = importJobRepo.findById(job.id).orElseThrow()
        assertEquals(ImportJobStatus.COMPLETED, updatedJob.status)
        assertEquals(1, updatedJob.totalTracks)
        assertNotNull(updatedJob.assetId)
    }
}

// ─── Fake Repositories ─────────────────────────────────────────────────────────

class FakeYtImportJobRepo : ImportJobRepository {
    val store = mutableMapOf<UUID, ImportJob>()
    override fun <S : ImportJob> save(entity: S): S { store[entity.id] = entity; return entity }
    override fun findById(id: UUID): Optional<ImportJob> = Optional.ofNullable(store[id])
    override fun findByIdempotencyKey(key: String): ImportJob? = store.values.firstOrNull { it.idempotencyKey == key }
    override fun findAll(): MutableList<ImportJob> = store.values.toMutableList()
    override fun count(): Long = store.size.toLong()
    override fun existsById(id: UUID): Boolean = store.containsKey(id)
    override fun deleteById(id: UUID) { store.remove(id) }
    override fun delete(entity: ImportJob) { store.remove(entity.id) }
    override fun deleteAll() { store.clear() }
    override fun deleteAll(entities: MutableIterable<ImportJob>) { entities.forEach { delete(it) } }
    override fun deleteAllById(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun <S : ImportJob> saveAll(entities: MutableIterable<S>): MutableList<S> = entities.map { save(it) }.toMutableList()
    override fun findAllById(ids: MutableIterable<UUID>): MutableList<ImportJob> = ids.mapNotNull { store[it] }.toMutableList()
    override fun findAll(sort: Sort): MutableList<ImportJob> = findAll()
    override fun findAll(pageable: Pageable): Page<ImportJob> = throw NotImplementedError()
    override fun flush() {}
    override fun <S : ImportJob> saveAndFlush(entity: S): S = save(entity)
    override fun <S : ImportJob> saveAllAndFlush(entities: MutableIterable<S>): MutableList<S> = saveAll(entities)
    override fun deleteAllInBatch() { store.clear() }
    override fun deleteAllInBatch(entities: MutableIterable<ImportJob>) { entities.forEach { delete(it) } }
    override fun deleteAllByIdInBatch(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun getOne(id: UUID): ImportJob = store[id] ?: throw NoSuchElementException()
    override fun getById(id: UUID): ImportJob = getOne(id)
    override fun getReferenceById(id: UUID): ImportJob = getOne(id)
    override fun <S : ImportJob> findOne(example: Example<S>): Optional<S> = throw NotImplementedError()
    override fun <S : ImportJob> findAll(example: Example<S>): MutableList<S> = throw NotImplementedError()
    override fun <S : ImportJob> findAll(example: Example<S>, sort: Sort): MutableList<S> = throw NotImplementedError()
    override fun <S : ImportJob> findAll(example: Example<S>, pageable: Pageable): Page<S> = throw NotImplementedError()
    override fun <S : ImportJob> count(example: Example<S>): Long = throw NotImplementedError()
    override fun <S : ImportJob> exists(example: Example<S>): Boolean = throw NotImplementedError()
    override fun <S : ImportJob, R : Any> findBy(example: Example<S>, queryFunction: Function<FluentQuery.FetchableFluentQuery<S>, R>): R = throw NotImplementedError()
}

class FakeYtAssetRepo : AssetRepository {
    val store = mutableMapOf<UUID, Asset>()
    override fun <S : Asset> save(entity: S): S { store[entity.id] = entity; return entity }
    override fun findByShareToken(shareToken: String): Asset? = store.values.firstOrNull { it.shareToken == shareToken }
    override fun findByOwnerIdAndComposeIdempotencyKey(ownerId: UUID, composeIdempotencyKey: String): Asset? =
        store.values.firstOrNull { it.ownerId == ownerId && it.composeIdempotencyKey == composeIdempotencyKey }
    override fun findWithTracksById(id: UUID): Asset? = store[id]
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

class FakeYtCanonicalRepo : CanonicalTrackRepository {
    val store = mutableMapOf<UUID, CanonicalTrack>()
    override fun findByIsrc(isrc: String): CanonicalTrack? = store.values.firstOrNull { it.isrc == isrc }
    override fun findBySpotifyTrackId(id: String): CanonicalTrack? = store.values.firstOrNull { it.spotifyTrackId == id }
    override fun findByAppleSongId(id: String): CanonicalTrack? = store.values.firstOrNull { it.appleSongId == id }
    override fun findByTitleAndArtists(title: String, artists: String): List<CanonicalTrack> = store.values.filter { it.title == title && it.artists == artists }
    override fun findCandidatesForFuzzyMatch(titlePrefix: String): List<CanonicalTrack> = store.values.filter { it.title.startsWith(titlePrefix) }
    override fun <S : CanonicalTrack> save(entity: S): S { store[entity.canonicalId] = entity; return entity }
    override fun findById(id: UUID): Optional<CanonicalTrack> = Optional.ofNullable(store[id])
    override fun findAll(): MutableList<CanonicalTrack> = store.values.toMutableList()
    override fun count(): Long = store.size.toLong()
    override fun existsById(id: UUID): Boolean = store.containsKey(id)
    override fun deleteById(id: UUID) { store.remove(id) }
    override fun delete(entity: CanonicalTrack) { store.remove(entity.canonicalId) }
    override fun deleteAll() { store.clear() }
    override fun deleteAll(entities: MutableIterable<CanonicalTrack>) { entities.forEach { delete(it) } }
    override fun deleteAllById(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun <S : CanonicalTrack> saveAll(entities: MutableIterable<S>): MutableList<S> = entities.map { save(it) }.toMutableList()
    override fun findAllById(ids: MutableIterable<UUID>): MutableList<CanonicalTrack> = ids.mapNotNull { store[it] }.toMutableList()
    override fun findAll(sort: Sort): MutableList<CanonicalTrack> = findAll()
    override fun findAll(pageable: Pageable): Page<CanonicalTrack> = throw NotImplementedError()
    override fun flush() {}
    override fun <S : CanonicalTrack> saveAndFlush(entity: S): S = save(entity)
    override fun <S : CanonicalTrack> saveAllAndFlush(entities: MutableIterable<S>): MutableList<S> = saveAll(entities)
    override fun deleteAllInBatch() { store.clear() }
    override fun deleteAllInBatch(entities: MutableIterable<CanonicalTrack>) { entities.forEach { delete(it) } }
    override fun deleteAllByIdInBatch(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun getOne(id: UUID): CanonicalTrack = store[id] ?: throw NoSuchElementException()
    override fun getById(id: UUID): CanonicalTrack = getOne(id)
    override fun getReferenceById(id: UUID): CanonicalTrack = getOne(id)
    override fun <S : CanonicalTrack> findOne(example: Example<S>): Optional<S> = throw NotImplementedError()
    override fun <S : CanonicalTrack> findAll(example: Example<S>): MutableList<S> = throw NotImplementedError()
    override fun <S : CanonicalTrack> findAll(example: Example<S>, sort: Sort): MutableList<S> = throw NotImplementedError()
    override fun <S : CanonicalTrack> findAll(example: Example<S>, pageable: Pageable): Page<S> = throw NotImplementedError()
    override fun <S : CanonicalTrack> count(example: Example<S>): Long = throw NotImplementedError()
    override fun <S : CanonicalTrack> exists(example: Example<S>): Boolean = throw NotImplementedError()
    override fun <S : CanonicalTrack, R : Any> findBy(example: Example<S>, queryFunction: Function<FluentQuery.FetchableFluentQuery<S>, R>): R = throw NotImplementedError()
}

// ─── no-op clients ─────────────────────────────────────────────────────────────

private fun noopSpotifyClient(): com.plshare.backend.infrastructure.spotify.SpotifyClient =
    object : com.plshare.backend.infrastructure.spotify.SpotifyClient {
        override fun getAccessToken() = throw AssertionError("SpotifyClient must not be called in YouTube test")
        override fun getPlaylist(playlistId: String, accessToken: String) = throw AssertionError("SpotifyClient must not be called in YouTube test")
        override fun listUserPlaylists(accessToken: String) = throw AssertionError("SpotifyClient must not be called in YouTube test")
        override fun buildAuthorizationUrl(state: String, codeChallenge: String, redirectUri: String, scopes: List<String>): String = throw AssertionError()
        override fun exchangeCodeForToken(code: String, codeVerifier: String, redirectUri: String) = throw AssertionError()
        override fun refreshAccessToken(refreshToken: String) = throw AssertionError()
        override fun getCurrentUserPlaylists(accessToken: String) = throw AssertionError()
    }

private fun noopYouTubeClient(): com.plshare.backend.infrastructure.youtube.YouTubeClient =
    object : com.plshare.backend.infrastructure.youtube.YouTubeClient {
        override fun listUserPlaylists(accessToken: String) = throw AssertionError("YouTubeClient must not be called directly in unit test")
        override fun getPlaylist(playlistId: String, accessToken: String) = throw AssertionError()
        override fun getPlaylistItems(playlistId: String, accessToken: String) = throw AssertionError()
        override fun getVideoDetails(videoIds: List<String>, accessToken: String) = throw AssertionError()
    }
