package com.plshare.backend.api

import com.plshare.backend.api.dto.AssetDetailDto
import com.plshare.backend.api.dto.AssetSummaryDto
import com.plshare.backend.api.dto.ShareResponseDto
import com.plshare.backend.api.dto.UpdateAssetRequest
import com.plshare.backend.domain.repository.AssetRepository
import com.plshare.backend.global.exception.ApiException
import com.plshare.backend.global.exception.ErrorCode
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api")
class AssetController(
    private val assetRepository: AssetRepository
) {
    @GetMapping("/assets")
    @Transactional(readOnly = true)
    fun list(): List<AssetSummaryDto> =
        assetRepository.findAll().map { AssetSummaryDto.from(it) }

    @GetMapping("/assets/{id}")
    @Transactional(readOnly = true)
    fun get(@PathVariable id: UUID): AssetDetailDto {
        val asset = assetRepository.findById(id).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "Asset not found: $id")
        }
        return AssetDetailDto.from(asset)
    }

    @PatchMapping("/assets/{id}")
    @Transactional
    fun update(@PathVariable id: UUID, @RequestBody body: UpdateAssetRequest): AssetDetailDto {
        val asset = assetRepository.findById(id).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "Asset not found: $id")
        }
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
        return AssetDetailDto.from(saved)
    }

    @PostMapping("/assets/{id}/share")
    @Transactional
    fun share(@PathVariable id: UUID): ShareResponseDto {
        val asset = assetRepository.findById(id).orElseThrow {
            ApiException(ErrorCode.NOT_FOUND, "Asset not found: $id")
        }
        if (asset.shareToken == null) {
            asset.shareToken = UUID.randomUUID().toString().replace("-", "").take(16)
            assetRepository.save(asset)
        }
        val token = asset.shareToken!!
        return ShareResponseDto(shareToken = token, url = "/share/$token")
    }
}

@RestController
@RequestMapping("/api")
class ShareController(
    private val assetRepository: AssetRepository
) {
    @GetMapping("/share/{token}")
    @Transactional(readOnly = true)
    fun publicAsset(@PathVariable token: String): AssetDetailDto {
        val asset = assetRepository.findByShareToken(token)
            ?: throw ApiException(ErrorCode.NOT_FOUND, "Shared asset not found")
        return AssetDetailDto.from(asset)
    }
}
