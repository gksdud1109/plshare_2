package com.plshare.backend.domain.gift

import com.plshare.backend.domain.asset.model.Asset
import com.plshare.backend.domain.asset.repository.AssetRepository
import com.plshare.backend.domain.gift.dto.CreateGiftRequest
import com.plshare.backend.domain.gift.model.Gift
import com.plshare.backend.domain.gift.model.GiftStatus
import com.plshare.backend.domain.gift.repository.GiftRepository
import com.plshare.backend.domain.gift.service.GiftService
import com.plshare.backend.domain.user.model.User
import com.plshare.backend.domain.user.repository.UserRepository
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Example
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.query.FluentQuery
import java.util.*
import java.util.function.Function

/**
 * GiftService 단위 테스트 — fake repository (no Spring context, no DB).
 *
 * 검증 케이스:
 * (a) 선물 생성 → token 발급 확인
 * (b) open 멱등 — 두 번 호출해도 openedAt 최초값 유지
 * (c) 없는 자산 → NOT_FOUND
 * (d) save → status SAVED + savedByUserId 전이
 * (e) save 멱등 — first-writer-wins(제3자가 최초 수령자 기록을 덮어쓰지 못함)
 */
class GiftServiceTest {

    private lateinit var giftRepo: FakeGiftRepository
    private lateinit var assetRepo: FakeAssetRepository
    private lateinit var userRepo: FakeUserRepositoryGift
    private lateinit var service: GiftService

    private lateinit var sender: User
    private lateinit var asset: Asset

