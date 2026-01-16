package com.guang.misty.data.settings

import kotlinx.coroutines.flow.Flow

/**
 * 设置存储接口
 * 
 * 提供设置的读写功能，使用 expect/actual 实现跨平台
 */
interface SettingsStorage {
    /**
     * 设置数据流
     */
    val settingsFlow: Flow<MistySettings>
    
    /**
     * 获取当前设置
     */
    suspend fun getSettings(): MistySettings
    
    /**
     * 更新设置
     */
    suspend fun updateSettings(settings: MistySettings)
    
    /**
     * 更新部分设置
     */
    suspend fun updateSettings(update: (MistySettings) -> MistySettings)
}

/**
 * 插件存储接口
 */
interface PluginStorage {
    /**
     * 插件数据流
     */
    val pluginDataFlow: Flow<PluginStorageData>
    
    /**
     * 获取插件存储数据
     */
    suspend fun getPluginData(): PluginStorageData
    
    /**
     * 更新插件存储数据
     */
    suspend fun updatePluginData(data: PluginStorageData)
    
    /**
     * 获取插件目录路径
     */
    fun getPluginsDirectory(): String
    
    /**
     * 保存插件代码文件
     * @param fileName 文件名
     * @param content 插件代码内容
     * @return 保存是否成功
     */
    suspend fun savePluginFile(fileName: String, content: String): Boolean
    
    /**
     * 读取插件代码文件
     * @param fileName 文件名
     * @return 插件代码内容，如果不存在返回 null
     */
    suspend fun readPluginFile(fileName: String): String?
    
    /**
     * 删除插件文件
     * @param fileName 文件名
     * @return 删除是否成功
     */
    suspend fun deletePluginFile(fileName: String): Boolean
    
    /**
     * 列出所有插件文件
     */
    suspend fun listPluginFiles(): List<String>
}

/**
 * 创建设置存储实例
 */
expect fun createSettingsStorage(): SettingsStorage

/**
 * 创建插件存储实例
 */
expect fun createPluginStorage(): PluginStorage
