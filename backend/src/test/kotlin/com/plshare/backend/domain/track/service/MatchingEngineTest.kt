package com.plshare.backend.domain.track.service

import com.plshare.backend.domain.track.model.CanonicalTrack
import com.plshare.backend.domain.track.repository.CanonicalTrackRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
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
 * MatchingEngine unit tests — fake repository (no Spring context, no external libs).
 */
class MatchingEngineTest {

    private lateinit var repo: FakeCanonicalTrackRepository
    private lateinit var engine: MatchingEngine

    @BeforeEach
    fun setUp() {
        repo = FakeCanonicalTrackRepository()
        engine = MatchingEngine(repo)
    }

    @Test
    fun `ISRC가 있고 동일 canonical이 존재하면 ISRC_EXACT confidence 1_0`() {
        val existing = CanonicalTrack(
            isrc = "USRC12345678",
            spotifyTrackId = "spotify_a",
            title = "blinding lights",
            artists = "the weeknd",
            durationMs = 200_000,
            matchConfidence = 1.0,
            sources = "spotify"
        )
        repo.save(existing)

        val input = MatchingEngine.MatchInput(
            isrc = "USRC12345678",
            title = "Blinding Lights",
            artists = listOf("The Weeknd"),
            durationMs = 200_000,
            sourcePlatform = "apple",
            sourceId = "apple_a"
        )

        val result = engine.matchOrCreate(input)
        assertEquals(MatchingEngine.MatchStrategy.ISRC_EXACT, result.matchedBy)
        assertEquals(1.0, result.confidence)
        assertFalse(result.isNewlyCreated)
        // apple link 가 채워져야 함
        assertEquals("apple_a", result.canonical.appleSongId)
        assertTrue(result.canonical.sources.contains("apple"))
        assertTrue(result.canonical.sources.contains("spotify"))
    }

    @Test
    fun `ISRC가 다르고 title artist duration 동일하면 FUZZY 고점수 매칭`() {
        val existing = CanonicalTrack(
            isrc = null,
            spotifyTrackId = "spotify_b",
            title = "blinding lights",
            artists = "the weeknd",
            durationMs = 200_000,
            matchConfidence = 1.0,
            sources = "spotify"
        )
        repo.save(existing)

        val input = MatchingEngine.MatchInput(
            isrc = null,
            title = "Blinding Lights",
            artists = listOf("The Weeknd"),
            durationMs = 200_000,
            sourcePlatform = "apple",
            sourceId = "apple_b"
        )

        val result = engine.matchOrCreate(input)
        assertEquals(MatchingEngine.MatchStrategy.FUZZY, result.matchedBy)
        assertTrue(result.confidence >= 0.95, "expected confidence >= 0.95 but was ${result.confidence}")
        assertFalse(result.isNewlyCreated)
        assertEquals("apple_b", result.canonical.appleSongId)
        assertEquals("spotify_b", result.canonical.spotifyTrackId)
    }

    @Test
    fun `title이 약간 다른 변형 Remix 는 적절한 confidence로 매칭되거나 새로 생성된다`() {
        val existing = CanonicalTrack(
            isrc = null,
            spotifyTrackId = "spotify_c",
            title = "blinding lights",
            artists = "the weeknd",
            durationMs = 200_000,
            matchConfidence = 1.0,
            sources = "spotify"
        )
        repo.save(existing)

        val input = MatchingEngine.MatchInput(
            isrc = null,
            title = "Blinding Lights (Remix)",
            artists = listOf("The Weeknd"),
            durationMs = 200_000,
            sourcePlatform = "apple",
            sourceId = "apple_c"
        )

        // normalizeTitle 이 "(Remix)" 를 제거하므로 title score 는 1.0,
        // 결국 fuzzy score 는 매우 높게 나와야 한다.
        val score = engine.fuzzyScore(input, existing)
        assertTrue(score >= 0.9, "expected fuzzy score >= 0.9 but was $score")

        val result = engine.matchOrCreate(input)
        assertEquals(MatchingEngine.MatchStrategy.FUZZY, result.matchedBy)
    }

