package com.plshare.backend.infrastructure.google

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Token response from Google's OAuth 2.0 token endpoint.
 * https://oauth2.googleapis.com/token
 */
data class GoogleTokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("token_type") val tokenType: String,
    @JsonProperty("expires_in") val expiresIn: Int,
    @JsonProperty("refresh_token") val refreshToken: String? = null,
    @JsonProperty("scope") val scope: String? = null,
    @JsonProperty("id_token") val idToken: String? = null
)

/**
 * Normalised token set passed between layers.
 * Mirrors SpotifyTokenSet to keep adapter interfaces consistent.
 */
data class GoogleTokenSet(
    val accessToken: String,
    val refreshToken: String?,
    val tokenType: String,
    val expiresInSeconds: Long,
    val scope: String?,
    val idToken: String? = null
)

/**
 * User info from Google OpenID Connect userinfo endpoint.
 * https://openidconnect.googleapis.com/v1/userinfo
 */
data class GoogleUserInfo(
    @JsonProperty("sub") val sub: String,
    @JsonProperty("email") val email: String?,
    @JsonProperty("name") val name: String?,
    @JsonProperty("picture") val picture: String?
)
