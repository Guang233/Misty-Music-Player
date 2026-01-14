package com.guang.misty.model

import kotlinx.serialization.Serializable

@Serializable
enum class MistyLyricType {
    ORIGINAL,     // 原文
    TRANSLATION,  // 译文
    ROMANIZATION  // 罗马音
}

@Serializable
enum class MistyLyricFormat {
    LINE_BY_LINE, // 逐行
    WORD_BY_WORD  // 逐字
}

@Serializable
data class MistyLyric(
    val content: String,             // 歌词文本内容
    val type: MistyLyricType,        // 类型
    val format: MistyLyricFormat     // 格式
)

@Serializable
data class MistyLyricBundle(
    val songId: String,
    val lyrics: List<MistyLyric>
)