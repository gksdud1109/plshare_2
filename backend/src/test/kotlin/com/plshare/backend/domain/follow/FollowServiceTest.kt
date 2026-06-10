package com.plshare.backend.domain.follow

import com.plshare.backend.domain.follow.model.Follow
import com.plshare.backend.domain.follow.repository.FollowRepository
import com.plshare.backend.domain.follow.service.FollowService
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
 * FollowService 단위 테스트 — fake repository(no Spring context, no DB).
 *
 * 핵심 불변식:
 * (b) 자기 자신 follow 거부 — VALIDATION_FAILED.
 * (e) 팔로잉 피드가 팔로우한 작성자의 것만 반환 (FollowRepository.findAllByFollowerId 계약).
 */
class FollowServiceTest {

    private lateinit var followRepo: FakeFollowRepository
    private lateinit var userRepo: FakeUserRepository
    private lateinit var service: FollowService

    private lateinit var alice: User
    private lateinit var bob: User
    private lateinit var carol: User

    @BeforeEach
    fun setUp() {
        followRepo = FakeFollowRepository()
        userRepo = FakeUserRepository()
        service = FollowService(followRepo, userRepo)

        alice = userRepo.saveUser(User(displayName = "Alice", handle = "alice"))
        bob   = userRepo.saveUser(User(displayName = "Bob",   handle = "bob"))
        carol = userRepo.saveUser(User(displayName = "Carol", handle = "carol"))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // (b) 자기 자신 follow 거부
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `자기 자신 follow 시도 - VALIDATION_FAILED`() {
        val ex = assertThrows(ApiException::class.java) {
            service.follow(alice.id, alice.handle)
        }
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.code)
    }

    @Test
    fun `다른 유저 follow 성공`() {
        service.follow(alice.id, bob.handle)
        assertTrue(followRepo.existsByFollowerIdAndFolloweeId(alice.id, bob.id))
    }

    @Test
    fun `follow 멱등 - 중복 follow 호출해도 1개만 유지`() {
        service.follow(alice.id, bob.handle)
        service.follow(alice.id, bob.handle) // duplicate
        assertEquals(1, followRepo.store.values.count { it.followerId == alice.id && it.followeeId == bob.id })
    }

