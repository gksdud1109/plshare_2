package com.plshare.backend.domain.post

import com.plshare.backend.domain.asset.repository.AssetRepository
import com.plshare.backend.domain.comment.repository.CommentRepository
import com.plshare.backend.domain.follow.model.Follow
import com.plshare.backend.domain.follow.repository.FollowRepository
import com.plshare.backend.domain.post.dto.CreatePostRequest
import com.plshare.backend.domain.post.model.Post
import com.plshare.backend.domain.post.repository.PostRepository
import com.plshare.backend.domain.post.service.PostService
import com.plshare.backend.domain.reaction.repository.PostLikeRepository
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
import java.util.*
import java.util.function.Function

/**
 * PostService 단위 테스트 — fake repository(no Spring context, no DB).
 *
 * 핵심 불변식:
 * (c) text 길이 검증: 500자 초과 시 VALIDATION_FAILED.
 * (d) 타인 포스트 삭제 시 FORBIDDEN.
 * (e) 팔로잉 피드가 팔로우한 작성자 것만 반환.
 */
class PostServiceTest {

    private lateinit var postRepo: FakePostRepo
    private lateinit var userRepo: FakeUserRepo
    private lateinit var assetRepo: FakeAssetRepo
    private lateinit var likeRepo: FakeLikeRepo
    private lateinit var commentRepo: FakeCommentRepo
    private lateinit var followRepo: FakeFollowRepo
    private lateinit var service: PostService

    private lateinit var alice: User
    private lateinit var bob: User

