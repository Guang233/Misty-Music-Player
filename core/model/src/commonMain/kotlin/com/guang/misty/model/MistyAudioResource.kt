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
    STANDARD,   // 标准音质
    HIGH,       // 高音质
    LOSSLESS,   // 无损音质
    HI_RES,     // Hi-Res 高解析度
}

/**
 * 音频资源描述，表示单个音质的音频资源。
 */
@Serializable
data class MistyAudioResource(
    val quality: MistyAudioQuality,     // 实际音质（可能与请求不同，表示降级）
    val url: String,
    val format: String? = null,         // 如 "mp3", "flac"
    val bitrateKbps: Int? = null,       // 比特率（kbps）
    val fileSizeBytes: Long? = null,    // 文件大小（字节）
    val md5: String? = null,            // 校验和（可选）
    val extras: Map<String, String> = emptyMap(), // 额外信息
)

/**
 * 音频资源请求结果。
 *
 * 用于处理「按指定音质请求」的场景：
 * - songId: 歌曲 ID
 * - requestedQuality: 请求的音质
 * - resource: 实际返回的资源（可能为 null 表示失败）
 *   - resource.quality: 实际返回的音质（可能与 requestedQuality 不同，表示降级）
 * - error: 错误信息（如果失败）
 *
 * 调用方可通过以下方式判断状态：
 * - resource != null && resource.quality == requestedQuality → 成功获取请求音质
 * - resource != null && resource.quality != requestedQuality → 降级（返回了其他音质）
 * - resource == null → 失败，查看 error 获取错误信息
 */
@Serializable
data class MistyAudioResourceResult(
    val songId: String,
    val requestedQuality: MistyAudioQuality,
    val resource: MistyAudioResource? = null,
    val error: String? = null,
)