package com.plshare.backend.infrastructure.youtube

import reactor.core.publisher.Mono

/**
 * YouTube Data API v3 읽기 어댑터 인터페이스.
 *
 * 설계 원칙 (Spotify 패턴 미러링):
 *   - accessToken은 파라미터로 전달받는다. 토큰 발급/저장은 이 인터페이스의 책임 아님.
 *   - 페이지네이션은 내부에서 nextPageToken 루프로 처리해 호출자에게 단일 결과를 반환한다.
 *   - 50개 초과 videoId 배치는 50개 단위로 분할해 내부에서 처리한다.
 *
 * YouTube Data API v3 quota 비용 (1일 10,000 unit 기본):
 *   - playlists.list 1 call    = 1 unit
 *   - playlistItems.list 1 call = 1 unit  (maxResults=50, 페이지당)
 *   - videos.list 1 call        = 1 unit  (ids 최대 50개)
 *   → read는 사실상 무제한(1 unit/call). 전략 근거: platform-strategy-v2-research §3.
 */
interface YouTubeClient {

    /**
     * 인증된 사용자의 YouTube 플레이리스트 목록 조회.
     * nextPageToken 루프를 내부에서 처리해 전체 결과를 반환한다.
     *
     * quota: playlists.list 호출 횟수 = ceil(총 플레이리스트 수 / 50) unit
     */
    fun listUserPlaylists(accessToken: String): Mono<List<YouTubePlaylistSummary>>

    /**
     * 특정 플레이리스트의 기본 정보 조회 (items 미포함).
     * items가 필요하면 [getPlaylistItems]를 사용한다.
     *
     * quota: playlists.list 1 unit
     */
    fun getPlaylist(playlistId: String, accessToken: String): Mono<YouTubePlaylistItem>

    /**
     * 플레이리스트의 모든 아이템 조회 (videoId, title, channelTitle 포함).
     * nextPageToken 루프로 전 페이지를 수집해 반환한다.
     *
     * quota: playlistItems.list ceil(아이템 수 / 50) unit
     */
    fun getPlaylistItems(playlistId: String, accessToken: String): Mono<List<YouTubePlaylistItemEntry>>

    /**
     * 비디오 상세(contentDetails.duration, snippet.title) 배치 조회.
     * videoIds가 50개를 초과하면 내부에서 50개 단위로 분할 요청한다.
     *
     * quota: videos.list ceil(videoIds.size / 50) unit
     */
    fun getVideoDetails(videoIds: List<String>, accessToken: String): Mono<List<YouTubeVideoItem>>

    /**
     * Spotify/Apple source tracks are resolved to a YouTube video before export.
     * search.list costs 100 quota units, so callers must reserve quota first.
     */
    fun searchVideo(title: String, artist: String, accessToken: String): Mono<String> =
        Mono.empty()
}