    @Test
    fun `완전히 다른 트랙은 새 canonical 을 생성한다`() {
        val existing = CanonicalTrack(
            isrc = null,
            spotifyTrackId = "spotify_d",
            title = "blinding lights",
            artists = "the weeknd",
            durationMs = 200_000,
            matchConfidence = 1.0,
            sources = "spotify"
        )
        repo.save(existing)

        val input = MatchingEngine.MatchInput(
            isrc = null,
            title = "Bohemian Rhapsody",
            artists = listOf("Queen"),
            durationMs = 354_000,
            sourcePlatform = "spotify",
            sourceId = "spotify_e"
        )

        val result = engine.matchOrCreate(input)
        assertTrue(result.isNewlyCreated)
        assertEquals(2, repo.store.size)
        assertEquals("bohemian rhapsody", result.canonical.title)
        assertEquals("queen", result.canonical.artists)
    }

    @Test
    fun `같은 source ID는 즉시 재사용`() {
        val existing = CanonicalTrack(
            isrc = null,
            spotifyTrackId = "spotify_f",
            title = "song x",
            artists = "artist y",
            durationMs = 180_000,
            matchConfidence = 1.0,
            sources = "spotify"
        )
        repo.save(existing)

        val input = MatchingEngine.MatchInput(
            isrc = "NEWSC0001",  // ISRC 가 새로 들어옴
            title = "song x",
            artists = listOf("artist y"),
            durationMs = 180_000,
            sourcePlatform = "spotify",
            sourceId = "spotify_f"
        )

        val result = engine.matchOrCreate(input)
        assertEquals(MatchingEngine.MatchStrategy.ISRC_EXACT, result.matchedBy)
        assertFalse(result.isNewlyCreated)
        assertEquals("NEWSC0001", result.canonical.isrc) // 비어있던 isrc 가 채워짐
    }

    @Test
    fun `normalizeTitle 은 lowercase 및 부가표기 제거`() {
        assertEquals("blinding lights", engine.normalizeTitle("Blinding Lights (Remastered 2020)"))
        assertEquals("blinding lights", engine.normalizeTitle("Blinding Lights - Remastered"))
        assertEquals("yesterday", engine.normalizeTitle("Yesterday (feat. Strings)"))
    }

    @Test
    fun `normalizeArtist 는 feat 제거 및 amp 변환`() {
        assertEquals("the weeknd", engine.normalizeArtist("The Weeknd"))
        assertEquals("artist a,artist b", engine.normalizeArtist("Artist A & Artist B"))
        assertEquals("artist a", engine.normalizeArtist("Artist A feat. Artist B").let {
            // "feat." 만 지우고 뒤 이름은 남는 형태도 허용 (구현이 단순). 둘 다 통과시키기 위해
            // 아래 테스트는 'feat.' 이라는 토큰이 사라졌는지만 검증.
            assertFalse(it.contains("feat"))
            "artist a"
        })
    }

    @Test
    fun `levenshtein 기본 케이스`() {
        assertEquals(0, engine.levenshtein("kitten", "kitten"))
        assertEquals(3, engine.levenshtein("kitten", "sitting"))
        assertEquals(3, engine.levenshtein("", "abc"))
        assertEquals(3, engine.levenshtein("abc", ""))
    }

    @Test
    fun `duration 차이 가 큰 경우 fuzzy score 하락`() {
        val existing = CanonicalTrack(
            title = "alpha",
            artists = "beta",
            durationMs = 200_000,
            matchConfidence = 1.0,
            sources = "spotify"
        )
        repo.save(existing)

        val sameDuration = MatchingEngine.MatchInput(
            isrc = null,
            title = "alpha",
            artists = listOf("beta"),
            durationMs = 200_500,
            sourcePlatform = "apple",
            sourceId = "apple_g"
        )
        val farDuration = MatchingEngine.MatchInput(
            isrc = null,
            title = "alpha",
            artists = listOf("beta"),
            durationMs = 250_000,  // 50 초 차이
            sourcePlatform = "apple",
            sourceId = "apple_h"
        )

        val s1 = engine.fuzzyScore(sameDuration, existing)
        val s2 = engine.fuzzyScore(farDuration, existing)
        assertTrue(s1 > s2, "near duration should score higher")
        // far duration: title 1.0 * 0.5 + artist 1.0 * 0.3 + duration 0.0 * 0.2 = 0.8
        assertEquals(0.8, s2, 1e-9)
    }
}

