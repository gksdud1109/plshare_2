package com.plshare.backend.infrastructure.apple

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration for Apple MusicKit integration.
 *
 * Bound to YAML prefix `apple-music`. All fields default to empty strings so the bean can
 * still be created in the demo profile without credentials. Real adapter resolves them
 * lazily on first call and throws if any required field is blank.
 */
@ConfigurationProperties(prefix = "apple-music")
data class AppleMusicConfig(
    val teamId: String = "",
    val keyId: String = "",
    val privateKeyPem: String = "",
    val storefront: String = "us"
)
