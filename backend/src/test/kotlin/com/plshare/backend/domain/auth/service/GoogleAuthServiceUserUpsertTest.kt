package com.plshare.backend.domain.auth.service

import com.plshare.backend.domain.user.model.User
import com.plshare.backend.domain.user.repository.UserRepository
import com.plshare.backend.infrastructure.google.GoogleUserInfo
import org.junit.jupiter.api.Assertions.*
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
 * GoogleAuthService unit tests — fake repository (no Spring context, no DB).
 *
 * Key invariant under test: a given googleSubject must map to exactly ONE User row.
 * Re-login with the same sub must update (not duplicate) the existing record.
 */
class GoogleAuthServiceUserUpsertTest {

    private lateinit var repo: FakeUserRepository
    private lateinit var service: GoogleAuthService

    @BeforeEach
    fun setUp() {
        repo = FakeUserRepository()
        service = GoogleAuthService(repo)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Case 1: first login → creates a new User
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `첫 로그인 - googleSubject 로 새 User 생성`() {
        val info = GoogleUserInfo(
            sub = "sub-001",
            email = "alice@example.com",
            name = "Alice",
            picture = "https://example.com/alice.jpg"
        )

        val user = service.upsertUser(info)

        assertEquals(1, repo.store.size)
        assertEquals("sub-001", user.googleSubject)
        assertEquals("alice@example.com", user.email)
        assertEquals("Alice", user.displayName)
        assertEquals("https://example.com/alice.jpg", user.avatarUrl)
        assertNotNull(user.handle)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Case 2: re-login with same sub → NO duplicate, updates mutable fields
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `동일 googleSubject 재로그인 - 중복 생성 없이 기존 User 업데이트`() {
        val info = GoogleUserInfo(
            sub = "sub-002",
            email = "bob@example.com",
            name = "Bob",
            picture = "https://example.com/bob.jpg"
        )
        val firstLogin = service.upsertUser(info)
        val firstId = firstLogin.id

        // Second login: name and picture changed on Google's side.
        val updatedInfo = info.copy(
            name = "Robert",
            picture = "https://example.com/robert.jpg"
        )
        val secondLogin = service.upsertUser(updatedInfo)

        assertEquals(1, repo.store.size, "Must not create a second User row")
        assertEquals(firstId, secondLogin.id, "ID must remain stable across logins")
        assertEquals("Robert", secondLogin.displayName, "Display name should be updated")
        assertEquals("https://example.com/robert.jpg", secondLogin.avatarUrl, "Avatar should be updated")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Case 3: two different Google accounts → two separate User rows
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `서로 다른 googleSubject 는 각각 별도 User 생성`() {
        service.upsertUser(
            GoogleUserInfo(sub = "sub-003", email = "carol@example.com", name = "Carol", picture = null)
        )
        service.upsertUser(
            GoogleUserInfo(sub = "sub-004", email = "dave@example.com", name = "Dave", picture = null)
        )

        assertEquals(2, repo.store.size)
        val subs = repo.store.values.map { it.googleSubject }.toSet()
        assertTrue(subs.contains("sub-003"))
        assertTrue(subs.contains("sub-004"))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Case 4: user with no email → handle derived from display name
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `email 없는 Google 계정 - name 으로 handle 생성`() {
        val info = GoogleUserInfo(
            sub = "sub-005",
            email = null,
            name = "No Email User",
            picture = null
        )

        val user = service.upsertUser(info)

        assertNull(user.email)
        assertTrue(user.handle.isNotBlank(), "handle must be non-blank even without email")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Case 5: handle collision → second user gets a unique handle with suffix
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `handle 충돌 - 두 번째 사용자는 suffix 붙은 고유 handle 생성`() {
        // Both users have the same email local-part base, simulated by pre-seeding a handle.
        service.upsertUser(
            GoogleUserInfo(sub = "sub-006a", email = "shared@example.com", name = "Shared", picture = null)
        )
        // Manually set the handle to "shared" to force collision for the next user.
        val first = repo.store.values.first { it.googleSubject == "sub-006a" }
        repo.store[first.id] = User(
            id = first.id,
            email = first.email,
            displayName = first.displayName,
            handle = "shared",
            avatarUrl = first.avatarUrl,
            googleSubject = first.googleSubject
        )

        // Second user attempts to get "shared" handle.
        val second = service.upsertUser(
            GoogleUserInfo(sub = "sub-006b", email = "shared2@example.com", name = "Shared", picture = null)
        )

        assertNotEquals("shared", second.handle, "Second user must get a unique handle")
        assertEquals(2, repo.store.size)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// In-memory fake — only implements methods used by GoogleAuthService.
// ─────────────────────────────────────────────────────────────────────────────

class FakeUserRepository : UserRepository {
    val store = mutableMapOf<UUID, User>()

    override fun findByGoogleSubject(googleSubject: String): User? =
        store.values.firstOrNull { it.googleSubject == googleSubject }

    override fun findByHandle(handle: String): User? =
        store.values.firstOrNull { it.handle == handle }

    override fun findByEmail(email: String): User? =
        store.values.firstOrNull { it.email == email }

    override fun <S : User> save(entity: S): S {
        store[entity.id] = entity
        return entity
    }

    override fun findById(id: UUID): Optional<User> = Optional.ofNullable(store[id])
    override fun findAll(): MutableList<User> = store.values.toMutableList()
    override fun count(): Long = store.size.toLong()
    override fun existsById(id: UUID): Boolean = store.containsKey(id)
    override fun deleteById(id: UUID) { store.remove(id) }
    override fun delete(entity: User) { store.remove(entity.id) }
    override fun deleteAll() { store.clear() }
    override fun deleteAll(entities: MutableIterable<User>) { entities.forEach { delete(it) } }
    override fun deleteAllById(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun <S : User> saveAll(entities: MutableIterable<S>): MutableList<S> =
        entities.map { save(it) }.toMutableList()
    override fun findAllById(ids: MutableIterable<UUID>): MutableList<User> =
        ids.mapNotNull { store[it] }.toMutableList()
    override fun findAll(sort: Sort): MutableList<User> = findAll()
    override fun findAll(pageable: Pageable): Page<User> = throw NotImplementedError()
    override fun flush() { /* no-op */ }
    override fun <S : User> saveAndFlush(entity: S): S = save(entity)
    override fun <S : User> saveAllAndFlush(entities: MutableIterable<S>): MutableList<S> =
        saveAll(entities)
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
    override fun <S : User, R : Any> findBy(
        example: Example<S>,
        queryFunction: Function<FluentQuery.FetchableFluentQuery<S>, R>
    ): R = throw NotImplementedError()
}
