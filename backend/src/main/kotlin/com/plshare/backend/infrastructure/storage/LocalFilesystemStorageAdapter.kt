package com.plshare.backend.infrastructure.storage

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

/**
 * Demo-only storage adapter. Persists uploaded bytes to a local filesystem directory under
 * `${user.home}/.plshare-demo-uploads/` and serves them back via the demo file controller.
 *
 * presignUpload returns a fake upload URL pointing at our own [DemoUploadController] so the
 * frontend's PUT-to-presigned-url flow still exercises the same code paths it would in prod.
 */
@Component
@Profile("demo")
class LocalFilesystemStorageAdapter : StorageAdapter {
    private val log = LoggerFactory.getLogger(javaClass)
    private val baseDir: Path = Path.of(System.getProperty("user.home"), ".plshare-demo-uploads")
    private val basePublicUrl = "http://localhost:8080/api/_demo/files"
    private val baseUploadUrl = "http://localhost:8080/api/_demo/upload"

    init {
        Files.createDirectories(baseDir)
        log.info("LocalFilesystemStorageAdapter active. uploads dir = {}", baseDir)
    }

    override fun presignUpload(
        key: String,
        contentType: String,
        expiresInSeconds: Long
    ): StorageAdapter.PresignResult {
        return StorageAdapter.PresignResult(
            uploadUrl = "$baseUploadUrl/$key",
            publicUrl = publicUrl(key),
            key = key,
            expiresAt = Instant.now().plusSeconds(expiresInSeconds),
            requiredHeaders = mapOf("Content-Type" to contentType)
        )
    }

    override fun publicUrl(key: String): String = "$basePublicUrl/$key"

    override fun delete(key: String) {
        val target = resolveSafe(key) ?: return
        Files.deleteIfExists(target)
    }

    /** Resolve a key to a path under [baseDir], rejecting traversal attempts. */
    fun resolveSafe(key: String): Path? {
        val candidate = baseDir.resolve(key).normalize()
        return if (candidate.startsWith(baseDir)) candidate else null
    }

    fun store(key: String, bytes: ByteArray) {
        val target = resolveSafe(key)
            ?: throw IllegalArgumentException("Invalid key: $key")
        Files.createDirectories(target.parent)
        Files.write(target, bytes)
    }

    fun read(key: String): ByteArray? {
        val target = resolveSafe(key) ?: return null
        if (!Files.exists(target)) return null
        return Files.readAllBytes(target)
    }
}

/**
 * Receives the fake-presigned PUT issued by [LocalFilesystemStorageAdapter].
 * Client uploads raw bytes; we stream them into the local uploads directory.
 *
 * Note: the {key} is captured as a wildcard so it can include slashes
 * (e.g. `assets/{assetId}/{uuid}-photo.jpg`).
 */
@RestController
@Profile("demo")
@RequestMapping("/api/_demo")
class DemoUploadController(
    private val adapter: LocalFilesystemStorageAdapter
) {
    @PutMapping("/upload/**")
    fun upload(request: HttpServletRequest): ResponseEntity<Map<String, String>> {
        val key = extractKey(request, prefix = "/api/_demo/upload/")
        val bytes = request.inputStream.readAllBytes()
        adapter.store(key, bytes)
        return ResponseEntity.ok(mapOf("key" to key, "bytes" to bytes.size.toString()))
    }

    private fun extractKey(req: HttpServletRequest, prefix: String): String {
        val path = req.requestURI
        val idx = path.indexOf(prefix)
        require(idx >= 0) { "request path missing prefix $prefix" }
        return path.substring(idx + prefix.length)
    }
}

/**
 * Streams previously-uploaded files back to the browser for the demo profile.
 */
@RestController
@Profile("demo")
@RequestMapping("/api/_demo")
class DemoFileController(
    private val adapter: LocalFilesystemStorageAdapter
) {
    @GetMapping("/files/**")
    fun get(request: HttpServletRequest): ResponseEntity<ByteArray> {
        val key = run {
            val path = request.requestURI
            val prefix = "/api/_demo/files/"
            val idx = path.indexOf(prefix)
            require(idx >= 0)
            path.substring(idx + prefix.length)
        }
        val bytes = adapter.read(key) ?: return ResponseEntity.notFound().build()
        val contentType = guessContentType(key)
        return ResponseEntity.ok()
            .contentType(contentType)
            .body(bytes)
    }

    @DeleteMapping("/files/**")
    fun delete(request: HttpServletRequest): ResponseEntity<Void> {
        val path = request.requestURI
        val prefix = "/api/_demo/files/"
        val idx = path.indexOf(prefix)
        require(idx >= 0)
        val key = path.substring(idx + prefix.length)
        adapter.delete(key)
        return ResponseEntity.noContent().build()
    }

    private fun guessContentType(key: String): MediaType {
        val lower = key.lowercase()
        return when {
            lower.endsWith(".png") -> MediaType.IMAGE_PNG
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> MediaType.IMAGE_JPEG
            lower.endsWith(".gif") -> MediaType.IMAGE_GIF
            lower.endsWith(".webp") -> MediaType.parseMediaType("image/webp")
            else -> MediaType.APPLICATION_OCTET_STREAM
        }
    }
}

