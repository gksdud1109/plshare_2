package com.plshare.backend.domain.reaction

import com.plshare.backend.domain.post.model.Post
import com.plshare.backend.domain.post.repository.PostRepository
import com.plshare.backend.domain.reaction.model.PostLike
import com.plshare.backend.domain.reaction.repository.PostLikeRepository
import com.plshare.backend.domain.reaction.service.PostLikeService
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
 * PostLikeService 단위 테스트 — fake repository(no Spring context, no DB).
 *
 * 핵심 불변식:
 * (a) like 멱등: 동일 (postId, userId)로 중복 호출해도 PostLike 1개만 유지.
 * (d) 삭제된 포스트에 대한 like는 NOT_FOUND.
 */
class PostLikeServiceTest {

    private lateinit var postRepo: FakePostRepository
    private lateinit var likeRepo: FakePostLikeRepository
    private lateinit var service: PostLikeService

    private val userId1 = UUID.randomUUID()
    private val userId2 = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        postRepo = FakePostRepository()
        likeRepo = FakePostLikeRepository()
        service = PostLikeService(likeRepo, postRepo)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // (a) like 멱등: 중복 호출해도 1개만 유지
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `like 멱등 - 동일 userId로 중복 like 호출해도 count 1 유지`() {
        val post = postRepo.savePost(Post(authorId = userId1, text = "hello"))

        service.like(post.id, userId2)
        service.like(post.id, userId2) // duplicate call

        assertEquals(1L, likeRepo.store.values.count { it.postId == post.id }.toLong())
        assertEquals(1L, service.like(post.id, userId2)) // still 1
    }

    @Test
    fun `unlike 멱등 - like 없는 상태에서 unlike 호출해도 예외 없음`() {
        val post = postRepo.savePost(Post(authorId = userId1, text = "hello"))

        val count = service.unlike(post.id, userId2) // nothing to delete
        assertEquals(0L, count)
    }

    @Test
    fun `like 후 unlike - count 0 반환`() {
        val post = postRepo.savePost(Post(authorId = userId1, text = "hello"))

        service.like(post.id, userId2)
        val count = service.unlike(post.id, userId2)

        assertEquals(0L, count)
        assertEquals(0, likeRepo.store.values.count { it.postId == post.id })
    }

    @Test
    fun `삭제된 포스트에 like 시도 - NOT_FOUND 예외`() {
        val post = postRepo.savePost(Post(authorId = userId1, text = "hello", deleted = true))

        val ex = assertThrows(ApiException::class.java) {
            service.like(post.id, userId2)
        }
        assertEquals(ErrorCode.NOT_FOUND, ex.code)
    }

    @Test
    fun `존재하지 않는 포스트에 like 시도 - NOT_FOUND 예외`() {
        val ex = assertThrows(ApiException::class.java) {
            service.like(UUID.randomUUID(), userId2)
        }
        assertEquals(ErrorCode.NOT_FOUND, ex.code)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fake repositories
// ─────────────────────────────────────────────────────────────────────────────

class FakePostRepository : PostRepository {
    val store = mutableMapOf<UUID, Post>()

    fun savePost(post: Post): Post { store[post.id] = post; return post }

    override fun findByIdAndDeletedFalse(id: UUID): Post? = store[id]?.takeIf { !it.deleted }
    override fun findAllByDeletedFalseOrderByCreatedAtDesc(pageable: Pageable): Page<Post> = throw NotImplementedError()
    override fun findAllByAuthorIdInAndDeletedFalseOrderByCreatedAtDesc(authorIds: Collection<UUID>, pageable: Pageable): Page<Post> = throw NotImplementedError()
    override fun findAllByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(authorId: UUID, pageable: Pageable): Page<Post> = throw NotImplementedError()

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

class FakePostLikeRepository : PostLikeRepository {
    val store = mutableMapOf<UUID, PostLike>()

    override fun existsByPostIdAndUserId(postId: UUID, userId: UUID): Boolean =
        store.values.any { it.postId == postId && it.userId == userId }

    override fun countByPostId(postId: UUID): Long =
        store.values.count { it.postId == postId }.toLong()

    override fun deleteByPostIdAndUserId(postId: UUID, userId: UUID): Long {
        val key = store.entries.firstOrNull { it.value.postId == postId && it.value.userId == userId }?.key
        return if (key != null) { store.remove(key); 1L } else 0L
    }

    override fun findByPostIdAndUserId(postId: UUID, userId: UUID): PostLike? =
        store.values.firstOrNull { it.postId == postId && it.userId == userId }

    override fun <S : PostLike> save(entity: S): S { store[entity.id] = entity; return entity }
    override fun findById(id: UUID): Optional<PostLike> = Optional.ofNullable(store[id])
    override fun findAll(): MutableList<PostLike> = store.values.toMutableList()
    override fun count(): Long = store.size.toLong()
    override fun existsById(id: UUID): Boolean = store.containsKey(id)
    override fun deleteById(id: UUID) { store.remove(id) }
    override fun delete(entity: PostLike) { store.remove(entity.id) }
    override fun deleteAll() { store.clear() }
    override fun deleteAll(entities: MutableIterable<PostLike>) { entities.forEach { delete(it) } }
    override fun deleteAllById(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun <S : PostLike> saveAll(entities: MutableIterable<S>): MutableList<S> = entities.map { save(it) }.toMutableList()
    override fun findAllById(ids: MutableIterable<UUID>): MutableList<PostLike> = ids.mapNotNull { store[it] }.toMutableList()
    override fun findAll(sort: Sort): MutableList<PostLike> = findAll()
    override fun findAll(pageable: Pageable): Page<PostLike> = throw NotImplementedError()
    override fun flush() {}
    override fun <S : PostLike> saveAndFlush(entity: S): S = save(entity)
    override fun <S : PostLike> saveAllAndFlush(entities: MutableIterable<S>): MutableList<S> = saveAll(entities)
    override fun deleteAllInBatch() { store.clear() }
    override fun deleteAllInBatch(entities: MutableIterable<PostLike>) { entities.forEach { delete(it) } }
    override fun deleteAllByIdInBatch(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun getOne(id: UUID): PostLike = store[id] ?: throw NoSuchElementException()
    override fun getById(id: UUID): PostLike = getOne(id)
    override fun getReferenceById(id: UUID): PostLike = getOne(id)
    override fun <S : PostLike> findOne(example: Example<S>): Optional<S> = throw NotImplementedError()
    override fun <S : PostLike> findAll(example: Example<S>): MutableList<S> = throw NotImplementedError()
    override fun <S : PostLike> findAll(example: Example<S>, sort: Sort): MutableList<S> = throw NotImplementedError()
    override fun <S : PostLike> findAll(example: Example<S>, pageable: Pageable): Page<S> = throw NotImplementedError()
    override fun <S : PostLike> count(example: Example<S>): Long = throw NotImplementedError()
    override fun <S : PostLike> exists(example: Example<S>): Boolean = throw NotImplementedError()
    override fun <S : PostLike, R : Any> findBy(example: Example<S>, queryFunction: Function<FluentQuery.FetchableFluentQuery<S>, R>): R = throw NotImplementedError()
}