    @BeforeEach
    fun setUp() {
        postRepo    = FakePostRepo()
        userRepo    = FakeUserRepo()
        assetRepo   = FakeAssetRepo()
        likeRepo    = FakeLikeRepo()
        commentRepo = FakeCommentRepo()
        followRepo  = FakeFollowRepo()
        service = PostService(postRepo, userRepo, assetRepo, likeRepo, commentRepo, followRepo)

        alice = userRepo.saveUser(User(displayName = "Alice", handle = "alice"))
        bob   = userRepo.saveUser(User(displayName = "Bob",   handle = "bob"))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // (c) text 길이 검증
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `text 500자 이하 - 포스트 생성 성공`() {
        val req = CreatePostRequest(authorId = alice.id, text = "a".repeat(500))
        val resp = service.create(req)
        assertEquals(500, resp.text.length)
    }

    @Test
    fun `text 501자 - VALIDATION_FAILED`() {
        val req = CreatePostRequest(authorId = alice.id, text = "a".repeat(501))
        val ex = assertThrows(ApiException::class.java) { service.create(req) }
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.code)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // (d) 타인 포스트 삭제 → FORBIDDEN
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `작성자 본인이 포스트 삭제 - 성공`() {
        val post = postRepo.savePost(Post(authorId = alice.id, text = "hello"))
        service.delete(post.id, alice.id)
        assertTrue(postRepo.store[post.id]!!.deleted)
    }

    @Test
    fun `타인이 포스트 삭제 시도 - FORBIDDEN`() {
        val post = postRepo.savePost(Post(authorId = alice.id, text = "hello"))
        val ex = assertThrows(ApiException::class.java) { service.delete(post.id, bob.id) }
        assertEquals(ErrorCode.FORBIDDEN, ex.code)
    }

    @Test
    fun `삭제된 포스트 조회 - NOT_FOUND`() {
        val post = postRepo.savePost(Post(authorId = alice.id, text = "hello", deleted = true))
        val ex = assertThrows(ApiException::class.java) { service.getById(post.id, null) }
        assertEquals(ErrorCode.NOT_FOUND, ex.code)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // (e) 팔로잉 피드 — 팔로우한 작성자 포스트만 반환
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `팔로잉 피드 - 팔로우한 작성자 포스트만 반환`() {
        // alice follows bob, not carol
        val carol = userRepo.saveUser(User(displayName = "Carol", handle = "carol"))
        followRepo.saveFollow(Follow(followerId = alice.id, followeeId = bob.id))

        postRepo.savePost(Post(authorId = bob.id,   text = "bob post"))
        postRepo.savePost(Post(authorId = carol.id, text = "carol post"))

        val page = service.listFeed(alice.id, PageRequest.of(0, 20))

        assertEquals(1, page.content.size)
        assertEquals("bob post", page.content[0].text)
    }

    @Test
    fun `팔로잉 없는 유저 피드 - 빈 결과`() {
        postRepo.savePost(Post(authorId = bob.id, text = "bob post"))

        val page = service.listFeed(alice.id, PageRequest.of(0, 20))

        assertEquals(0, page.content.size)
        assertEquals(0L, page.totalElements)
    }

    @Test
    fun `존재하지 않는 author로 포스트 생성 - NOT_FOUND`() {
        val req = CreatePostRequest(authorId = UUID.randomUUID(), text = "hello")
        val ex = assertThrows(ApiException::class.java) { service.create(req) }
        assertEquals(ErrorCode.NOT_FOUND, ex.code)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fake repositories (minimal — only methods used by PostService)
// ─────────────────────────────────────────────────────────────────────────────

class FakePostRepo : PostRepository {
    val store = mutableMapOf<UUID, Post>()
    fun savePost(p: Post): Post { store[p.id] = p; return p }

    override fun findByIdAndDeletedFalse(id: UUID): Post? = store[id]?.takeIf { !it.deleted }
    override fun findAllByDeletedFalseOrderByCreatedAtDesc(pageable: Pageable): Page<Post> {
        val list = store.values.filter { !it.deleted }.sortedByDescending { it.createdAt }
        return PageImpl(list, pageable, list.size.toLong())
    }
    override fun findAllByAuthorIdInAndDeletedFalseOrderByCreatedAtDesc(authorIds: Collection<UUID>, pageable: Pageable): Page<Post> {
        val list = store.values.filter { !it.deleted && it.authorId in authorIds }.sortedByDescending { it.createdAt }
        return PageImpl(list, pageable, list.size.toLong())
    }

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

class FakeAssetRepo : AssetRepository {
    private val store = mutableMapOf<UUID, com.plshare.backend.domain.asset.model.Asset>()
    override fun findByShareToken(shareToken: String) = store.values.firstOrNull { it.shareToken == shareToken }
    override fun <S : com.plshare.backend.domain.asset.model.Asset> save(entity: S): S { store[entity.id] = entity; return entity }
    override fun findById(id: UUID): Optional<com.plshare.backend.domain.asset.model.Asset> = Optional.ofNullable(store[id])
    override fun findAll(): MutableList<com.plshare.backend.domain.asset.model.Asset> = store.values.toMutableList()
    override fun count(): Long = store.size.toLong()
    override fun existsById(id: UUID): Boolean = store.containsKey(id)
    override fun deleteById(id: UUID) { store.remove(id) }
    override fun delete(entity: com.plshare.backend.domain.asset.model.Asset) { store.remove(entity.id) }
    override fun deleteAll() { store.clear() }
    override fun deleteAll(entities: MutableIterable<com.plshare.backend.domain.asset.model.Asset>) { entities.forEach { delete(it) } }
    override fun deleteAllById(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun <S : com.plshare.backend.domain.asset.model.Asset> saveAll(entities: MutableIterable<S>): MutableList<S> = entities.map { save(it) }.toMutableList()
    override fun findAllById(ids: MutableIterable<UUID>): MutableList<com.plshare.backend.domain.asset.model.Asset> = ids.mapNotNull { store[it] }.toMutableList()
    override fun findAll(sort: Sort): MutableList<com.plshare.backend.domain.asset.model.Asset> = findAll()
    override fun findAll(pageable: Pageable): Page<com.plshare.backend.domain.asset.model.Asset> = throw NotImplementedError()
    override fun flush() {}
    override fun <S : com.plshare.backend.domain.asset.model.Asset> saveAndFlush(entity: S): S = save(entity)
    override fun <S : com.plshare.backend.domain.asset.model.Asset> saveAllAndFlush(entities: MutableIterable<S>): MutableList<S> = saveAll(entities)
    override fun deleteAllInBatch() { store.clear() }
    override fun deleteAllInBatch(entities: MutableIterable<com.plshare.backend.domain.asset.model.Asset>) { entities.forEach { delete(it) } }
    override fun deleteAllByIdInBatch(ids: MutableIterable<UUID>) { ids.forEach { deleteById(it) } }
    override fun getOne(id: UUID): com.plshare.backend.domain.asset.model.Asset = store[id] ?: throw NoSuchElementException()
    override fun getById(id: UUID): com.plshare.backend.domain.asset.model.Asset = getOne(id)
    override fun getReferenceById(id: UUID): com.plshare.backend.domain.asset.model.Asset = getOne(id)
    override fun <S : com.plshare.backend.domain.asset.model.Asset> findOne(example: Example<S>): Optional<S> = throw NotImplementedError()
    override fun <S : com.plshare.backend.domain.asset.model.Asset> findAll(example: Example<S>): MutableList<S> = throw NotImplementedError()
    override fun <S : com.plshare.backend.domain.asset.model.Asset> findAll(example: Example<S>, sort: Sort): MutableList<S> = throw NotImplementedError()
    override fun <S : com.plshare.backend.domain.asset.model.Asset> findAll(example: Example<S>, pageable: Pageable): Page<S> = throw NotImplementedError()
    override fun <S : com.plshare.backend.domain.asset.model.Asset> count(example: Example<S>): Long = throw NotImplementedError()
    override fun <S : com.plshare.backend.domain.asset.model.Asset> exists(example: Example<S>): Boolean = throw NotImplementedError()
    override fun <S : com.plshare.backend.domain.asset.model.Asset, R : Any> findBy(example: Example<S>, queryFunction: Function<FluentQuery.FetchableFluentQuery<S>, R>): R = throw NotImplementedError()
}

class FakeLikeRepo : com.plshare.backend.domain.reaction.repository.PostLikeRepository {
    override fun existsByPostIdAndUserId(postId: UUID, userId: UUID) = false
    override fun countByPostId(postId: UUID) = 0L
    override fun deleteByPostIdAndUserId(postId: UUID, userId: UUID) = 0L
    override fun findByPostIdAndUserId(postId: UUID, userId: UUID) = null
    override fun <S : com.plshare.backend.domain.reaction.model.PostLike> save(entity: S): S = entity
    override fun findById(id: UUID): Optional<com.plshare.backend.domain.reaction.model.PostLike> = Optional.empty()
    override fun findAll(): MutableList<com.plshare.backend.domain.reaction.model.PostLike> = mutableListOf()
    override fun count() = 0L
    override fun existsById(id: UUID) = false
    override fun deleteById(id: UUID) {}
    override fun delete(entity: com.plshare.backend.domain.reaction.model.PostLike) {}
    override fun deleteAll() {}
    override fun deleteAll(entities: MutableIterable<com.plshare.backend.domain.reaction.model.PostLike>) {}
    override fun deleteAllById(ids: MutableIterable<UUID>) {}
    override fun <S : com.plshare.backend.domain.reaction.model.PostLike> saveAll(entities: MutableIterable<S>): MutableList<S> = mutableListOf()
    override fun findAllById(ids: MutableIterable<UUID>): MutableList<com.plshare.backend.domain.reaction.model.PostLike> = mutableListOf()
    override fun findAll(sort: Sort): MutableList<com.plshare.backend.domain.reaction.model.PostLike> = mutableListOf()
    override fun findAll(pageable: Pageable): Page<com.plshare.backend.domain.reaction.model.PostLike> = throw NotImplementedError()
    override fun flush() {}
    override fun <S : com.plshare.backend.domain.reaction.model.PostLike> saveAndFlush(entity: S): S = entity
    override fun <S : com.plshare.backend.domain.reaction.model.PostLike> saveAllAndFlush(entities: MutableIterable<S>): MutableList<S> = mutableListOf()
    override fun deleteAllInBatch() {}
    override fun deleteAllInBatch(entities: MutableIterable<com.plshare.backend.domain.reaction.model.PostLike>) {}
    override fun deleteAllByIdInBatch(ids: MutableIterable<UUID>) {}
    override fun getOne(id: UUID): com.plshare.backend.domain.reaction.model.PostLike = throw NoSuchElementException()
    override fun getById(id: UUID): com.plshare.backend.domain.reaction.model.PostLike = throw NoSuchElementException()
    override fun getReferenceById(id: UUID): com.plshare.backend.domain.reaction.model.PostLike = throw NoSuchElementException()
    override fun <S : com.plshare.backend.domain.reaction.model.PostLike> findOne(example: Example<S>): Optional<S> = throw NotImplementedError()
    override fun <S : com.plshare.backend.domain.reaction.model.PostLike> findAll(example: Example<S>): MutableList<S> = throw NotImplementedError()
    override fun <S : com.plshare.backend.domain.reaction.model.PostLike> findAll(example: Example<S>, sort: Sort): MutableList<S> = throw NotImplementedError()
    override fun <S : com.plshare.backend.domain.reaction.model.PostLike> findAll(example: Example<S>, pageable: Pageable): Page<S> = throw NotImplementedError()
    override fun <S : com.plshare.backend.domain.reaction.model.PostLike> count(example: Example<S>): Long = throw NotImplementedError()
    override fun <S : com.plshare.backend.domain.reaction.model.PostLike> exists(example: Example<S>): Boolean = throw NotImplementedError()
    override fun <S : com.plshare.backend.domain.reaction.model.PostLike, R : Any> findBy(example: Example<S>, queryFunction: Function<FluentQuery.FetchableFluentQuery<S>, R>): R = throw NotImplementedError()
}

class FakeCommentRepo : CommentRepository {
    override fun findAllByPostIdAndDeletedFalseOrderByCreatedAtAsc(postId: UUID, pageable: Pageable): Page<com.plshare.backend.domain.comment.model.Comment> = throw NotImplementedError()
    override fun countByPostIdAndDeletedFalse(postId: UUID) = 0L
    override fun <S : com.plshare.backend.domain.comment.model.Comment> save(entity: S): S = entity
    override fun findById(id: UUID): Optional<com.plshare.backend.domain.comment.model.Comment> = Optional.empty()
    override fun findAll(): MutableList<com.plshare.backend.domain.comment.model.Comment> = mutableListOf()
    override fun count() = 0L
    override fun existsById(id: UUID) = false
    override fun deleteById(id: UUID) {}
    override fun delete(entity: com.plshare.backend.domain.comment.model.Comment) {}
    override fun deleteAll() {}
    override fun deleteAll(entities: MutableIterable<com.plshare.backend.domain.comment.model.Comment>) {}
    override fun deleteAllById(ids: MutableIterable<UUID>) {}
    override fun <S : com.plshare.backend.domain.comment.model.Comment> saveAll(entities: MutableIterable<S>): MutableList<S> = mutableListOf()
    override fun findAllById(ids: MutableIterable<UUID>): MutableList<com.plshare.backend.domain.comment.model.Comment> = mutableListOf()
    override fun findAll(sort: Sort): MutableList<com.plshare.backend.domain.comment.model.Comment> = mutableListOf()
    override fun findAll(pageable: Pageable): Page<com.plshare.backend.domain.comment.model.Comment> = throw NotImplementedError()
    override fun flush() {}
    override fun <S : com.plshare.backend.domain.comment.model.Comment> saveAndFlush(entity: S): S = entity
    override fun <S : com.plshare.backend.domain.comment.model.Comment> saveAllAndFlush(entities: MutableIterable<S>): MutableList<S> = mutableListOf()
    override fun deleteAllInBatch() {}
    override fun deleteAllInBatch(entities: MutableIterable<com.plshare.backend.domain.comment.model.Comment>) {}
    override fun deleteAllByIdInBatch(ids: MutableIterable<UUID>) {}
    override fun getOne(id: UUID): com.plshare.backend.domain.comment.model.Comment = throw NoSuchElementException()
    override fun getById(id: UUID): com.plshare.backend.domain.comment.model.Comment = throw NoSuchElementException()
    override fun getReferenceById(id: UUID): com.plshare.backend.domain.comment.model.Comment = throw NoSuchElementException()
    override fun <S : com.plshare.backend.domain.comment.model.Comment> findOne(example: Example<S>): Optional<S> = throw NotImplementedError()
    override fun <S : com.plshare.backend.domain.comment.model.Comment> findAll(example: Example<S>): MutableList<S> = throw NotImplementedError()
    override fun <S : com.plshare.backend.domain.comment.model.Comment> findAll(example: Example<S>, sort: Sort): MutableList<S> = throw NotImplementedError()
    override fun <S : com.plshare.backend.domain.comment.model.Comment> findAll(example: Example<S>, pageable: Pageable): Page<S> = throw NotImplementedError()
    override fun <S : com.plshare.backend.domain.comment.model.Comment> count(example: Example<S>): Long = throw NotImplementedError()
    override fun <S : com.plshare.backend.domain.comment.model.Comment> exists(example: Example<S>): Boolean = throw NotImplementedError()
    override fun <S : com.plshare.backend.domain.comment.model.Comment, R : Any> findBy(example: Example<S>, queryFunction: Function<FluentQuery.FetchableFluentQuery<S>, R>): R = throw NotImplementedError()
}

class FakeFollowRepo : FollowRepository {
    val store = mutableMapOf<UUID, Follow>()
    fun saveFollow(f: Follow): Follow { store[f.id] = f; return f }

    override fun existsByFollowerIdAndFolloweeId(followerId: UUID, followeeId: UUID) =
        store.values.any { it.followerId == followerId && it.followeeId == followeeId }
    override fun deleteByFollowerIdAndFolloweeId(followerId: UUID, followeeId: UUID): Long {
        val key = store.entries.firstOrNull { it.value.followerId == followerId && it.value.followeeId == followeeId }?.key
        return if (key != null) { store.remove(key); 1L } else 0L
    }
    override fun countByFollowerId(followerId: UUID) = store.values.count { it.followerId == followerId }.toLong()
    override fun countByFolloweeId(followeeId: UUID) = store.values.count { it.followeeId == followeeId }.toLong()
    override fun findAllByFollowerId(followerId: UUID): List<Follow> = store.values.filter { it.followerId == followerId }

    override fun <S : Follow> save(entity: S): S { store[entity.id] = entity; return entity }
    override fun findById(id: UUID): Optional<Follow> = Optional.ofNullable(store[id])
    override fun findAll(): MutableList<Follow> = store.values.toMutableList()
    override fun count(): Long = store.size.toLong()
    override fun existsById(id: UUID) = store.containsKey(id)
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
