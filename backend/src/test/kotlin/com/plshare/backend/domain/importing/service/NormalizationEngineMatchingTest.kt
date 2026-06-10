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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Example
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.query.FluentQuery
import java.util.Optional
import java.util.UUID
import java.util.function.Function

/**
 * NormalizationEngine 통합 검증 — fake repositories (no Spring context).
 *
 * 검증 포인트:
 *   (a) ISRC 있는 트랙이 canonical 생성 + Track.canonicalTrackId 연결
 *   (b) 동일 ISRC 재import 시 canonical 재사용 (새로 생성되지 않음)
 *   (c) matchConfidence 가 Track에 저장됨
 */
class NormalizationEngineMatchingTest {

    private lateinit var canonicalRepo: FakeCanonicalRepo
    private lateinit var assetRepo: FakeAssetRepo
    private lateinit var importJobRepo: FakeImportJobRepo
    private lateinit var matchingEngine: MatchingEngine
    private lateinit var engine: NormalizationEngine

    @BeforeEach
    fun setUp() {
        canonicalRepo = FakeCanonicalRepo()
        assetRepo = FakeAssetRepo()
        importJobRepo = FakeImportJobRepo()
        matchingEngine = MatchingEngine(canonicalRepo)
        engine = NormalizationEngine(
            importJobRepository = importJobRepo,
            assetRepository = assetRepo,
            spotifyClient = TODO_SpotifyClient(),
            youTubeClient = TODO_YouTubeClient(),
            matchingEngine = matchingEngine
        )
    }

    // ─── helpers ───────────────────────────────────────────────────────────

    private fun makeJob(): ImportJob {
        val job = ImportJob(idempotencyKey = UUID.randomUUID().toString())
        job.start()
        importJobRepo.save(job)
        return job
    }

    private fun makePlaylist(tracks: List<SpotifyTrack>): SpotifyPlaylistResponse =
        SpotifyPlaylistResponse(
            id = "playlist_1",
            name = "Test Playlist",
            images = listOf(SpotifyImage("https://example.com/cover.jpg")),
            tracks = SpotifyTrackContainer(tracks.map { SpotifyTrackItem(it) })
        )

    private fun spotifyTrack(
        id: String,
        name: String,
        artists: List<String>,
        durationMs: Int,
        isrc: String?
    ) = SpotifyTrack(
        id = id,
        name = name,
        artists = artists.map { SpotifyArtist(it) },
        durationMs = durationMs,
        externalIds = if (isrc != null) mapOf("isrc" to isrc) else null
    )

    // ─── tests ─────────────────────────────────────────────────────────────

    @Test
    fun `ISRC 있는 트랙은 canonical이 생성되고 Track에 canonicalTrackId가 연결된다`() {
        val job = makeJob()
        val playlist = makePlaylist(listOf(
            spotifyTrack("sp_1", "Blinding Lights", listOf("The Weeknd"), 200_000, "USARC1234567")
        ))

        engine.saveNormalizedAsset(job.id, playlist)

        val savedAsset = assetRepo.store.values.single()
        val track = savedAsset.tracks.single()

        assertNotNull(track.canonicalTrackId, "canonicalTrackId must be set")
        assertNotNull(track.matchConfidence, "matchConfidence must be set")
        assertEquals(1.0, track.matchConfidence!!, 1e-9)

        // canonical 이 실제로 canonicalRepo에 저장돼야 함
        val canonical = canonicalRepo.findById(track.canonicalTrackId!!).orElse(null)
        assertNotNull(canonical)
        assertEquals("USARC1234567", canonical!!.isrc)
    }

    @Test
    fun `동일 ISRC 재import 시 canonical이 재사용되고 새로 생성되지 않는다`() {
        val job1 = makeJob()
        val playlist = makePlaylist(listOf(
            spotifyTrack("sp_1", "Blinding Lights", listOf("The Weeknd"), 200_000, "USARC9999999")
        ))

        engine.saveNormalizedAsset(job1.id, playlist)

        val canonicalCountAfterFirst = canonicalRepo.store.size

        val job2 = makeJob()
        val playlist2 = makePlaylist(listOf(
            spotifyTrack("sp_1_dup", "Blinding Lights", listOf("The Weeknd"), 200_000, "USARC9999999")
        ))
        engine.saveNormalizedAsset(job2.id, playlist2)

        // canonical 개수는 첫 import 이후 증가하지 않아야 한다
        assertEquals(canonicalCountAfterFirst, canonicalRepo.store.size,
            "Re-import with same ISRC must reuse canonical, not create a new one")

        // 두 asset의 track 모두 동일한 canonicalTrackId 를 가져야 한다
        val allTracks = assetRepo.store.values.flatMap { it.tracks }
        assertEquals(2, allTracks.size)
        val ids = allTracks.map { it.canonicalTrackId }.toSet()
        assertEquals(1, ids.size, "Both tracks must link to the same canonical")
        assertFalse(ids.contains(null), "canonicalTrackId must not be null")
    }

