package com.plshare.backend.global.seed

import com.plshare.backend.domain.asset.model.Asset
import com.plshare.backend.domain.asset.model.Track
import com.plshare.backend.domain.asset.repository.AssetRepository
import com.plshare.backend.domain.follow.model.Follow
import com.plshare.backend.domain.follow.repository.FollowRepository
import com.plshare.backend.domain.gift.model.Gift
import com.plshare.backend.domain.gift.model.GiftStatus
import com.plshare.backend.domain.gift.repository.GiftRepository
import com.plshare.backend.domain.post.model.Post
import com.plshare.backend.domain.post.repository.PostRepository
import com.plshare.backend.domain.reaction.model.PostLike
import com.plshare.backend.domain.reaction.repository.PostLikeRepository
import com.plshare.backend.domain.user.model.User
import com.plshare.backend.domain.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 데모 세계 시더 — "믿을 만한 작은 음악 커뮤니티"를 만든다.
 *
 * demo 유저 + 캐릭터 3명(지민/소율/현우, FE fixtures-social 과 핸들·아바타 일치)이
 * 각자 플레이리스트를 올리고 서로 팔로우·좋아요·선물한다. 이로써:
 *  - /ranking 이 다양한 점수의 플레이리스트·사용자로 채워지고(점수 = 자산첨부 포스트 좋아요 + 팔로워),
 *  - demo 가 팔로우/언팔로우할 대상과 받은/보낸 선물을 즉시 체험하며,
 *  - 모든 트랙은 realDemoVideoIds 매핑 곡이라 인라인 재생된다.
 *
 * 멱등: 전체 시드를 "jimin 존재 여부" 하나로 가드한다(데모 유저는 googleSubject 로 별도 가드).
 */
