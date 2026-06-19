package com.plshare.backend.domain.catalog.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.plshare.backend.domain.asset.model.Asset
import com.plshare.backend.domain.asset.repository.AssetRepository
import com.plshare.backend.domain.catalog.dto.ComposeAssetRequest
import com.plshare.backend.domain.catalog.model.CuratedTrack
import com.plshare.backend.domain.catalog.repository.CuratedTrackRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class CatalogServiceTest {
    private val selectionTokenCodec =
        YouTubeCatalogSelectionTokenCodec(jacksonObjectMapper(), "test-selection-secret")

    @Test
    fun `같은 소유자와 멱등키의 compose 재요청은 기존 플레이리스트를 반환한다`() {
        val ownerId = UUID.randomUUID()
        val track = CuratedTrack(
            title = "Song",
            artist = "Artist",
            youtubeVideoId = "abcdefghijk",
            mood = "focus",
        )
        val curatedTracks = mockk<CuratedTrackRepository>()
        val assets = mockk<AssetRepository>()
        var saved: Asset? = null

        every { curatedTracks.findAllById(any<Iterable<UUID>>()) } returns listOf(track)
        every {
            assets.findByOwnerIdAndComposeIdempotencyKey(ownerId, "compose-key")
        } answers {
            saved?.takeIf { it.composeIdempotencyKey == "compose-key" }
        }
        every { assets.save(any<Asset>()) } answers {
            firstArg<Asset>().also { saved = it }
        }

        val service = CatalogService(curatedTracks, assets, selectionTokenCodec)
        val request = ComposeAssetRequest(title = "Playlist", trackIds = listOf(track.id))

        val first = service.compose(request, ownerId, "compose-key")
        val retry = service.compose(request, ownerId, "compose-key")

        assertEquals(first.id, retry.id)
        assertEquals("compose-key", saved?.composeIdempotencyKey)
        verify(exactly = 1) { assets.save(any<Asset>()) }
    }

    @Test
    fun `compose accepts ordered curated UUID and signed YouTube selection without client metadata`() {
        val ownerId = UUID.randomUUID()
        val curatedTrack = CuratedTrack(
            title = "Curated Song",
            artist = "Curated Artist",
            youtubeVideoId = "abcdefghijk",
            mood = "focus",
        )
        val searched = YouTubeCatalogCandidate(
            videoId = "lmnopqrstuv",
            title = "Searched Song",
            channelTitle = "Searched Artist - Topic",
            thumbnailUrl = "https://i.ytimg.com/vi/lmnopqrstuv/hqdefault.jpg",
        )
        val curatedTracks = mockk<CuratedTrackRepository>()
        val assets = mockk<AssetRepository>()
        var saved: Asset? = null

        every { curatedTracks.findAllById(any<Iterable<UUID>>()) } returns listOf(curatedTrack)
        every {
            assets.findByOwnerIdAndComposeIdempotencyKey(ownerId, "mixed-compose-key")
        } returns null
        every { assets.save(any<Asset>()) } answers {
            firstArg<Asset>().also { saved = it }
        }

        val request = ComposeAssetRequest(
            title = "Mixed Playlist",
            selectionIds = listOf(
                curatedTrack.id.toString(),
                selectionTokenCodec.issue(searched),
            ),
        )
        CatalogService(curatedTracks, assets, selectionTokenCodec)
            .compose(request, ownerId, "mixed-compose-key")

        assertEquals(listOf("Curated Song", "Searched Song"), saved?.tracks?.map { it.name })
        assertEquals(
            listOf("abcdefghijk", "lmnopqrstuv"),
            saved?.tracks?.map { it.youtubeVideoId },
        )
        assertEquals("Searched Artist - Topic", saved?.tracks?.get(1)?.artist)
        assertEquals(searched.thumbnailUrl, saved?.coverUrl)
    }
}