    @Test
    fun `unfollow - follow 관계 제거`() {
        service.follow(alice.id, bob.handle)
        service.unfollow(alice.id, bob.handle)
        assertFalse(followRepo.existsByFollowerIdAndFolloweeId(alice.id, bob.id))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // (e) 팔로잉 피드: 팔로우한 작성자 followeeId 목록 계약
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `팔로잉 피드 - findAllByFollowerId 반환 목록이 팔로우한 followeeId만 포함`() {
        service.follow(alice.id, bob.handle)
        service.follow(alice.id, carol.handle)

        val followingIds = followRepo.findAllByFollowerId(alice.id).map { it.followeeId }.toSet()

        assertTrue(followingIds.contains(bob.id))
        assertTrue(followingIds.contains(carol.id))
        assertFalse(followingIds.contains(alice.id)) // 자기 자신 제외
        assertEquals(2, followingIds.size)
    }

    @Test
    fun `팔로잉 없는 유저 - findAllByFollowerId 빈 목록`() {
        val followingIds = followRepo.findAllByFollowerId(alice.id)
        assertTrue(followingIds.isEmpty())
    }

    @Test
    fun `존재하지 않는 handle follow - NOT_FOUND`() {
        val ex = assertThrows(ApiException::class.java) {
            service.follow(alice.id, "nonexistent")
        }
        assertEquals(ErrorCode.NOT_FOUND, ex.code)
    }

    @Test
    fun `follow-stats - follower and following count plus isFollowing`() {
        service.follow(alice.id, bob.handle)
        service.follow(carol.id, bob.handle)

        val stats = service.stats(bob.handle, viewerId = alice.id)

        assertEquals(2L, stats.followerCount)
        assertEquals(0L, stats.followingCount)
        assertTrue(stats.isFollowing)
    }

    @Test
    fun `follow-stats - viewerId null이면 isFollowing false`() {
        service.follow(alice.id, bob.handle)

        val stats = service.stats(bob.handle, viewerId = null)

        assertFalse(stats.isFollowing)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fake repositories
// ─────────────────────────────────────────────────────────────────────────────

class FakeFollowRepository : FollowRepository {
    val store = mutableMapOf<UUID, Follow>()

    override fun existsByFollowerIdAndFolloweeId(followerId: UUID, followeeId: UUID): Boolean =
        store.values.any { it.followerId == followerId && it.followeeId == followeeId }

    override fun deleteByFollowerIdAndFolloweeId(followerId: UUID, followeeId: UUID): Long {
        val key = store.entries.firstOrNull { it.value.followerId == followerId && it.value.followeeId == followeeId }?.key
        return if (key != null) { store.remove(key); 1L } else 0L
    }

    override fun countByFollowerId(followerId: UUID): Long =
        store.values.count { it.followerId == followerId }.toLong()

    override fun countByFolloweeId(followeeId: UUID): Long =
        store.values.count { it.followeeId == followeeId }.toLong()

    override fun findAllByFollowerId(followerId: UUID): List<Follow> =
        store.values.filter { it.followerId == followerId }

    override fun <S : Follow> save(entity: S): S { store[entity.id] = entity; return entity }
    override fun findById(id: UUID): Optional<Follow> = Optional.ofNullable(store[id])
    override fun findAll(): MutableList<Follow> = store.values.toMutableList()
    override fun count(): Long = store.size.toLong()
    override fun existsById(id: UUID): Boolean = store.containsKey(id)
    override fun deleteById(id: UUID) { store.remove(id) }
    override fun delete(entity: Follow) { store.remove(entity.id) }
    override fun deleteAll() { store.clear() }
    override fun deleteAll(entities: MutableIterable<Follow>) { entities.forEach { delete(it) } }
    override fun deleteAllById(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun <S : Follow> saveAll(entities: MutableIterable<S>): MutableList<S> = entities.map { save(it) }.toMutableList()
    override fun findAllById(ids: MutableIterable<UUID>): MutableList<Follow> = ids.mapNotNull { store[it] }.toMutableList()
    override fun findAll(sort: Sort): MutableList<Follow> = findAll()
    override fun findAll(pageable: Pageable): Page<Follow> = throw NotImplementedError()
    override fun flush() {}
    override fun <S : Follow> saveAndFlush(entity: S): S = save(entity)
    override fun <S : Follow> saveAllAndFlush(entities: MutableIterable<S>): MutableList<S> = saveAll(entities)
    override fun deleteAllInBatch() { store.clear() }
    override fun deleteAllInBatch(entities: MutableIterable<Follow>) { entities.forEach { delete(it) } }
    override fun deleteAllByIdInBatch(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun getOne(id: UUID): Follow = store[id] ?: throw NoSuchElementException()
    override fun getById(id: UUID): Follow = getOne(id)
    override fun getReferenceById(id: UUID): Follow = getOne(id)
    override fun <S : Follow> findOne(example: Example<S>): Optional<S> = throw NotImplementedError()
    override fun <S : Follow> findAll(example: Example<S>): MutableList<S> = throw NotImplementedError()
    override fun <S : Follow> findAll(example: Example<S>, sort: Sort): MutableList<S> = throw NotImplementedError()
    override fun <S : Follow> findAll(example: Example<S>, pageable: Pageable): Page<S> = throw NotImplementedError()
    override fun <S : Follow> count(example: Example<S>): Long = throw NotImplementedError()
    override fun <S : Follow> exists(example: Example<S>): Boolean = throw NotImplementedError()
    override fun <S : Follow, R : Any> findBy(example: Example<S>, queryFunction: Function<FluentQuery.FetchableFluentQuery<S>, R>): R = throw NotImplementedError()
}

class FakeUserRepository : UserRepository {
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
