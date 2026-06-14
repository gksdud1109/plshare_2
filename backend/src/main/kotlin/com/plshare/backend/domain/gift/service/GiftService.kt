package com.plshare.backend.domain.gift.service

import com.plshare.backend.domain.asset.repository.AssetRepository
import com.plshare.backend.domain.gift.dto.*
import com.plshare.backend.domain.gift.model.Gift
import com.plshare.backend.domain.gift.model.GiftStatus
import com.plshare.backend.domain.gift.repository.GiftRepository
import com.plshare.backend.domain.user.repository.UserRepository
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

/**
 * 감성 선물 도메인 서비스.
 *
 * 불변식:
 * - 선물 생성 시 senderId(User), assetId(Asset) 모두 실존해야 한다. 없으면 NOT_FOUND.
 * - open은 멱등: 이미 OPENED/SAVED 상태이면 별도 처리 없이 현재 상태 반환.
 * - save는 SAVED 상태로 전이하고 savedByUserId를 기록한다.
 *   v1: 자산 복제 없이 수령 기록만 남긴다.
 *   향후 자산 소유권 모델(ownerId) 도입 시 수령자 라이브러리로 Asset 복제 예정.
 */
@Service
@Transactional(readOnly = true)
class GiftService(
    private val giftRepository: GiftRepository,
    private val assetRepository: AssetRepository,
    private val userRepository: UserRepository,
) {

    /**
     * 선물 생성.
     * senderId, assetId 존재 검증 후 토큰을 발급한다.
     * token = UUID.randomUUID().toString().replace("-","").take(16) (share token 패턴 동일).
     */
    @Transactional
    fun create(
        req: CreateGiftRequest,
        senderId: UUID,
        requireOwnedAsset: Boolean = false,
    ): GiftCreatedResponse {
        userRepository.findById(senderId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "발신자를 찾을 수 없습니다: $senderId")
        }
        val asset = assetRepository.findById(req.assetId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "자산을 찾을 수 없습니다: ${req.assetId}")
        }
        if (requireOwnedAsset && asset.ownerId != senderId) {
            throw ApiException(ErrorCode.FORBIDDEN, "본인 소유 자산만 선물할 수 있습니다")
        }
        if (req.message.length > 3000) {
            throw ApiException(ErrorCode.VALIDATION_FAILED, "메시지는 3000자를 초과할 수 없습니다")
        }

        val token = UUID.randomUUID().toString().replace("-", "").take(16)
        val gift = Gift(
            senderId = senderId,
            assetId = req.assetId,
            message = req.message,
            wrapSkin = req.wrapSkin,
            token = token,
        )
        giftRepository.save(gift)
        return GiftCreatedResponse(token = token, url = "/gift/$token")
    }

    /**
     * 공개 선물 뷰 조회 (인증 불필요).
     * 발신자 handle/displayName/avatar + 자산 상세(커버/제목/트랙) + message + wrapSkin.
     */
    fun view(token: String): GiftViewResponse {
        val gift = giftRepository.findByToken(token)
            ?: throw ApiException(ErrorCode.NOT_FOUND, "선물을 찾을 수 없습니다: $token")
        val sender = userRepository.findById(gift.senderId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "발신자를 찾을 수 없습니다: ${gift.senderId}")
        }
        val asset = assetRepository.findById(gift.assetId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "자산을 찾을 수 없습니다: ${gift.assetId}")
        }
        return GiftViewResponse.from(gift, sender, asset)
    }

    /**
     * 언박싱 진입 — status를 OPENED로 전이하고 openedAt을 기록한다(멱등).
     * 이미 OPENED 또는 SAVED 상태이면 openedAt을 덮어쓰지 않는다.
     */
    @Transactional
    fun open(token: String): GiftViewResponse {
        val gift = giftRepository.findByToken(token)
            ?: throw ApiException(ErrorCode.NOT_FOUND, "선물을 찾을 수 없습니다: $token")

        if (gift.status == GiftStatus.CREATED) {
            gift.status = GiftStatus.OPENED
            gift.openedAt = LocalDateTime.now()
            giftRepository.save(gift)
        }
        // 이미 OPENED/SAVED이면 멱등 — 상태 그대로 반환

        val sender = userRepository.findById(gift.senderId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "발신자를 찾을 수 없습니다: ${gift.senderId}")
        }
        val asset = assetRepository.findById(gift.assetId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "자산을 찾을 수 없습니다: ${gift.assetId}")
        }
        return GiftViewResponse.from(gift, sender, asset)
    }

    /**
     * 수신자 라이브러리 저장 — 멱등(first-writer-wins).
     *
     * 최초 저장자만 savedByUserId 로 기록한다. 이미 저장된 선물은 상태를 그대로 두고
     * 현재 뷰를 반환한다 — 공유 링크를 받은 제3자가 최초 수령자의 기록을 덮어쓰는 것을 막는다.
     * v1: 자산 복제 없이 수령 기록만 남긴다. 향후 ownerId 모델 도입 시 Asset 복제 추가.
     */
    @Transactional
    fun save(token: String, savedByUserId: UUID): GiftViewResponse {
        val gift = giftRepository.findByToken(token)
            ?: throw ApiException(ErrorCode.NOT_FOUND, "선물을 찾을 수 없습니다: $token")

        if (gift.savedByUserId == null) {
            userRepository.findById(savedByUserId).orElseThrow {
                ApiException(ErrorCode.NOT_FOUND, "수신자를 찾을 수 없습니다: $savedByUserId")
            }
            gift.status = GiftStatus.SAVED
            gift.savedByUserId = savedByUserId
            giftRepository.save(gift)
        }
        // 이미 저장됨 → 멱등, 상태 그대로 반환

        val sender = userRepository.findById(gift.senderId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "발신자를 찾을 수 없습니다: ${gift.senderId}")
        }
        val asset = assetRepository.findById(gift.assetId).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "자산을 찾을 수 없습니다: ${gift.assetId}")
        }
        return GiftViewResponse.from(gift, sender, asset)
    }
}