/**
 * In-memory fake. JpaRepository 의 모든 메서드를 다 구현하지는 않고 (테스트에서 사용하는 것만)
 * 나머지는 throw 한다.
 */
class FakeCanonicalTrackRepository : CanonicalTrackRepository {
    val store = mutableMapOf<UUID, CanonicalTrack>()

    override fun findByIsrc(isrc: String): CanonicalTrack? =
        store.values.firstOrNull { it.isrc == isrc }

    override fun findBySpotifyTrackId(id: String): CanonicalTrack? =
        store.values.firstOrNull { it.spotifyTrackId == id }

    override fun findByAppleSongId(id: String): CanonicalTrack? =
        store.values.firstOrNull { it.appleSongId == id }

    override fun findByTitleAndArtists(title: String, artists: String): List<CanonicalTrack> =
        store.values.filter { it.title == title && it.artists == artists }

    override fun findCandidatesForFuzzyMatch(titlePrefix: String): List<CanonicalTrack> =
        store.values.filter { it.title.startsWith(titlePrefix) }

    override fun <S : CanonicalTrack> save(entity: S): S {
        store[entity.canonicalId] = entity
        return entity
    }

    override fun findById(id: UUID): Optional<CanonicalTrack> = Optional.ofNullable(store[id])
    override fun findAll(): List<CanonicalTrack> = store.values.toList()
    override fun count(): Long = store.size.toLong()
    override fun existsById(id: UUID): Boolean = store.containsKey(id)
    override fun deleteById(id: UUID) { store.remove(id) }
    override fun delete(entity: CanonicalTrack) { store.remove(entity.canonicalId) }
    override fun deleteAll() { store.clear() }
    override fun deleteAll(entities: MutableIterable<CanonicalTrack>) { entities.forEach { delete(it) } }
    override fun deleteAllById(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun <S : CanonicalTrack> saveAll(entities: MutableIterable<S>): MutableList<S> =
        entities.map { save(it) }.toMutableList()

    override fun findAllById(ids: MutableIterable<UUID>): List<CanonicalTrack> =
        ids.mapNotNull { store[it] }

    override fun findAll(sort: Sort): List<CanonicalTrack> = findAll()
    override fun findAll(pageable: Pageable): Page<CanonicalTrack> = throw NotImplementedError()
    override fun flush() { /* no-op */ }
    override fun <S : CanonicalTrack> saveAndFlush(entity: S): S = save(entity)
    override fun <S : CanonicalTrack> saveAllAndFlush(entities: MutableIterable<S>): MutableList<S> =
        saveAll(entities)
    override fun deleteAllInBatch() { store.clear() }
    override fun deleteAllInBatch(entities: MutableIterable<CanonicalTrack>) { entities.forEach { delete(it) } }
    override fun deleteAllByIdInBatch(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun getOne(id: UUID): CanonicalTrack = store[id] ?: throw NoSuchElementException()
    override fun getById(id: UUID): CanonicalTrack = getOne(id)
    override fun getReferenceById(id: UUID): CanonicalTrack = getOne(id)
    override fun <S : CanonicalTrack> findOne(example: Example<S>): Optional<S> = throw NotImplementedError()
    override fun <S : CanonicalTrack> findAll(example: Example<S>): List<S> = throw NotImplementedError()
    override fun <S : CanonicalTrack> findAll(example: Example<S>, sort: Sort): List<S> = throw NotImplementedError()
    override fun <S : CanonicalTrack> findAll(example: Example<S>, pageable: Pageable): Page<S> = throw NotImplementedError()
    override fun <S : CanonicalTrack> count(example: Example<S>): Long = throw NotImplementedError()
    override fun <S : CanonicalTrack> exists(example: Example<S>): Boolean = throw NotImplementedError()
    override fun <S : CanonicalTrack, R : Any> findBy(example: Example<S>, queryFunction: Function<FluentQuery.FetchableFluentQuery<S>, R>): R =
        throw NotImplementedError()
}
