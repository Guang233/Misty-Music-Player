package com.guang.misty.model

import kotlinx.serialization.Serializable

@Serializable
data class MistyAlbum(
    val id: String,
    val source: String,
    val name: String,
    val artists: List<MistyArtist> = emptyList(), // 专辑艺术家（可能有多个）
    val coverUrl: String? = null,
    val trackCount: Int? = null,    // 歌曲总数
    val releaseDate: String? = null,// 发行日期
    val description: String? = null,// 专辑介绍
    val songs: List<MistySong>? = null, // 专辑内的歌曲列表（按需加载）
    val extras: Map<String, String> = emptyMap()
) {
    val globalId: String get() = "$source:album:$id"
}
