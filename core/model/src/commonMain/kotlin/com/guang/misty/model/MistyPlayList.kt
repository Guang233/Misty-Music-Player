package com.guang.misty.model

import kotlinx.serialization.Serializable

@Serializable
data class MistyPlaylist(
    val id: String,
    val source: String,
    val name: String,
    val creator: String? = null,    // 创建者名字
    val coverUrl: String? = null,
    val description: String? = null,
    val songCount: Int? = null,
    val playCount: Long? = null,    // 播放量
    val updateTime: String? = null, // 最近更新时间
    val songs: List<MistySong> = emptyList(),
    val extras: Map<String, String> = emptyMap()
) {
    val globalId: String get() = "$source:playlist:$id"
}