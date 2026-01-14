package com.guang.misty.model

import kotlinx.serialization.Serializable

@Serializable
data class MistySong (
    val id: String,
    val source: String,
    val name: String,
    val artists: List<MistyArtist>,
    val album: MistyAlbum? = null,
    val url: String? = null,
    val coverUrl: String? = null,
    val extras: Map<String, String> = emptyMap(),
) {
    val globalId: String get() = "$source:song:$id"
}
