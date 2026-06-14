package com.plshare.backend.domain.catalog.seed

import com.plshare.backend.domain.catalog.model.CuratedTrack
import com.plshare.backend.domain.catalog.repository.CuratedTrackRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 큐레이션 카탈로그 시더 — 재생 검증된(oEmbed 200 + 임베드 차단 없음) YouTube videoId 가 박힌 곡 풀.
 * demo·prod 모두 시드(실 콘텐츠). 멱등: 이미 있으면 skip. 커버는 YouTube 썸네일.
 * 트랙에 videoId 가 미리 있으므로 재생 시 YouTube 검색/쿼터/키가 불필요하다.
 */
@Component
class CuratedCatalogSeeder(
    private val curatedTrackRepository: CuratedTrackRepository,
) : CommandLineRunner {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val rows = mutableListOf<CuratedTrack>()

    private fun seed(title: String, artist: String, videoId: String, durationMs: Int?, mood: String) {
        rows.add(
            CuratedTrack(
                title = title,
                artist = artist,
                youtubeVideoId = videoId,
                durationMs = durationMs,
                mood = mood,
                coverUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
            )
        )
    }

    @Transactional
    override fun run(vararg args: String?) {
        if (curatedTrackRepository.count() > 0L) {
            log.info("Curated catalog already seeded — skip")
            return
        }
        seed("Apocalypse", "Cigarettes After Sex", "sElE_BfQ67s", 290000, "latenight")
        seed("Space Song", "Beach House", "GAFwrXOsL68", 320000, "latenight")
        seed("Best Part (feat. H.E.R.)", "Daniel Caesar", "vBy7FaapGRo", 209000, "latenight")
        seed("EVERYTHING", "검정치마 (The Black Skirts)", "Aq_gsctWHtQ", 222000, "latenight")
        seed("난춘 (亂春) (NAN CHUN)", "새소년 (SE SO NEON)", "KsznX5j2oQ0", 268000, "latenight")
        seed("Blinding Lights", "The Weeknd", "4NRXx6U8ABQ", 240000, "drive")
        seed("Midnight City", "M83", "dX3k_QDnzHE", 244000, "drive")
        seed("Something About Us", "Daft Punk", "em0MknB6wFo", 232000, "drive")
        seed("1901", "Phoenix", "cFElidiwxYU", 193000, "drive")
        seed("Nightcall", "Kavinsky", "ZVS6Q_lbKQ0", 257000, "drive")
        seed("Perfect", "Ed Sheeran", "2Vv-BfVoq4g", 263000, "love")
        seed("All of Me", "John Legend", "450p7goxZqg", 270000, "love")
        seed("strawberry moon", "IU (아이유)", "sqgxcCjD04s", 220000, "love")
        seed("Just the Way You Are", "Bruno Mars", "LjhCEhWiKXk", 221000, "love")
        seed("모든 날, 모든 순간 (Every Day, Every Moment)", "Paul Kim (폴킴)", "1q_t6RNuH8c", 260000, "love")
        seed("I Won't Give Up", "Jason Mraz", "O1-4u9W-bns", 240000, "comfort")
        seed("Lean on Me", "Bill Withers", "gOZgo8gMIoM", 259000, "comfort")
        seed("Through the Night (밤편지)", "IU (아이유)", "BzYnNdJhZQw", 253000, "comfort")
        seed("You've Got a Friend", "Carole King", "eAR_Ff5A8Rk", 291000, "comfort")
        seed("Let It Be", "The Beatles", "egCy1KoE1Ss", 243000, "comfort")
        seed("Kerala", "Bonobo", "S0Q4gqBUs7c", 224000, "focus")
        seed("Let It Happen", "Tame Impala", "-ed6UeDp1ek", 467000, "focus")
        seed("Feather", "Nujabes", "hQ5x8pHoIPA", 314000, "focus")
        seed("White Gloves", "Khruangbin", "wJzoos4rE_o", 256000, "focus")
        seed("Show Me How", "Men I Trust", "OZRYzH0Q0pU", 200000, "focus")
        seed("Don't Start Now", "Dua Lipa", "oygrmJFKYZY", 183000, "energy")
        seed("Uptown Funk", "Mark Ronson ft. Bruno Mars", "OPf0YbXqDm0", 270000, "energy")
        seed("Get Lucky", "Daft Punk ft. Pharrell Williams, Nile Rodgers", "5NV6Rdv1a3I", 369000, "energy")
        seed("Happy", "Pharrell Williams", "ZbZSe6N_BXs", 233000, "energy")
        seed("Feel So Close", "Calvin Harris", "d-2jLh1znTY", 207000, "energy")
        curatedTrackRepository.saveAll(rows)
        log.info("Seeded curated catalog: {} tracks", rows.size)
    }
}
