package com.plshare.backend.infrastructure.storage

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration for object storage (photo uploads).
 *
 * Bound to YAML prefix `storage`. All fields default to safe empty values so the bean
 * boots even without credentials (demo profile). The S3 adapter resolves them lazily
 * and throws on first use if any required field is blank.
 */
@ConfigurationProperties(prefix = "storage")
data class StorageConfig(
    val bucket: String = "plshare-photos",
    val endpoint: String = "",
    val region: String = "us-east-1",
    val accessKey: String = "",
    val secretKey: String = "",
    val publicBaseUrl: String = ""
)
