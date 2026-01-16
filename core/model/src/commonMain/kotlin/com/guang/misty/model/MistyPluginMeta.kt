package com.guang.misty.model

import kotlinx.serialization.Serializable

/**
 * 插件能力枚举，用于声明插件实现了哪些功能。
 *
 * JS 侧应返回同名字符串（例如 "SEARCH"），方便 kotlinx-serialization 解析。
 */
@Serializable
enum class MistyPluginCapability {
    SEARCH,           // 搜索歌曲
    SEARCH_SUGGEST,   // 搜索联想词
    PLAYLIST,         // 获取歌单
    ALBUM,            // 获取专辑
    LYRICS,           // 获取歌词
    AUDIO_RESOURCES,  // 获取多音质音频资源
}

/**
 * 插件基础信息（元数据）。
 *
 * 注意：Misty 本身不提供、也不内置任何音源，只负责加载和运行由社区提供的脚本。
 * 插件作者有责任确保脚本的合法性和版权合规性。
 */
@Serializable
data class MistyPluginMeta(
    val id: String,                         // 插件 ID，对应 MistyPlugins 中的 key
    val name: String,                       // 插件名称（展示用）
    val author: String? = null,             // 作者
    val version: String? = null,            // 版本号（如 "1.0.0"）
    val description: String? = null,        // 简要描述
    val homepage: String? = null,           // 项目主页或仓库地址
    val license: String? = null,            // 许可证信息（例如 "MIT"）

    // 与音源平台相关的信息（可选）
    val sourceName: String? = null,         // 音源平台名称（例如 "Netease"）
    val sourceHomepage: String? = null,     // 平台官网或 API 文档

    // 声明插件支持的能力，便于 Misty 动态启用/禁用某些功能
    val capabilities: List<MistyPluginCapability> = emptyList(),

    // 支持的地区/区域标识（如 ["CN", "US"]），可用于 UI 提示
    val supportRegions: List<String> = emptyList(),

    // 插件作者可声明的额外免责声明/说明
    val disclaimer: String? = null,
)

