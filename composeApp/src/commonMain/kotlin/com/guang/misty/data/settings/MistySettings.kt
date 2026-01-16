package com.guang.misty.data.settings

import com.guang.misty.ui.theme.ThemeColor
import com.guang.misty.ui.theme.ThemeMode
import kotlinx.serialization.Serializable

/**
 * Misty 应用设置数据
 * 
 * 使用 kotlinx-serialization 序列化为 JSON 存储
 */
@Serializable
data class MistySettings(
    // 主题设置
    val themeMode: String = ThemeMode.SYSTEM.name,
    val themeColor: String = ThemeColor.MIST_BLUE.name,
    
    // 播放设置
    val defaultAudioQuality: String = "HIGH",
    
    // 下载设置
    val downloadPath: String = "",
    val downloadQuality: String = "LOSSLESS",
    val wifiOnlyDownload: Boolean = true,
    
    // 插件设置
    val enabledPlugins: List<String> = emptyList(),
    val pluginOrder: List<String> = emptyList(),
    
    // 搜索历史
    val searchHistory: List<String> = emptyList(),
) {
    companion object {
        val Default = MistySettings()
        const val MAX_SEARCH_HISTORY = 20
    }
}

/**
 * 插件安装信息
 */
@Serializable
data class InstalledPlugin(
    val id: String,
    val fileName: String,
    val enabled: Boolean = true,
    val installedAt: Long = System.currentTimeMillis(),
    val lastUpdatedAt: Long = System.currentTimeMillis(),
)

/**
 * 插件存储数据
 */
@Serializable
data class PluginStorageData(
    val installedPlugins: List<InstalledPlugin> = emptyList(),
)
