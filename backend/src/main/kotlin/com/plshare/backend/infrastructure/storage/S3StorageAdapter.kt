package com.plshare.backend.infrastructure.storage

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Production / non-demo storage adapter targeting S3-compatible object stores
 * (AWS S3 or MinIO). Activated for any profile other than `demo`.
 *
 * STATUS: stub. Issuing real AWS SigV4 presigned URLs without the AWS SDK is non-trivial,
 * and adding the SDK dependency belongs to a separate change (build.gradle.kts is owned by
 * a different agent in this batch). The stub keeps the bean wireable so the rest of the
 * application boots, but throws on first call so misconfiguration is loud.
 *
 * TODO: implement SigV4 manually (HMAC-SHA256 / SHA-256 / canonical request, see
 *       https://docs.aws.amazon.com/general/latest/gr/sigv4-create-canonical-request.html)
 *       OR add aws-sdk-java-v2 s3 + s3-presigner once the build owner allows new deps.
 */
@Component
@Profile("!demo")
class S3StorageAdapter(
    private val config: StorageConfig
) : StorageAdapter {
    private val log = LoggerFactory.getLogger(javaClass)

    init {
        log.info(
            "S3StorageAdapter loaded (bucket={}, region={}, endpoint={}, credentials={})",
            config.bucket,
            config.region,
            config.endpoint.ifBlank { "<aws default>" },
            if (config.accessKey.isBlank() || config.secretKey.isBlank()) "MISSING" else "present"
        )
    }

    override fun presignUpload(
        key: String,
        contentType: String,
        expiresInSeconds: Long
    ): StorageAdapter.PresignResult {
        ensureConfigured()
        // TODO: implement SigV4 manually.
        throw IllegalStateException("Configure S3StorageAdapter for production use (SigV4 presign not yet implemented)")
    }

    override fun publicUrl(key: String): String {
        ensureConfigured()
        val base = config.publicBaseUrl.trimEnd('/')
        return if (base.isNotBlank()) {
            "$base/$key"
        } else if (config.endpoint.isNotBlank()) {
            "${config.endpoint.trimEnd('/')}/${config.bucket}/$key"
        } else {
            "https://${config.bucket}.s3.${config.region}.amazonaws.com/$key"
        }
    }

    override fun delete(key: String) {
        ensureConfigured()
        // TODO: issue authenticated DELETE against the bucket.
        throw IllegalStateException("Configure S3StorageAdapter for production use (delete not yet implemented)")
    }

    private fun ensureConfigured() {
        if (config.accessKey.isBlank() || config.secretKey.isBlank() || config.bucket.isBlank()) {
            throw IllegalStateException("S3 credentials not configured")
        }
    }
}
