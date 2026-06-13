package com.plshare.backend.domain.gift.dto

import com.plshare.backend.domain.asset.dto.TrackDto
import com.plshare.backend.domain.asset.model.Asset
import com.plshare.backend.domain.gift.model.Gift
import com.plshare.backend.domain.gift.model.GiftStatus
import com.plshare.backend.domain.user.model.User
import java.util.UUID

/** POST /api/gifts 요청 바디. */
data class CreateGiftRequest(
    val senderId: UUID,
    val assetId: UUID,
    val message: String,
    val wrapSkin: String,
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
) {
    companion object {
        fun from(asset: Asset) = GiftAssetDto(
            id = asset.id,
            title = asset.title,
            coverUrl = asset.coverUrl,
            tracks = asset.tracks.map { TrackDto.from(it) },
        )
    }
}

/** POST /api/gifts/{token}/save 요청 바디. */
data class SaveGiftRequest(
    val userId: UUID,
)
