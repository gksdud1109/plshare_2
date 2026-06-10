package com.plshare.backend.domain.follow.dto

import java.util.UUID

/** 팔로우 통계 응답 DTO. */
data class FollowStatsResponse(
    val followerCount: Long,
    val followingCount: Long,
    /** viewerId가 이 유저를 팔로우하고 있는지 여부. viewerId가 null이면 항상 false. */
    val isFollowing: Boolean,
)