@Component
@Profile("demo")
class DemoDataSeeder(
    private val assetRepository: AssetRepository,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val giftRepository: GiftRepository,
    private val followRepository: FollowRepository,
    private val postLikeRepository: PostLikeRepository,
) : CommandLineRunner {
    private val log = LoggerFactory.getLogger(this::class.java)

    /** realDemoVideoIds 에 매핑돼 실제 재생되는 곡 풀(MockYouTubeClient 와 제목 일치 필수). */
    private data class TrackSeed(val artist: String, val title: String, val durationMs: Int)
    private val playable = listOf(
        TrackSeed("The Weeknd", "Blinding Lights", 200000),
        TrackSeed("Daft Punk", "Something About Us", 232000),
        TrackSeed("M83", "Midnight City", 244000),
        TrackSeed("Tame Impala", "The Less I Know the Better", 216000),
        TrackSeed("Phoenix", "1901", 193000),
        TrackSeed("LCD Soundsystem", "All My Friends", 311000),
    )

    @Transactional
    override fun run(vararg args: String?) {
        val demo = seedUser("demo", "Demo User", "demo@plshare.app", "demouser", "google-demo-sub-001")

        if (userRepository.findByHandle("jimin") != null) {
            log.info("Demo world already seeded — skip")
            return
        }
        log.info("Seeding demo world…")

        // 데모 유저 자신의 플레이리스트(고정 자산) — 시드 선물 demo0000000001 의 자산.
        val demoAsset = buildAsset(
            demo, "Late Night Drives",
            "https://picsum.photos/seed/latenight/600/600",
            "늦은 밤 드라이브에 어울리는 곡들",
            "창문을 조금 내리고, 가로등이 흐르는 길을 달렸다.",
            listOf("nocturne", "yearning", "calm"),
            listOf(0, 1, 2, 3, 4, 5),
        )

        // 캐릭터 3명.
        val jimin = seedUser("jimin", "지민", "jimin@plshare.app", "jimin", "google-seed-jimin")
        val soyul = seedUser("soyul", "소율", "soyul@plshare.app", "soyul", "google-seed-soyul")
        val hyunwoo = seedUser("hyunwoo", "현우", "hyunwoo@plshare.app", null, "google-seed-hyunwoo")

        // 캐릭터별 자산 ≥3개(재생 가능 트랙으로).
        val jiminA = buildAsset(jimin, "심야 운전", "https://picsum.photos/seed/jimin1/600/600", "혼자 달리는 새벽 도로", "헤드라이트만 보고 달리던 밤.", listOf("nocturne", "free"), listOf(0, 2, 5))
        buildAsset(jimin, "비의 기억", "https://picsum.photos/seed/jimin2/600/600", "비 오는 날의 무드", "창에 맺힌 빗방울처럼.", listOf("rainy", "calm"), listOf(3, 1))
        buildAsset(jimin, "여름의 끝", "https://picsum.photos/seed/jimin3/600/600", "끝나가는 계절", "햇살이 길어지던 그 무렵.", listOf("warm", "nostalgia"), listOf(4, 5, 0))

        val soyulA = buildAsset(soyul, "새벽 감성", "https://picsum.photos/seed/soyul1/600/600", "잠들기 전 듣는 곡", "이불 속에서 듣던 노래들.", listOf("dreamy", "calm"), listOf(1, 3))
        buildAsset(soyul, "혼자 듣는 밤", "https://picsum.photos/seed/soyul2/600/600", "나만의 시간", "아무에게도 방해받지 않는 밤.", listOf("nocturne", "intro"), listOf(5, 2, 4))
        buildAsset(soyul, "출근길 부스터", "https://picsum.photos/seed/soyul3/600/600", "아침을 깨우는", "지하철에서 텐션 올리던 곡.", listOf("energetic", "morning"), listOf(0, 4))

        val hyunwooA = buildAsset(hyunwoo, "몽환 신스", "https://picsum.photos/seed/hyunwoo1/600/600", "신스웨이브 무드", "네온이 흐르는 도시의 밤.", listOf("synth", "dreamy"), listOf(2, 3))
        buildAsset(hyunwoo, "주말 아침", "https://picsum.photos/seed/hyunwoo2/600/600", "느긋한 휴일", "커피 한 잔과 함께.", listOf("warm", "lazy"), listOf(1, 4))
        buildAsset(hyunwoo, "밤 산책", "https://picsum.photos/seed/hyunwoo3/600/600", "걷기 좋은 밤", "동네 한 바퀴.", listOf("calm", "walk"), listOf(5, 0, 2))

        // 포스트(자산 첨부 → 랭킹 점수 발생 + 피드 콘텐츠).
        val demoPost = post(demo, "늦은 밤 드라이브할 때 듣기 좋은 플레이리스트 공유해요. 창문 내리고 달리면 딱이에요 🌙", demoAsset.id, "nocturne")
        post(demo, "오늘 처음으로 plshare 써봤는데 좋네요. 여러분의 플레이리스트도 공유해주세요!", null, null)
        val jiminPost = post(jimin, "새벽 도로에서만 듣는 플레이리스트. 헤드라이트랑 이 노래들이면 충분해요.", jiminA.id, "nocturne")
        val soyulPost = post(soyul, "잠들기 전에 듣는 곡들 모아봤어요. 오늘 하루도 수고했어요 🌙", soyulA.id, "calm")
        val hyunwooPost = post(hyunwoo, "신스웨이브 좋아하는 분? 네온 도시 감성 가득 담았어요.", hyunwooA.id, "synth")

        // 소셜 그래프 — 팔로우(비대칭): demo 가 팔로우할 대상 + demo 의 팔로워.
        follow(demo, jimin); follow(demo, soyul)          // demo follows 2
        follow(jimin, demo); follow(soyul, demo); follow(hyunwoo, demo) // demo 팔로워 3
        follow(jimin, soyul); follow(soyul, jimin); follow(hyunwoo, jimin)

        // 좋아요(비대칭 → 랭킹 점수 차등). jimin 글이 가장 인기.
        like(jiminPost, soyul); like(jiminPost, hyunwoo); like(jiminPost, demo)   // 3
        like(soyulPost, jimin); like(soyulPost, demo)                            // 2
        like(demoPost, jimin); like(demoPost, soyul)                             // 2
        like(hyunwooPost, jimin)                                                 // 1

        // 선물 — demo 의 받은(저장된) 선물 + 보낸 선물 + 시드 선물.
        // (1) 데모가 처음 열어볼 시드 선물(링크 기반, demo0000000001).
        giftRepository.save(
            Gift(
                senderId = demo.id, assetId = demoAsset.id,
                message = "늦은 밤, 창문 내리고 이 노래들 들으면서 달려봐. 네 생각하면서 골랐어. 오늘 하루도 수고했어 🌙",
                wrapSkin = "nocturne-violet", token = "demo0000000001",
            )
        )
        // (2) 소율 → demo, demo 가 이미 저장함(받은 선물함에 노출).
        val received = Gift(
            senderId = soyul.id, assetId = soyulA.id,
            message = "요즘 잠 잘 못 잔다며. 이 곡들 들으면서 천천히 눈 감아봐. 늘 응원해. — 소율",
            wrapSkin = "nocturne-rose", token = "demoseedrecv01",
        )
        received.status = GiftStatus.SAVED
        received.savedByUserId = demo.id
        received.openedAt = LocalDateTime.now()
        giftRepository.save(received)
        // (3) demo → 현우, 보낸 선물.
        giftRepository.save(
            Gift(
                senderId = demo.id, assetId = demoAsset.id,
                message = "지난번에 드라이브 얘기했잖아. 그때 들었던 곡들이야. 또 같이 달리자.",
                wrapSkin = "nocturne-teal", token = "demoseedsent01",
            )
        )

        log.info("Demo world seeded: 4 users, {} assets, posts/follows/likes/gifts", assetRepository.count())
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private fun seedUser(key: String, displayName: String, email: String, avatarSeed: String?, googleSubject: String): User {
        userRepository.findByGoogleSubject(googleSubject)?.let { return it }
        val user = userRepository.save(
            User(
                email = email,
                displayName = displayName,
                handle = key,
                avatarUrl = avatarSeed?.let { "https://picsum.photos/seed/$it/200/200" },
                googleSubject = googleSubject,
            )
        )
        log.info("Seeded user: {} ({})", key, user.id)
        return user
    }

    private fun buildAsset(
        owner: User, title: String, coverUrl: String, description: String,
        diaryText: String, tags: List<String>, trackIdx: List<Int>,
    ): Asset {
        val asset = Asset(
            ownerId = owner.id, title = title, coverUrl = coverUrl,
            description = description, diaryText = diaryText, sourcePlatform = "spotify",
            emotionTags = tags.toMutableList(),
        )
        trackIdx.forEachIndexed { i, idx ->
            val t = playable[idx]
            asset.tracks.add(
                Track(asset = asset, name = t.title, artist = t.artist, durationMs = t.durationMs, spotifyId = "seed-${owner.handle}-${title.hashCode()}-$i")
            )
        }
        return assetRepository.save(asset)
    }

    private fun post(author: User, text: String, assetId: java.util.UUID?, moodTag: String?): Post =
        postRepository.save(Post(authorId = author.id, text = text, assetId = assetId, moodTag = moodTag))

    private fun follow(follower: User, followee: User) {
        if (!followRepository.existsByFollowerIdAndFolloweeId(follower.id, followee.id)) {
            followRepository.save(Follow(followerId = follower.id, followeeId = followee.id))
        }
    }

    private fun like(post: Post, user: User) {
        if (!postLikeRepository.existsByPostIdAndUserId(post.id, user.id)) {
            postLikeRepository.save(PostLike(postId = post.id, userId = user.id))
        }
    }
}
