package com.plshare.backend.domain.ranking

import com.plshare.backend.domain.asset.model.Asset
import com.plshare.backend.domain.asset.repository.AssetRepository
import com.plshare.backend.domain.ranking.dto.AssetRankRow
import com.plshare.backend.domain.ranking.dto.UserRankRow
import com.plshare.backend.domain.ranking.repository.RankingQueryRepository
import com.plshare.backend.domain.ranking.service.RankingService
import com.plshare.backend.domain.user.model.User
import com.plshare.backend.domain.user.repository.UserRepository
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Example
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.query.FluentQuery
import java.time.LocalDateTime
import java.util.*
import java.util.function.Function

/**
 * RankingService 단위 테스트 — fake repository (no Spring context, no DB).
 *
 * 불변식:
 * (a) 좋아요 많은 자산이 상위 순위.
 * (b) 기간 윈도우가 오래된 포스트 제외 (since 필터).
 * (c) 빈 데이터 → 빈 PageResponse.
 * (d) period 값 잘못되면 VALIDATION_FAILED.
 * (e) 사용자 랭킹 — 좋아요 + 팔로워 합산 순위.
 */
class RankingServiceTest {

    private lateinit var rankingRepo: FakeRankingQueryRepo
    private lateinit var assetRepo: FakeRankingAssetRepo
    private lateinit var userRepo: FakeRankingUserRepo
    private lateinit var service: RankingService

    @BeforeEach
    fun setUp() {
        rankingRepo = FakeRankingQueryRepo()
        assetRepo   = FakeRankingAssetRepo()
        userRepo    = FakeRankingUserRepo()
        service = RankingService(rankingRepo, assetRepo, userRepo)
    }

    // ── (a) 좋아요 많은 자산이 상위 순위 ─────────────────────────────

    @Test
    fun `좋아요 많은 자산이 상위 순위로 반환된다`() {
        val asset1 = assetRepo.saveAsset(Asset(title = "Popular", coverUrl = null))
        val asset2 = assetRepo.saveAsset(Asset(title = "Less Popular", coverUrl = null))

        // asset1이 더 높은 점수
        rankingRepo.assetRows = listOf(
            AssetRankRow(assetId = asset1.id, score = 100, latestPostCreatedAt = LocalDateTime.now()),
            AssetRankRow(assetId = asset2.id, score = 20,  latestPostCreatedAt = LocalDateTime.now()),
        )
        rankingRepo.assetCount = 2

        val result = service.rankPlaylists("realtime", PageRequest.of(0, 20))

        assertEquals(2, result.content.size)
        assertEquals("Popular", result.content[0].title)
        assertEquals(1, result.content[0].rank)
        assertEquals(100L, result.content[0].score)
        assertEquals("Less Popular", result.content[1].title)
        assertEquals(2, result.content[1].rank)
    }

    // ── (b) 기간 윈도우 — since 파라미터가 전달되는지 검증 ───────────

    @Test
    fun `weekly period는 since 파라미터를 전달한다`() {
        val asset = assetRepo.saveAsset(Asset(title = "Recent", coverUrl = null))
        rankingRepo.assetRows = listOf(
            AssetRankRow(assetId = asset.id, score = 5, latestPostCreatedAt = LocalDateTime.now()),
        )
        rankingRepo.assetCount = 1

        val before = LocalDateTime.now().minusDays(RankingService.WEEKLY_DAYS).minusSeconds(5)
        service.rankPlaylists("weekly", PageRequest.of(0, 20))
        val after = LocalDateTime.now().minusDays(RankingService.WEEKLY_DAYS).plusSeconds(5)

        val capturedSince = rankingRepo.capturedAssetSince
        assertNotNull(capturedSince, "weekly period는 since를 전달해야 한다")
        assertTrue(
            capturedSince!!.isAfter(before) && capturedSince.isBefore(after),
            "since는 최근 7일 시점이어야 한다",
        )
    }

