package com.plshare.backend.global.seed

import com.plshare.backend.domain.asset.model.Asset
import com.plshare.backend.domain.asset.model.Track
import com.plshare.backend.domain.asset.repository.AssetRepository
import com.plshare.backend.domain.gift.model.Gift
import com.plshare.backend.domain.gift.repository.GiftRepository
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
    private val giftRepository: GiftRepository,
) : CommandLineRunner {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun run(vararg args: String?) {
        val demoUser = seedDemoUser()

        if (assetRepository.count() > 0L) {
            log.info("Skipping demo seed: assets already present")
            seedDemoPosts(demoUser, existingAssetId = null)
            seedDemoGift(demoUser, existingAssetId = null)
            return
        }
        val asset = Asset(
            ownerId = demoUser.id,
            title = "Late Night Drives",
            coverUrl = "https://picsum.photos/seed/latenight/600/600",
            description = "늦은 밤 드라이브에 어울리는 곡들",
            diaryText = "창문을 조금 내리고, 가로등이 흐르는 길을 달렸다. 그날의 공기가 이 노래들에 담겨 있다.",
            sourcePlatform = "spotify",
            emotionTags = mutableListOf("nocturne", "yearning", "calm")
        )
        // 데모 인라인 재생을 위해 검증된 실 YouTube id 가 매핑된 트랙으로 시딩한다
        // (MockYouTubeClient.realDemoVideoIds). 미매핑 곡은 synthetic id 로 떨어져 임베드가
        // 깨지므로, 시드 선물은 반드시 매핑된 타이틀만 사용한다.
        val seedTracks = listOf(
            Triple("The Weeknd", "Blinding Lights", 200000),
            Triple("Daft Punk", "Something About Us", 232000),
            Triple("M83", "Midnight City", 244000),
            Triple("Tame Impala", "The Less I Know the Better", 216000),
            Triple("Phoenix", "1901", 193000),
            Triple("LCD Soundsystem", "All My Friends", 311000)
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
        seedDemoGift(demoUser, existingAssetId = asset.id)
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
            text = "늦은 밤 드라이브할 때 듣기 좋은 플레이리스트 공유해요. 창문 내리고 달리면 딱이에요 🌙",
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

    /**
     * demo 선물 1건을 idempotent 시딩.
     * demo 유저가 자신의 첫 번째 자산에 감성 메시지를 붙여 선물로 포장.
     * 토큰 "demo0000000000001"로 고정해 재시작 시 중복 생성을 방지한다.
     */
    private fun seedDemoGift(demoUser: User, existingAssetId: java.util.UUID?) {
        val demoToken = "demo0000000001"
        if (giftRepository.findByToken(demoToken) != null) {
            log.info("Skipping demo gift seed: already present")
            return
        }
        val assetId = existingAssetId ?: assetRepository.findAll().firstOrNull()?.id ?: run {
            log.info("Skipping demo gift seed: no asset available")
            return
        }
        val gift = Gift(
            senderId = demoUser.id,
            assetId = assetId,
            message = "늦은 밤, 창문 내리고 이 노래들 들으면서 달려봐. 네 생각하면서 골랐어. 오늘 하루도 수고했어 🌙",
            wrapSkin = "nocturne-violet",
            token = demoToken,
        )
        giftRepository.save(gift)
        log.info("Seeded demo gift: token={}", demoToken)
    }
}