    @BeforeEach
    fun setUp() {
        giftRepo = FakeGiftRepository()
        assetRepo = FakeAssetRepository()
        userRepo = FakeUserRepositoryGift()
        service = GiftService(giftRepo, assetRepo, userRepo)

        sender = userRepo.saveUser(User(displayName = "Alice", handle = "alice"))
        asset = assetRepo.saveAsset(Asset(title = "Test Asset"))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // (a) 선물 생성 → token 발급
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `선물 생성 - token과 url이 발급된다`() {
        val req = CreateGiftRequest(
            assetId = asset.id,
            message = "보내요",
            wrapSkin = "nocturne-violet",
        )
        val resp = service.create(req, sender.id)

        assertNotNull(resp.token)
        assertTrue(resp.token.isNotBlank())
        assertEquals("/gift/${resp.token}", resp.url)

        val saved = giftRepo.findByToken(resp.token)
        assertNotNull(saved)
        assertEquals(GiftStatus.CREATED, saved!!.status)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // (b) open 멱등 — 이미 열린 선물을 다시 열어도 openedAt 변경 없음
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `open 멱등 - 두 번 호출해도 openedAt이 최초값을 유지한다`() {
        val req = CreateGiftRequest(
            assetId = asset.id,
            message = "첫 번째 열기",
            wrapSkin = "nocturne-rose",
        )
        val token = service.create(req, sender.id).token

        service.open(token)
        val firstOpenedAt = giftRepo.findByToken(token)!!.openedAt

        service.open(token) // 두 번째 호출
        val secondOpenedAt = giftRepo.findByToken(token)!!.openedAt

        assertEquals(GiftStatus.OPENED, giftRepo.findByToken(token)!!.status)
        assertEquals(firstOpenedAt, secondOpenedAt) // 덮어쓰지 않음
    }

    // ─────────────────────────────────────────────────────────────────────────
    // (c) 없는 자산 → NOT_FOUND
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `없는 assetId로 선물 생성 시 NOT_FOUND`() {
        val req = CreateGiftRequest(
            assetId = UUID.randomUUID(), // 존재하지 않음
            message = "없는 자산",
            wrapSkin = "nocturne-teal",
        )
        val ex = assertThrows(ApiException::class.java) {
            service.create(req, sender.id)
        }
        assertEquals(ErrorCode.NOT_FOUND, ex.code)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // (d) save → status SAVED + savedByUserId 전이
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `save 호출 시 status가 SAVED로 전이되고 savedByUserId가 기록된다`() {
        val req = CreateGiftRequest(
            assetId = asset.id,
            message = "저장해요",
            wrapSkin = "nocturne-gold",
        )
        val token = service.create(req, sender.id).token

        val receiver = userRepo.saveUser(User(displayName = "Bob", handle = "bob"))
        service.save(token, receiver.id)

        val saved = giftRepo.findByToken(token)!!
        assertEquals(GiftStatus.SAVED, saved.status)
        assertEquals(receiver.id, saved.savedByUserId)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // (e) save 멱등 — 최초 저장자만 기록, 제3자가 덮어쓰지 못한다(first-writer-wins)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `save 멱등 - 두 번째 저장자는 최초 savedByUserId를 덮어쓰지 못한다`() {
        val req = CreateGiftRequest(
            assetId = asset.id,
            message = "선착순 저장",
            wrapSkin = "nocturne-gold",
        )
        val token = service.create(req, sender.id).token

        val firstReceiver = userRepo.saveUser(User(displayName = "Bob", handle = "bob"))
        val secondReceiver = userRepo.saveUser(User(displayName = "Carol", handle = "carol"))

        service.save(token, firstReceiver.id)
        service.save(token, secondReceiver.id) // 두 번째 호출 — 무시되어야 함

        val saved = giftRepo.findByToken(token)!!
        assertEquals(GiftStatus.SAVED, saved.status)
        assertEquals(firstReceiver.id, saved.savedByUserId) // 최초 저장자 유지
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fake repositories
// ─────────────────────────────────────────────────────────────────────────────

class FakeGiftRepository : GiftRepository {
    val store = mutableMapOf<UUID, Gift>()

    override fun findByToken(token: String): Gift? = store.values.firstOrNull { it.token == token }

    override fun <S : Gift> save(entity: S): S { store[entity.id] = entity; return entity }
    override fun findById(id: UUID): Optional<Gift> = Optional.ofNullable(store[id])
    override fun findAll(): MutableList<Gift> = store.values.toMutableList()
    override fun count(): Long = store.size.toLong()
    override fun existsById(id: UUID): Boolean = store.containsKey(id)
    override fun deleteById(id: UUID) { store.remove(id) }
    override fun delete(entity: Gift) { store.remove(entity.id) }
    override fun deleteAll() { store.clear() }
    override fun deleteAll(entities: MutableIterable<Gift>) { entities.forEach { delete(it) } }
    override fun deleteAllById(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun <S : Gift> saveAll(entities: MutableIterable<S>): MutableList<S> = entities.map { save(it) }.toMutableList()
    override fun findAllById(ids: MutableIterable<UUID>): MutableList<Gift> = ids.mapNotNull { store[it] }.toMutableList()
    override fun findAll(sort: Sort): MutableList<Gift> = findAll()
    override fun findAll(pageable: Pageable): Page<Gift> = throw NotImplementedError()
    override fun flush() {}
    override fun <S : Gift> saveAndFlush(entity: S): S = save(entity)
    override fun <S : Gift> saveAllAndFlush(entities: MutableIterable<S>): MutableList<S> = saveAll(entities)
    override fun deleteAllInBatch() { store.clear() }
    override fun deleteAllInBatch(entities: MutableIterable<Gift>) { entities.forEach { delete(it) } }
    override fun deleteAllByIdInBatch(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun getOne(id: UUID): Gift = store[id] ?: throw NoSuchElementException()
    override fun getById(id: UUID): Gift = getOne(id)
    override fun getReferenceById(id: UUID): Gift = getOne(id)
    override fun <S : Gift> findOne(example: Example<S>): Optional<S> = throw NotImplementedError()
    override fun <S : Gift> findAll(example: Example<S>): MutableList<S> = throw NotImplementedError()
    override fun <S : Gift> findAll(example: Example<S>, sort: Sort): MutableList<S> = throw NotImplementedError()
    override fun <S : Gift> findAll(example: Example<S>, pageable: Pageable): Page<S> = throw NotImplementedError()
    override fun <S : Gift> count(example: Example<S>): Long = throw NotImplementedError()
    override fun <S : Gift> exists(example: Example<S>): Boolean = throw NotImplementedError()
    override fun <S : Gift, R : Any> findBy(example: Example<S>, queryFunction: Function<FluentQuery.FetchableFluentQuery<S>, R>): R = throw NotImplementedError()
}

class FakeAssetRepository : AssetRepository {
    val store = mutableMapOf<UUID, Asset>()

    fun saveAsset(asset: Asset): Asset { store[asset.id] = asset; return asset }

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

class FakeUserRepositoryGift : UserRepository {
    val store = mutableMapOf<UUID, User>()

    fun saveUser(user: User): User { store[user.id] = user; return user }

    override fun findByHandle(handle: String): User? = store.values.firstOrNull { it.handle == handle }
    override fun findByGoogleSubject(googleSubject: String): User? = store.values.firstOrNull { it.googleSubject == googleSubject }
    override fun findByEmail(email: String): User? = store.values.firstOrNull { it.email == email }

    override fun <S : User> save(entity: S): S { store[entity.id] = entity; return entity }
    override fun findById(id: UUID): Optional<User> = Optional.ofNullable(store[id])
    override fun findAll(): MutableList<User> = store.values.toMutableList()
    override fun count(): Long = store.size.toLong()
    override fun existsById(id: UUID): Boolean = store.containsKey(id)
    override fun deleteById(id: UUID) { store.remove(id) }
    override fun delete(entity: User) { store.remove(entity.id) }
    override fun deleteAll() { store.clear() }
    override fun deleteAll(entities: MutableIterable<User>) { entities.forEach { delete(it) } }
    override fun deleteAllById(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun <S : User> saveAll(entities: MutableIterable<S>): MutableList<S> = entities.map { save(it) }.toMutableList()
    override fun findAllById(ids: MutableIterable<UUID>): MutableList<User> = ids.mapNotNull { store[it] }.toMutableList()
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
