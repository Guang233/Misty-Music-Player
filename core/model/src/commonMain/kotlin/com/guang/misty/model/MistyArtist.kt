package com.guang.misty.model

import kotlinx.serialization.Serializable

@Serializable
data class MistyArtist (
    val id: String?,
    val source: String,
    val name: String,
    val coverUrl: String?,
) {
    val globalId: String get() = "$source:artist:$id"
}
