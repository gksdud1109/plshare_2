package com.plshare.backend.api

import com.plshare.backend.api.dto.AssetDetailDto
import com.plshare.backend.domain.repository.AssetRepository
import com.plshare.backend.infrastructure.storage.StorageAdapter
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Photo upload endpoints for Emotional Context.
 *
 * Flow:
 *   1. Client POSTs /presign with a content-type and filename, gets back a presigned upload URL.
 *   2. Client PUTs the file bytes directly to that URL (S3 in prod, local demo controller in demo).
 *   3. Client POSTs /attach with the returned publicUrl to register the photo on the asset.
 */
@RestController
@RequestMapping("/api/assets/{assetId}/media")
class MediaController(
    private val storage: StorageAdapter,
    private val assetRepository: AssetRepository
) {
    data class PresignRequest(val contentType: String, val filename: String)

    data class PresignResponse(
        val uploadUrl: String,
        val publicUrl: String,
        val key: String,
        val expiresAt: String,
        val requiredHeaders: Map<String, String>
    )

    data class AttachRequest(val publicUrl: String)

    @PostMapping("/presign")
    fun presign(
        @PathVariable assetId: UUID,
        @RequestBody body: PresignRequest
    ): PresignResponse {
        // Verify the asset exists so we don't issue presigns for ghosts.
        if (!assetRepository.existsById(assetId)) {
            throw NoSuchElementException("Asset not found: $assetId")
        }
        val key = buildKey(assetId, body.filename)
        val result = storage.presignUpload(key, body.contentType)
        return PresignResponse(
            uploadUrl = result.uploadUrl,
            publicUrl = result.publicUrl,
            key = result.key,
            expiresAt = result.expiresAt.toString(),
            requiredHeaders = result.requiredHeaders
        )
    }

    @PostMapping("/attach")
    @Transactional
    fun attach(
        @PathVariable assetId: UUID,
        @RequestBody body: AttachRequest
    ): AssetDetailDto {
        val asset = assetRepository.findById(assetId).orElseThrow {
            NoSuchElementException("Asset not found: $assetId")
        }
        if (body.publicUrl.isBlank()) {
            throw IllegalArgumentException("publicUrl required")
        }
        if (!asset.photoUrls.contains(body.publicUrl)) {
            asset.photoUrls.add(body.publicUrl)
        }
        val saved = assetRepository.save(asset)
        return AssetDetailDto.from(saved)
    }

    @DeleteMapping("/{photoIndex}")
    @Transactional
    @ResponseStatus(HttpStatus.OK)
    fun delete(
        @PathVariable assetId: UUID,
        @PathVariable photoIndex: Int
    ): AssetDetailDto {
        val asset = assetRepository.findById(assetId).orElseThrow {
            NoSuchElementException("Asset not found: $assetId")
        }
        if (photoIndex < 0 || photoIndex >= asset.photoUrls.size) {
            throw IndexOutOfBoundsException("photoIndex $photoIndex out of bounds (size=${asset.photoUrls.size})")
        }
        val removedUrl = asset.photoUrls.removeAt(photoIndex)
        // Best-effort delete from storage. Key derivation: any path segment after our known
        // prefix `assets/`. If we can't recover it we still drop the URL from the asset.
        runCatching { keyFromPublicUrl(removedUrl)?.let { storage.delete(it) } }
        val saved = assetRepository.save(asset)
        return AssetDetailDto.from(saved)
    }

    private fun buildKey(assetId: UUID, filename: String): String {
        val safe = sanitizeFilename(filename)
        return "assets/$assetId/${UUID.randomUUID()}-$safe"
    }

    private fun sanitizeFilename(raw: String): String {
        val trimmed = raw.trim().ifBlank { "upload.bin" }
        // Drop any directory components and keep alnum / dot / dash / underscore.
        val basename = trimmed.substringAfterLast('/').substringAfterLast('\\')
        return basename.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120)
    }

    /** Best-effort: pull the key out of a public URL if it contains the conventional `assets/...` segment. */
    private fun keyFromPublicUrl(url: String): String? {
        val marker = "assets/"
        val idx = url.indexOf(marker)
        return if (idx >= 0) url.substring(idx) else null
    }
}
