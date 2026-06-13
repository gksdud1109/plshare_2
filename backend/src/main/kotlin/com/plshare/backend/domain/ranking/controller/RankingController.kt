package com.plshare.backend.domain.ranking.controller

import com.plshare.backend.domain.ranking.dto.PlaylistRankItemResponse
import com.plshare.backend.domain.ranking.dto.UserRankItemResponse
import com.plshare.backend.domain.ranking.service.RankingService
import com.plshare.backend.global.response.ApiResponse
import com.plshare.backend.global.response.PageResponse
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 랭킹 REST 컨트롤러.
 *
 * GET /api/ranking/playlists?period=&page=&size=
 * GET /api/ranking/users?period=&page=&size=
 *
 * period: realtime(기본) | weekly | all-time
 */
@RestController
@RequestMapping("/api/ranking")
class RankingController(private val rankingService: RankingService) {

    /** 플레이리스트(자산) 랭킹. 점수 = 포스트 좋아요 합 + export 완료 건수. */
    @GetMapping("/playlists")
    fun rankPlaylists(
        @RequestParam(defaultValue = "realtime") period: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<PlaylistRankItemResponse>> =
        ApiResponse.ok(rankingService.rankPlaylists(period, PageRequest.of(page, size)))

    /** 사용자 랭킹. 점수 = 포스트 좋아요 합 + 팔로워 수. */
    @GetMapping("/users")
    fun rankUsers(
        @RequestParam(defaultValue = "realtime") period: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<UserRankItemResponse>> =
        ApiResponse.ok(rankingService.rankUsers(period, PageRequest.of(page, size)))
}
