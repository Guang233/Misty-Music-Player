package com.guang.misty.model

import kotlinx.serialization.Serializable

/**
 * 逐字歌词中的单个字/词
 */
@Serializable
data class LyricWord(
    val startTimeMs: Long,  // 开始时间（毫秒）
    val endTimeMs: Long,    // 结束时间（毫秒）
    val text: String        // 文字内容
)

/**
 * 一行歌词（支持逐行和逐字两种格式）
 * 
 * - 逐行模式：words 为 null，使用 text 显示
 * - 逐字模式：words 不为 null，使用 words 逐字高亮显示
 */
@Serializable
data class LyricLine(
    val startTimeMs: Long,                // 本行开始时间
    val endTimeMs: Long,                  // 本行结束时间（下一行开始前）
    val text: String,                     // 完整文本（逐行模式用，或逐字模式的完整文本）
    val words: List<LyricWord>? = null,   // 逐字列表（null 表示逐行模式）
    val translation: String? = null,      // 译文
    val romanization: String? = null      // 罗马音
) {
    /**
     * 是否为逐字歌词
     */
    val isWordByWord: Boolean get() = !words.isNullOrEmpty()
    
    /**
     * 根据播放位置获取当前高亮的字索引
     * @return -1 表示没有字需要高亮（还没开始或已结束）
     */
    fun getCurrentWordIndex(positionMs: Long): Int {
        if (words == null) return -1
        return words.indexOfLast { positionMs >= it.startTimeMs }
    }
    
    /**
     * 获取当前字的进度（0.0 ~ 1.0）
     * 用于实现渐变高亮效果
     */
    fun getCurrentWordProgress(positionMs: Long): Float {
        if (words == null) return 0f
        val wordIndex = getCurrentWordIndex(positionMs)
        if (wordIndex < 0) return 0f
        val word = words[wordIndex]
        val duration = word.endTimeMs - word.startTimeMs
        if (duration <= 0) return 1f
        return ((positionMs - word.startTimeMs).toFloat() / duration).coerceIn(0f, 1f)
    }
}

/**
 * 歌词显示模式
 */
enum class LyricDisplayMode {
    ORIGINAL,      // 仅原文
    TRANSLATION,   // 仅译文
    ROMANIZATION,  // 仅罗马音
    DUAL           // 原文 + 译文（默认）
}

/**
 * 歌词加载状态
 */
sealed class LyricState {
    /** 空闲状态（无歌曲） */
    data object Idle : LyricState()
    
    /** 加载中 */
    data object Loading : LyricState()
    
    /** 加载成功 */
    data class Success(
        val lines: List<LyricLine>,
        val hasTranslation: Boolean,
        val hasRomanization: Boolean
    ) : LyricState()
    
    /** 无歌词 */
    data object NoLyrics : LyricState()
    
    /** 加载失败 */
    data class Error(val message: String) : LyricState()
}
