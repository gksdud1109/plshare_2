package com.plshare.backend.domain.track.service

import com.plshare.backend.domain.asset.model.Asset
import com.plshare.backend.domain.asset.model.Track
import com.plshare.backend.domain.track.repository.TrackRepository
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import com.plshare.backend.infrastructure.youtube.YouTubeClient
import com.plshare.backend.infrastructure.youtube.YouTubeSearchCandidate
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import java.util.Optional
import java.util.UUID

/**
 * TrackPlaybackService.resolveYouTubeVideoId 단위 테스트.
 *
 * 핵심: 검색 후보가 없는(null) 트랙은 음성 캐시되어 재호출 시 재검색(=쿼터 재소모)하지 않는다.
 * (리뷰가 지적한 익명 쿼터 드레인 벡터의 회귀 방지.)
 */
class TrackPlaybackServiceTest {

    private val asset = Asset(ownerId = UUID.randomUUID(), title = "Test Asset")

    private fun track(name: String = "Song", artist: String = "Artist") =
        Track(asset = asset, name = name, artist = artist)

    @Test
    fun `미해결 트랙은 음성 캐시되어 두 번째 호출에서 재검색하지 않는다`() {
        val t = track()
        val repo = mockk<TrackRepository>()
        every { repo.findById(t.id) } returns Optional.of(t)
        val yt = mockk<YouTubeClient>()
        every { yt.searchVideoCandidates(any(), any(), any()) } returns Mono.just(emptyList())

        val svc = TrackPlaybackService(repo, yt, youtubeQuotaGuard = null)

        assertNull(svc.resolveYouTubeVideoId(t.id))
        assertNull(svc.resolveYouTubeVideoId(t.id)) // 두 번째 — 음성 캐시 단락

        verify(exactly = 1) { yt.searchVideoCandidates(any(), any(), any()) }
    }

    @Test
    fun `해결된 트랙은 DB에 캐시되어 재검색하지 않는다`() {
        val t = track()
        val repo = mockk<TrackRepository>()
        every { repo.findById(t.id) } returns Optional.of(t)
        every { repo.save(any<Track>()) } answers { firstArg() }
        val yt = mockk<YouTubeClient>()
        every { yt.searchVideoCandidates(any(), any(), any()) } returns
            Mono.just(listOf(YouTubeSearchCandidate("realId12345", "Song", "Artist")))

        val svc = TrackPlaybackService(repo, yt, youtubeQuotaGuard = null)

        assertEquals("realId12345", svc.resolveYouTubeVideoId(t.id))
        assertEquals("realId12345", svc.resolveYouTubeVideoId(t.id)) // DB 캐시(youtubeVideoId) 단락

        verify(exactly = 1) { yt.searchVideoCandidates(any(), any(), any()) }
    }

    @Test
    fun `존재하지 않는 트랙은 NOT_FOUND`() {
        val repo = mockk<TrackRepository>()
        every { repo.findById(any()) } returns Optional.empty()
        val svc = TrackPlaybackService(repo, mockk(), youtubeQuotaGuard = null)

        val ex = assertThrows(ApiException::class.java) {
            svc.resolveYouTubeVideoId(UUID.randomUUID())
        }
        assertEquals(ErrorCode.NOT_FOUND, ex.code)
    }
}
