package com.plshare.backend.domain.gift.dto

import com.plshare.backend.domain.asset.dto.TrackDto
import com.plshare.backend.domain.asset.model.Asset
import com.plshare.backend.domain.asset.model.AssetKind
import com.plshare.backend.domain.gift.model.Gift
import com.plshare.backend.domain.gift.model.GiftStatus
import com.plshare.backend.domain.gift.model.Occasion
import com.plshare.backend.domain.user.model.User
import java.time.LocalDateTime
import java.util.UUID

/** POST /api/gifts 요청 바디. 발신자는 @CurrentUserId(세션 principal)로 해석한다. */
data class CreateGiftRequest(
    val assetId: UUID,
    val message: String,
    val wrapSkin: String,
    /** 헌정 — 받는 사람 이름/애칭(최대 40자, 선택). */
    val dedicationTo: String? = null,
    /** 계기 — 선택. */
    val occasion: Occasion? = null,
)

/** POST /api/gifts 응답: 생성된 토큰 + 공개 링크. */
data class GiftCreatedResponse(
    val token: String,
    val url: String,
)

/** GET /api/gifts/{token} 공개 뷰. */
data class GiftViewResponse(
    val token: String,
    val status: GiftStatus,
    val message: String,
    val wrapSkin: String,
    val dedicationTo: String?,
    val occasion: Occasion?,
    val sender: SenderDto,
    val asset: GiftAssetDto,
) {
    companion object {
        fun from(gift: Gift, sender: User, asset: Asset): GiftViewResponse =
            GiftViewResponse(
                token = gift.token,
                status = gift.status,
                message = gift.message,
                wrapSkin = gift.wrapSkin,
                dedicationTo = gift.dedicationTo,
                occasion = gift.occasion,
                sender = SenderDto.from(sender),
                asset = GiftAssetDto.from(asset),
            )
    }
}

data class SenderDto(
    val handle: String,
    val displayName: String,
    val avatarUrl: String?,
) {
    companion object {
        fun from(user: User) = SenderDto(
            handle = user.handle,
            displayName = user.displayName,
            avatarUrl = user.avatarUrl,
        )
    }
}

data class GiftAssetDto(
    val id: UUID,
    val title: String,
    val coverUrl: String?,
    val tracks: List<TrackDto>,
    /** 듀얼 포맷 — 뷰가 재생 컴포넌트를 이걸로 분기한다. */
    val assetKind: AssetKind,
    val moodVideoId: String?,
    val moodChannelName: String?,
    val moodTrackListText: String?,
) {
    companion object {
        fun from(asset: Asset) = GiftAssetDto(
            id = asset.id,
            title = asset.title,
            coverUrl = asset.coverUrl,
            tracks = asset.tracks.map { TrackDto.from(it) },
            assetKind = asset.assetKind,
            moodVideoId = asset.moodVideoId,
            moodChannelName = asset.moodChannelName,
            moodTrackListText = asset.moodTrackListText,
        )
    }
}

/** 선물함(받은/보낸) 목록용 요약 — 트랙 전체 대신 제목/커버/곡수만. */
data class GiftSummaryResponse(
    val token: String,
    val status: GiftStatus,
    val message: String,
    val wrapSkin: String,
    val dedicationTo: String?,
    val occasion: Occasion?,
    val sender: SenderDto,
    val assetTitle: String,
    val assetCoverUrl: String?,
    val trackCount: Int,
    val assetKind: AssetKind,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(gift: Gift, sender: User, asset: Asset) = GiftSummaryResponse(
            token = gift.token,
            status = gift.status,
            message = gift.message,
            wrapSkin = gift.wrapSkin,
            dedicationTo = gift.dedicationTo,
            occasion = gift.occasion,
            sender = SenderDto.from(sender),
            assetTitle = asset.title,
            assetCoverUrl = asset.coverUrl,
            trackCount = asset.tracks.size,
            assetKind = asset.assetKind,
            createdAt = gift.createdAt,
        )
    }
}
