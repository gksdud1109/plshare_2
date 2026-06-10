package com.plshare.backend.domain.comment

import com.plshare.backend.domain.comment.dto.CreateCommentRequest
import com.plshare.backend.domain.comment.model.Comment
import com.plshare.backend.domain.comment.repository.CommentRepository
import com.plshare.backend.domain.comment.service.CommentService
import com.plshare.backend.domain.post.model.Post
import com.plshare.backend.domain.post.repository.PostRepository
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
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.query.FluentQuery
import java.util.*
import java.util.function.Function

/**
 * CommentService 단위 테스트 — fake repository(no Spring context, no DB).
 *
 * 핵심 불변식:
 * (c) text 길이 검증: 300자 초과 시 VALIDATION_FAILED.
 * (d) 타인 댓글 삭제 시 FORBIDDEN.
 */
class CommentServiceTest {

    private lateinit var commentRepo: FakeCommentRepo
    private lateinit var postRepo: FakePostRepo
    private lateinit var userRepo: FakeUserRepo
    private lateinit var service: CommentService

    private lateinit var alice: User
    private lateinit var bob: User
    private lateinit var post: Post

    @BeforeEach
    fun setUp() {
        commentRepo = FakeCommentRepo()
        postRepo    = FakePostRepo()
        userRepo    = FakeUserRepo()
        service = CommentService(commentRepo, postRepo, userRepo)

        alice = userRepo.saveUser(User(displayName = "Alice", handle = "alice"))
        bob   = userRepo.saveUser(User(displayName = "Bob",   handle = "bob"))
        post  = postRepo.savePost(Post(authorId = alice.id, text = "test post"))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // (c) text 길이 검증
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `댓글 text 300자 이하 - 생성 성공`() {
        val req = CreateCommentRequest(authorId = alice.id, text = "a".repeat(300))
        val resp = service.create(post.id, req)
        assertEquals(300, resp.text.length)
    }

    @Test
    fun `댓글 text 301자 - VALIDATION_FAILED`() {
        val req = CreateCommentRequest(authorId = alice.id, text = "a".repeat(301))
        val ex = assertThrows(ApiException::class.java) { service.create(post.id, req) }
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.code)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // (d) 타인 댓글 삭제 → FORBIDDEN
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `작성자 본인이 댓글 삭제 - soft-delete 성공`() {
        val req = CreateCommentRequest(authorId = alice.id, text = "hello")
        val resp = service.create(post.id, req)

        service.delete(resp.id, alice.id)

        val comment = commentRepo.store[resp.id]!!
        assertTrue(comment.deleted)
    }

    @Test
    fun `타인이 댓글 삭제 시도 - FORBIDDEN`() {
        val req = CreateCommentRequest(authorId = alice.id, text = "hello")
        val resp = service.create(post.id, req)

        val ex = assertThrows(ApiException::class.java) { service.delete(resp.id, bob.id) }
        assertEquals(ErrorCode.FORBIDDEN, ex.code)
    }

    @Test
    fun `삭제된 포스트에 댓글 작성 - NOT_FOUND`() {
        val deletedPost = postRepo.savePost(Post(authorId = alice.id, text = "gone", deleted = true))
        val req = CreateCommentRequest(authorId = alice.id, text = "hi")
        val ex = assertThrows(ApiException::class.java) { service.create(deletedPost.id, req) }
        assertEquals(ErrorCode.NOT_FOUND, ex.code)
    }

    @Test
    fun `이미 삭제된 댓글 재삭제 - NOT_FOUND`() {
        val req = CreateCommentRequest(authorId = alice.id, text = "hello")
        val resp = service.create(post.id, req)
        service.delete(resp.id, alice.id)

        val ex = assertThrows(ApiException::class.java) { service.delete(resp.id, alice.id) }
        assertEquals(ErrorCode.NOT_FOUND, ex.code)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fake repositories
// ─────────────────────────────────────────────────────────────────────────────

class FakeCommentRepo : CommentRepository {
    val store = mutableMapOf<UUID, Comment>()

    override fun findAllByPostIdAndDeletedFalseOrderByCreatedAtAsc(postId: UUID, pageable: Pageable): Page<Comment> {
        val list = store.values.filter { it.postId == postId && !it.deleted }.sortedBy { it.createdAt }
        return PageImpl(list, pageable, list.size.toLong())
    }

    override fun countByPostIdAndDeletedFalse(postId: UUID): Long =
        store.values.count { it.postId == postId && !it.deleted }.toLong()

    override fun <S : Comment> save(entity: S): S { store[entity.id] = entity; return entity }
    override fun findById(id: UUID): Optional<Comment> = Optional.ofNullable(store[id])
    override fun findAll(): MutableList<Comment> = store.values.toMutableList()
    override fun count(): Long = store.size.toLong()
    override fun existsById(id: UUID): Boolean = store.containsKey(id)
    override fun deleteById(id: UUID) { store.remove(id) }
    override fun delete(entity: Comment) { store.remove(entity.id) }
    override fun deleteAll() { store.clear() }
    override fun deleteAll(entities: MutableIterable<Comment>) { entities.forEach { delete(it) } }
    override fun deleteAllById(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun <S : Comment> saveAll(entities: MutableIterable<S>): MutableList<S> = entities.map { save(it) }.toMutableList()
    override fun findAllById(ids: MutableIterable<UUID>): MutableList<Comment> = ids.mapNotNull { store[it] }.toMutableList()
    override fun findAll(sort: Sort): MutableList<Comment> = findAll()
    override fun findAll(pageable: Pageable): Page<Comment> = throw NotImplementedError()
    override fun flush() {}
    override fun <S : Comment> saveAndFlush(entity: S): S = save(entity)
    override fun <S : Comment> saveAllAndFlush(entities: MutableIterable<S>): MutableList<S> = saveAll(entities)
    override fun deleteAllInBatch() { store.clear() }
    override fun deleteAllInBatch(entities: MutableIterable<Comment>) { entities.forEach { delete(it) } }
    override fun deleteAllByIdInBatch(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun getOne(id: UUID): Comment = store[id] ?: throw NoSuchElementException()
    override fun getById(id: UUID): Comment = getOne(id)
    override fun getReferenceById(id: UUID): Comment = getOne(id)
    override fun <S : Comment> findOne(example: Example<S>): Optional<S> = throw NotImplementedError()
    override fun <S : Comment> findAll(example: Example<S>): MutableList<S> = throw NotImplementedError()
    override fun <S : Comment> findAll(example: Example<S>, sort: Sort): MutableList<S> = throw NotImplementedError()
    override fun <S : Comment> findAll(example: Example<S>, pageable: Pageable): Page<S> = throw NotImplementedError()
    override fun <S : Comment> count(example: Example<S>): Long = throw NotImplementedError()
    override fun <S : Comment> exists(example: Example<S>): Boolean = throw NotImplementedError()
    override fun <S : Comment, R : Any> findBy(example: Example<S>, queryFunction: Function<FluentQuery.FetchableFluentQuery<S>, R>): R = throw NotImplementedError()
}

class FakePostRepo : PostRepository {
    val store = mutableMapOf<UUID, Post>()
    fun savePost(p: Post): Post { store[p.id] = p; return p }

    override fun findByIdAndDeletedFalse(id: UUID): Post? = store[id]?.takeIf { !it.deleted }
    override fun findAllByDeletedFalseOrderByCreatedAtDesc(pageable: Pageable): Page<Post> = throw NotImplementedError()
    override fun findAllByAuthorIdInAndDeletedFalseOrderByCreatedAtDesc(authorIds: Collection<UUID>, pageable: Pageable): Page<Post> = throw NotImplementedError()

    override fun <S : Post> save(entity: S): S { store[entity.id] = entity; return entity }
    override fun findById(id: UUID): Optional<Post> = Optional.ofNullable(store[id])
    override fun findAll(): MutableList<Post> = store.values.toMutableList()
    override fun count(): Long = store.size.toLong()
    override fun existsById(id: UUID): Boolean = store.containsKey(id)
    override fun deleteById(id: UUID) { store.remove(id) }
    override fun delete(entity: Post) { store.remove(entity.id) }
    override fun deleteAll() { store.clear() }
    override fun deleteAll(entities: MutableIterable<Post>) { entities.forEach { delete(it) } }
    override fun deleteAllById(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun <S : Post> saveAll(entities: MutableIterable<S>): MutableList<S> = entities.map { save(it) }.toMutableList()
    override fun findAllById(ids: MutableIterable<UUID>): MutableList<Post> = ids.mapNotNull { store[it] }.toMutableList()
    override fun findAll(sort: Sort): MutableList<Post> = findAll()
    override fun findAll(pageable: Pageable): Page<Post> = throw NotImplementedError()
    override fun flush() {}
    override fun <S : Post> saveAndFlush(entity: S): S = save(entity)
    override fun <S : Post> saveAllAndFlush(entities: MutableIterable<S>): MutableList<S> = saveAll(entities)
    override fun deleteAllInBatch() { store.clear() }
    override fun deleteAllInBatch(entities: MutableIterable<Post>) { entities.forEach { delete(it) } }
    override fun deleteAllByIdInBatch(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun getOne(id: UUID): Post = store[id] ?: throw NoSuchElementException()
    override fun getById(id: UUID): Post = getOne(id)
    override fun getReferenceById(id: UUID): Post = getOne(id)
    override fun <S : Post> findOne(example: Example<S>): Optional<S> = throw NotImplementedError()
    override fun <S : Post> findAll(example: Example<S>): MutableList<S> = throw NotImplementedError()
    override fun <S : Post> findAll(example: Example<S>, sort: Sort): MutableList<S> = throw NotImplementedError()
    override fun <S : Post> findAll(example: Example<S>, pageable: Pageable): Page<S> = throw NotImplementedError()
    override fun <S : Post> count(example: Example<S>): Long = throw NotImplementedError()
    override fun <S : Post> exists(example: Example<S>): Boolean = throw NotImplementedError()
    override fun <S : Post, R : Any> findBy(example: Example<S>, queryFunction: Function<FluentQuery.FetchableFluentQuery<S>, R>): R = throw NotImplementedError()
}

class FakeUserRepo : UserRepository {
    val store = mutableMapOf<UUID, User>()
    fun saveUser(u: User): User { store[u.id] = u; return u }

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