    @Test
    fun `realtime period는 since=null을 전달한다`() {
        rankingRepo.assetRows = emptyList()
        rankingRepo.assetCount = 0

        service.rankPlaylists("realtime", PageRequest.of(0, 20))

        assertNull(rankingRepo.capturedAssetSince, "realtime period는 since=null이어야 한다")
    }

    // ── (c) 빈 데이터 → 빈 PageResponse ─────────────────────────────

    @Test
    fun `데이터 없으면 빈 PageResponse 반환`() {
        rankingRepo.assetRows = emptyList()
        rankingRepo.assetCount = 0

        val result = service.rankPlaylists("realtime", PageRequest.of(0, 20))

        assertEquals(0, result.content.size)
        assertEquals(0L, result.totalElements)
        assertEquals(0, result.totalPages)
        assertFalse(result.hasNext)
    }

    @Test
    fun `사용자 데이터 없으면 빈 PageResponse 반환`() {
        rankingRepo.userRows = emptyList()
        rankingRepo.userCount = 0

        val result = service.rankUsers("realtime", PageRequest.of(0, 20))

        assertEquals(0, result.content.size)
        assertEquals(0L, result.totalElements)
    }

    // ── (d) period 잘못된 값 → VALIDATION_FAILED ─────────────────────

    @Test
    fun `잘못된 period 값은 VALIDATION_FAILED를 던진다`() {
        val ex = assertThrows(ApiException::class.java) {
            service.rankPlaylists("invalid-period", PageRequest.of(0, 20))
        }
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.code)
    }

    // ── (e) 사용자 랭킹 — 점수 높은 순 ──────────────────────────────

    @Test
    fun `사용자 랭킹 점수 높은 순으로 반환된다`() {
        val userA = userRepo.saveUser(User(displayName = "Alice", handle = "alice"))
        val userB = userRepo.saveUser(User(displayName = "Bob",   handle = "bob"))

        rankingRepo.userRows = listOf(
            UserRankRow(userId = userA.id, score = 200),
            UserRankRow(userId = userB.id, score = 50),
        )
        rankingRepo.userCount = 2

        val result = service.rankUsers("realtime", PageRequest.of(0, 20))

        assertEquals(2, result.content.size)
        assertEquals("alice", result.content[0].handle)
        assertEquals(1, result.content[0].rank)
        assertEquals(200L, result.content[0].score)
        assertEquals("bob", result.content[1].handle)
        assertEquals(2, result.content[1].rank)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fake repositories
// ─────────────────────────────────────────────────────────────────────────────

class FakeRankingQueryRepo : RankingQueryRepository {
    var assetRows: List<AssetRankRow> = emptyList()
    var assetCount: Long = 0
    var userRows: List<UserRankRow> = emptyList()
    var userCount: Long = 0

    var capturedAssetSince: LocalDateTime? = null
    var capturedUserSince: LocalDateTime? = null

    override fun findTopAssets(since: LocalDateTime?, limit: Int, offset: Int): List<AssetRankRow> {
        capturedAssetSince = since
        return assetRows.drop(offset).take(limit)
    }

    override fun countRankedAssets(since: LocalDateTime?): Long = assetCount

    override fun findTopUsers(since: LocalDateTime?, limit: Int, offset: Int): List<UserRankRow> {
        capturedUserSince = since
        return userRows.drop(offset).take(limit)
    }

    override fun countRankedUsers(since: LocalDateTime?): Long = userCount
}

class FakeRankingAssetRepo : AssetRepository {
    private val store = mutableMapOf<UUID, Asset>()
    fun saveAsset(a: Asset): Asset { store[a.id] = a; return a }

    override fun findByShareToken(shareToken: String) = store.values.firstOrNull { it.shareToken == shareToken }
    override fun <S : Asset> save(entity: S): S { store[entity.id] = entity; return entity }
    override fun findById(id: UUID): Optional<Asset> = Optional.ofNullable(store[id])
    override fun findAllById(ids: MutableIterable<UUID>): MutableList<Asset> = ids.mapNotNull { store[it] }.toMutableList()
    override fun findAll(): MutableList<Asset> = store.values.toMutableList()
    override fun count(): Long = store.size.toLong()
    override fun existsById(id: UUID): Boolean = store.containsKey(id)
    override fun deleteById(id: UUID) { store.remove(id) }
    override fun delete(entity: Asset) { store.remove(entity.id) }
    override fun deleteAll() { store.clear() }
    override fun deleteAll(entities: MutableIterable<Asset>) { entities.forEach { delete(it) } }
    override fun deleteAllById(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun <S : Asset> saveAll(entities: MutableIterable<S>): MutableList<S> = entities.map { save(it) }.toMutableList()
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

class FakeRankingUserRepo : UserRepository {
    private val store = mutableMapOf<UUID, User>()
    fun saveUser(u: User): User { store[u.id] = u; return u }

    override fun findByHandle(handle: String): User? = store.values.firstOrNull { it.handle == handle }
    override fun findByGoogleSubject(googleSubject: String): User? = store.values.firstOrNull { it.googleSubject == googleSubject }
    override fun findByEmail(email: String): User? = store.values.firstOrNull { it.email == email }
    override fun <S : User> save(entity: S): S { store[entity.id] = entity; return entity }
    override fun findById(id: UUID): Optional<User> = Optional.ofNullable(store[id])
    override fun findAllById(ids: MutableIterable<UUID>): MutableList<User> = ids.mapNotNull { store[it] }.toMutableList()
    override fun findAll(): MutableList<User> = store.values.toMutableList()
    override fun count(): Long = store.size.toLong()
    override fun existsById(id: UUID): Boolean = store.containsKey(id)
    override fun deleteById(id: UUID) { store.remove(id) }
    override fun delete(entity: User) { store.remove(entity.id) }
    override fun deleteAll() { store.clear() }
    override fun deleteAll(entities: MutableIterable<User>) { entities.forEach { delete(it) } }
    override fun deleteAllById(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun <S : User> saveAll(entities: MutableIterable<S>): MutableList<S> = entities.map { save(it) }.toMutableList()
    override fun findAll(sort: Sort): MutableList<User> = findAll()
    override fun findAll(pageable: Pageable): Page<User> = throw NotImplementedError()
    override fun flush() {}
    override fun <S : User> saveAndFlush(entity: S): S = save(entity)
    override fun <S : User> saveAllAndFlush(entities: MutableIterable<S>): MutableList<S> = saveAll(entities)
    override fun deleteAllInBatch() { store.clear() }
    override fun deleteAllInBatch(entities: MutableIterable<User>) { entities.forEach { delete(it) } }
    override fun deleteAllByIdInBatch(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun getOne(id: UUID): User = store[id] ?: throw NoSuchElementException()
    override fun getById(id: UUID): User = getOne(id)
    override fun getReferenceById(id: UUID): User = getOne(id)
    override fun <S : User> findOne(example: Example<S>): Optional<S> = throw NotImplementedError()
    override fun <S : User> findAll(example: Example<S>): MutableList<S> = throw NotImplementedError()
    override fun <S : User> findAll(example: Example<S>, sort: Sort): MutableList<S> = throw NotImplementedError()
    override fun <S : User> findAll(example: Example<S>, pageable: Pageable): Page<S> = throw NotImplementedError()
    override fun <S : User> count(example: Example<S>): Long = throw NotImplementedError()
    override fun <S : User> exists(example: Example<S>): Boolean = throw NotImplementedError()
    override fun <S : User, R : Any> findBy(example: Example<S>, queryFunction: Function<FluentQuery.FetchableFluentQuery<S>, R>): R = throw NotImplementedError()
}
