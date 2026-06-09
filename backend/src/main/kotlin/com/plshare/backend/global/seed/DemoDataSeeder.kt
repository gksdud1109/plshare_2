package com.plshare.backend.global.seed

import com.plshare.backend.domain.asset.model.Asset
import com.plshare.backend.domain.asset.model.Track
import com.plshare.backend.domain.asset.repository.AssetRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Profile("demo")
class DemoDataSeeder(
    private val assetRepository: AssetRepository
) : CommandLineRunner {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun run(vararg args: String?) {
        if (assetRepository.count() > 0L) {
            log.info("Skipping demo seed: assets already present")
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
    }
}
