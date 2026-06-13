package com.plshare.backend.domain.asset.controller

import com.plshare.backend.domain.asset.dto.AssetDetailDto
import com.plshare.backend.domain.asset.dto.AssetSummaryDto
import com.plshare.backend.domain.asset.dto.ShareResponseDto
import com.plshare.backend.domain.asset.dto.UpdateAssetRequest
import com.plshare.backend.domain.asset.repository.AssetRepository
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import com.plshare.backend.global.response.ApiResponse
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.UUID
import com.plshare.backend.global.security.ApplicationPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal

@RestController
@RequestMapping("/api")
class AssetController(
    private val assetRepository: AssetRepository
) {
    @GetMapping("/assets")
    @Transactional(readOnly = true)
    fun list(
        @AuthenticationPrincipal principal: ApplicationPrincipal?,
    ): ApiResponse<List<AssetSummaryDto>> {
        val assets = assetRepository.findAll()
            .filter { principal == null || it.ownerId == principal.userId }
            .sortedByDescending { it.createdAt }
        return ApiResponse.ok(assets.map { AssetSummaryDto.from(it) })
    }

    @GetMapping("/assets/{id}")
    @Transactional(readOnly = true)
    fun get(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: ApplicationPrincipal?,
    ): ApiResponse<AssetDetailDto> {
        val asset = assetRepository.findById(id).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "Asset not found: $id")
        }
        requireOwner(asset.ownerId, principal)
        return ApiResponse.ok(AssetDetailDto.from(asset))
    }

    @PatchMapping("/assets/{id}")
    @Transactional
    fun update(
        @PathVariable id: UUID,
        @RequestBody body: UpdateAssetRequest,
        @AuthenticationPrincipal principal: ApplicationPrincipal?,
    ): ApiResponse<AssetDetailDto> {
        val asset = assetRepository.findById(id).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "Asset not found: $id")
        }
        requireOwner(asset.ownerId, principal)
        body.title?.let { asset.title = it }
        body.diaryText?.let { asset.diaryText = it }
        body.description?.let { asset.description = it }
        body.coverUrl?.let { asset.coverUrl = it }
        body.emotionTags?.let {
            asset.emotionTags.clear()
            asset.emotionTags.addAll(it)
        }
        body.photoUrls?.let {
            asset.photoUrls.clear()
            asset.photoUrls.addAll(it)
        }
        val saved = assetRepository.save(asset)
        return ApiResponse.ok(AssetDetailDto.from(saved))
    }

    @PostMapping("/assets/{id}/share")
    @Transactional
    fun share(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: ApplicationPrincipal?,
    ): ApiResponse<ShareResponseDto> {
        val asset = assetRepository.findById(id).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "Asset not found: $id")
        }
        requireOwner(asset.ownerId, principal)
        if (asset.shareToken == null) {
            asset.shareToken = UUID.randomUUID().toString().replace("-", "").take(16)
            assetRepository.save(asset)
        }
        val token = asset.shareToken!!
        return ApiResponse.ok(ShareResponseDto(shareToken = token, url = "/share/$token"))
    }

    private fun requireOwner(ownerId: UUID?, principal: ApplicationPrincipal?) {
        if (principal != null && ownerId != principal.userId) {
            throw ApiException(ErrorCode.FORBIDDEN, "Asset does not belong to the current user")
        }
    }
}

@RestController
@RequestMapping("/api")
class ShareController(
    private val assetRepository: AssetRepository
) {
    @GetMapping("/share/{token}")
    @Transactional(readOnly = true)
    fun publicAsset(@PathVariable token: String): ApiResponse<AssetDetailDto> {
        val asset = assetRepository.findByShareToken(token)
            ?: throw ApiException(ErrorCode.NOT_FOUND, "Shared asset not found")
        return ApiResponse.ok(AssetDetailDto.from(asset))
    }
}
