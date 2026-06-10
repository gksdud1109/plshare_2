package com.plshare.backend.global.seed

import com.plshare.backend.domain.asset.model.Asset
import com.plshare.backend.domain.asset.model.Track
import com.plshare.backend.domain.asset.repository.AssetRepository
import com.plshare.backend.domain.post.model.Post
import com.plshare.backend.domain.post.repository.PostRepository
import com.plshare.backend.domain.user.model.User
import com.plshare.backend.domain.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Profile("demo")
class DemoDataSeeder(
    private val assetRepository: AssetRepository,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
) : CommandLineRunner {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun run(vararg args: String?) {
        val demoUser = seedDemoUser()

        if (assetRepository.count() > 0L) {
            log.info("Skipping demo seed: assets already present")
            seedDemoPosts(demoUser, existingAssetId = null)
            return
        }
        val asset = Asset(
            title = "Sample - Sunset Walks",
            coverUrl = "https://picsum.photos/seed/sunset/600/600",
            description = "A small playlist seeded for demo purposes",
            diaryText = "오늘은 노을이 유난히 짙었다. 골목길을 따라 천천히 걸으며 들었던 곡들.",
            sourcePlatform = "spotify",
            emotionTags = mutableListOf("calm", "warm", "nostalgia")
        )
        val seedTracks = listOf(
            Triple("Beach House", "Space Song", 320000),
            Triple("Mac DeMarco", "Chamber of Reflection", 268000),
            Triple("Cigarettes After Sex", "Apocalypse", 290000),
            Triple("Beabadoobee", "Coffee", 117000)
        )
        seedTracks.forEachIndexed { idx, (artist, title, duration) ->
            asset.tracks.add(
                Track(
                    asset = asset,
                    name = title,
                    artist = artist,
                    durationMs = duration,
                    isrc = "SEED${"%04d".format(idx)}",
                    spotifyId = "seed-spotify-$idx"
                )
            )
        }
        assetRepository.save(asset)
        log.info("Seeded demo asset: {}", asset.id)

        seedDemoPosts(demoUser, existingAssetId = asset.id)
    }

    private fun seedDemoUser(): User {
        val existing = userRepository.findByGoogleSubject("google-demo-sub-001")
        if (existing != null) {
            log.info("Skipping demo user seed: already present")
            return existing
        }
        val user = User(
            email = "demo@plshare.app",
            displayName = "Demo User",
            handle = "demo",
            avatarUrl = "https://picsum.photos/seed/demouser/200/200",
            googleSubject = "google-demo-sub-001"
        )
        userRepository.save(user)
        log.info("Seeded demo user: {}", user.id)
        return user
    }

    /**
     * demo 유저의 포스트 2개를 idempotent 시딩.
     * - 포스트 1: assetId 첨부 + 무드태그
     * - 포스트 2: 텍스트 전용
     *
     * 이미 존재하면 skip (postRepository.count > 0 기준).
     */
    private fun seedDemoPosts(demoUser: User, existingAssetId: java.util.UUID?) {
        if (postRepository.count() > 0L) {
            log.info("Skipping demo posts seed: posts already present")
            return
        }
        val assetId = existingAssetId ?: assetRepository.findAll().firstOrNull()?.id

        val post1 = Post(
            authorId = demoUser.id,
            text = "노을 질 때 듣기 좋은 플레이리스트 공유해요. 골목길 산책하면서 들으면 딱이에요 🌅",
            assetId = assetId,
            moodTag = "calm",
        )
        val post2 = Post(
            authorId = demoUser.id,
            text = "오늘 처음으로 plshare 써봤는데 좋네요. 여러분의 플레이리스트도 공유해주세요!",
        )
        postRepository.save(post1)
        postRepository.save(post2)
        log.info("Seeded 2 demo posts for user: {}", demoUser.id)
    }
}
