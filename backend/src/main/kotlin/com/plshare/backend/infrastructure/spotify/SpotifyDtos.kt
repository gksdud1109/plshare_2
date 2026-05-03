package com.plshare.backend.infrastructure.spotify

import com.fasterxml.jackson.annotation.JsonProperty

data class SpotifyTokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("token_type") val tokenType: String,
    @JsonProperty("expires_in") val expiresIn: Int,
    @JsonProperty("refresh_token") val refreshToken: String? = null,
    @JsonProperty("scope") val scope: String? = null
)

data class SpotifyTokenSet(
    val accessToken: String,
    val refreshToken: String?,
    val tokenType: String,
    val expiresInSeconds: Long,
    val scope: String?
)

data class SpotifyPlaylistResponse(
    val id: String,
    val name: String,
    val images: List<SpotifyImage>,
    val tracks: SpotifyTrackContainer
)

data class SpotifyImage(
    val url: String
)

data class SpotifyTrackContainer(
    val items: List<SpotifyTrackItem>
)

data class SpotifyTrackItem(
    val track: SpotifyTrack
)

data class SpotifyTrack(
    val id: String,
    val name: String,
    val artists: List<SpotifyArtist>,
    @JsonProperty("duration_ms") val durationMs: Int? = null,
    @JsonProperty("external_ids") val externalIds: Map<String, String>? = null
) {
    val isrc: String? get() = externalIds?.get("isrc")
}

data class SpotifyArtist(
    val name: String
)

data class SpotifyUserPlaylistsResponse(
    val items: List<SpotifyPlaylistResponse>
)
