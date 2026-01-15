package com.guang.misty.model

import kotlinx.serialization.Serializable

/**
 * 音频音质枚举。
 *
 * JS 端请返回与此枚举同名的字符串（例如 "LOSSLESS"），
 * 以便 kotlinx-serialization 能够正确反序列化。
 */
@Serializable
enum class MistyAudioQuality {
    STANDARD,
    HIGH,
    LOSSLESS,
    HI_RES,
    OTHER
}

/**
 * 音频资源描述，用于表示同一首歌的不同音质资源。
 */
@Serializable
data class MistyAudioResource(
    val quality: MistyAudioQuality,
    val url: String,
    val format: String? = null,        // 如 "mp3", "flac"
    val bitrateKbps: Int? = null,      // 比特率（kbps）
    val fileSizeBytes: Long? = null,   // 文件大小（字节）
    val md5: String? = null,           // 校验和（可选）
    val extras: Map<String, String> = emptyMap(), // 额外信息
)

/**
 * 音频资源集合，表示某首歌对应的多音质资源列表。
 *
 * 设计上与歌词模型中的 MistyLyricBundle 保持一致：
 * - songId 绑定到具体歌曲
 * - resources 为同一首歌的不同音质/格式资源
 */
@Serializable
data class MistyAudioResourceBundle(
    val songId: String,
    val resources: List<MistyAudioResource>,
)