    @Test
    fun `matchConfidence가 Track 엔티티에 저장된다`() {
        val job = makeJob()
        // ISRC 없는 트랙 → fuzzy 또는 신규 생성, confidence 저장 여부만 검증
        val playlist = makePlaylist(listOf(
            spotifyTrack("sp_2", "Bohemian Rhapsody", listOf("Queen"), 354_000, null)
        ))

        engine.saveNormalizedAsset(job.id, playlist)

        val track = assetRepo.store.values.single().tracks.single()
        assertNotNull(track.matchConfidence, "matchConfidence must be set even for newly-created canonical")
        assertTrue(track.matchConfidence!! > 0.0 && track.matchConfidence!! <= 1.0)
        assertNotNull(track.canonicalTrackId)
    }
}

// ─── Fake Repositories ─────────────────────────────────────────────────────

class FakeImportJobRepo : ImportJobRepository {
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

class FakeAssetRepo : AssetRepository {
    val store = mutableMapOf<UUID, Asset>()

    override fun <S : Asset> save(entity: S): S { store[entity.id] = entity; return entity }
    override fun findByShareToken(shareToken: String): Asset? = store.values.firstOrNull { it.shareToken == shareToken }
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

class FakeCanonicalRepo : CanonicalTrackRepository {
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

/**
 * NormalizationEngine 단위 테스트에서는 SpotifyClient를 사용하지 않는다.
 * saveNormalizedAsset 은 이미 resolved 된 SpotifyPlaylistResponse 를 직접 받으므로
 * client 는 호출되지 않는다. 혹시라도 호출되면 즉시 실패해야 한다.
 */
private fun TODO_SpotifyClient(): com.plshare.backend.infrastructure.spotify.SpotifyClient =
    object : com.plshare.backend.infrastructure.spotify.SpotifyClient {
        override fun getAccessToken() = throw AssertionError("SpotifyClient must not be called in unit test")
        override fun getPlaylist(playlistId: String, accessToken: String) = throw AssertionError("SpotifyClient must not be called in unit test")
        override fun listUserPlaylists(accessToken: String) = throw AssertionError("SpotifyClient must not be called in unit test")
        override fun buildAuthorizationUrl(state: String, codeChallenge: String, redirectUri: String, scopes: List<String>): String = throw AssertionError("SpotifyClient must not be called in unit test")
        override fun exchangeCodeForToken(code: String, codeVerifier: String, redirectUri: String) = throw AssertionError("SpotifyClient must not be called in unit test")
        override fun refreshAccessToken(refreshToken: String) = throw AssertionError("SpotifyClient must not be called in unit test")
        override fun getCurrentUserPlaylists(accessToken: String) = throw AssertionError("SpotifyClient must not be called in unit test")
    }

/** saveNormalizedAsset(Spotify) は YouTubeClient를 사용하지 않는다. 호출되면 즉시 실패. */
private fun TODO_YouTubeClient(): com.plshare.backend.infrastructure.youtube.YouTubeClient =
    object : com.plshare.backend.infrastructure.youtube.YouTubeClient {
        override fun listUserPlaylists(accessToken: String) = throw AssertionError("YouTubeClient must not be called in Spotify unit test")
        override fun getPlaylist(playlistId: String, accessToken: String) = throw AssertionError("YouTubeClient must not be called in Spotify unit test")
        override fun getPlaylistItems(playlistId: String, accessToken: String) = throw AssertionError("YouTubeClient must not be called in Spotify unit test")
        override fun getVideoDetails(videoIds: List<String>, accessToken: String) = throw AssertionError("YouTubeClient must not be called in Spotify unit test")
    }
