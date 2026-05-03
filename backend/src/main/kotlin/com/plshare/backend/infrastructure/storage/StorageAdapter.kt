package com.plshare.backend.infrastructure.storage

import java.time.Instant

/**
 * Abstraction over object storage backends.
 *
 * - demo profile  -> [LocalFilesystemStorageAdapter] (no external service required)
 * - other profiles -> [S3StorageAdapter] (S3 / MinIO compatible)
 */
interface StorageAdapter {
    /** Issue a presigned upload URL together with the final public URL the client should use after upload. */
    fun presignUpload(key: String, contentType: String, expiresInSeconds: Long = 600): PresignResult

    /** Public (download) URL for an already-uploaded object. */
    fun publicUrl(key: String): String

    /** Delete an object by key. Implementations must be idempotent (no-op if missing). */
    fun delete(key: String)

    data class PresignResult(
        val uploadUrl: String,
        val publicUrl: String,
        val key: String,
        val expiresAt: Instant,
        val requiredHeaders: Map<String, String> = emptyMap()
    )
}
