package com.guang.misty.service

import com.guang.misty.data.settings.InstalledPlugin
import com.guang.misty.data.settings.PluginStorage
import com.guang.misty.data.settings.createPluginStorage
import com.guang.misty.engine.MistyJsEngine
import com.guang.misty.engine.MistyPluginManager
import com.guang.misty.engine.bridge.StandardMistyBridge
import com.guang.misty.engine.cookie.createCookieStorage
import com.guang.misty.engine.cookie.createLoginHandler
import com.guang.misty.model.*
import com.guang.misty.network.MistyHttpClient
import com.guang.misty.util.LogLevel
import com.guang.misty.util.MistyLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 已加载的插件信息
 */
data class LoadedPlugin(
    val id: String,
    val meta: MistyPluginMeta,
    val enabled: Boolean = true
)

/**
 * 插件服务状态
 */
data class PluginServiceState(
    val isInitialized: Boolean = false,
    val isLoading: Boolean = false,
    val loadedPlugins: List<LoadedPlugin> = emptyList(),
    val error: String? = null
)

/**
 * 全局插件服务
 * 负责管理插件引擎的生命周期和提供插件功能访问
 */
object PluginService {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    
    private val pluginStorage: PluginStorage = createPluginStorage()
    
    private var jsEngine: MistyJsEngine? = null
    private var pluginManager: MistyPluginManager? = null
    
    private val _state = MutableStateFlow(PluginServiceState())
    val state: StateFlow<PluginServiceState> = _state.asStateFlow()
    
    /**
     * 初始化插件服务
     */
    fun initialize() {
        scope.launch {
            loadAllPlugins()
        }
    }
    
    /**
     * 重新加载所有插件
     */
    suspend fun reloadPlugins() {
        loadAllPlugins()
    }
    
    /**
     * 加载所有启用的插件
     */
    private suspend fun loadAllPlugins() {
        mutex.withLock {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                // 确保引擎初始化
                if (jsEngine == null) {
                    val bridge = createBridge()
                    jsEngine = MistyJsEngine(bridge)
                    pluginManager = MistyPluginManager(jsEngine!!)
                }
                
                // 清除之前加载的所有插件，确保状态干净
                pluginManager?.clearAllPlugins()
                
                val loadedList = mutableListOf<LoadedPlugin>()
                
                // 读取已安装的插件
                val pluginData = pluginStorage.getPluginData()
                
                for (installed in pluginData.installedPlugins) {
                    if (!installed.enabled) continue
                    
                    try {
                        val code = pluginStorage.readPluginFile(installed.fileName)
                        if (code != null) {
                            pluginManager?.loadPlugin(installed.id, code)
                            val meta = pluginManager?.getPluginMeta(installed.id)
                            if (meta != null) {
                                loadedList.add(LoadedPlugin(
                                    id = installed.id,
                                    meta = meta,
                                    enabled = true
                                ))
                            }
                        }
                    } catch (e: Exception) {
                        MistyLogger.e("PluginService", "Failed to load plugin ${installed.id}: ${e.message}", e)
                    }
                }
                
                _state.value = PluginServiceState(
                    isInitialized = true,
                    isLoading = false,
                    loadedPlugins = loadedList,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
    
    /**
     * 创建 MistyBridge 实例
     */
    private fun createBridge(): StandardMistyBridge {
        val httpClient = MistyHttpClient()
        val cookieStorage = createCookieStorage()
        val loginHandler = createLoginHandler()
        
        // 日志回调，将插件日志桥接到 MistyLogger
        val logCallback: (String, String, String) -> Unit = { level, tag, message ->
            val logLevel = when (level.uppercase()) {
                "DEBUG" -> LogLevel.DEBUG
                "INFO" -> LogLevel.INFO
                "WARN", "WARNING" -> LogLevel.WARN
                "ERROR" -> LogLevel.ERROR
                else -> LogLevel.INFO
            }
            MistyLogger.log(logLevel, tag, message)
        }

        return StandardMistyBridge(httpClient, cookieStorage, loginHandler, logCallback)
    }
    
    /**
     * 搜索歌曲
     * @param pluginId 插件 ID（如为空则使用所有启用的插件）
     * @param keyword 搜索关键词
     * @param page 页码
     */
    suspend fun search(pluginId: String?, keyword: String, page: Int = 1): List<MistySong> {
        val manager = pluginManager ?: return emptyList()
        
        return try {
            if (pluginId != null) {
                manager.search(pluginId, keyword, page)
            } else {
                // 搜索所有启用的插件
                val results = mutableListOf<MistySong>()
                for (plugin in _state.value.loadedPlugins) {
                    if (plugin.meta.capabilities.contains(MistyPluginCapability.SEARCH)) {
                        try {
                            results.addAll(manager.search(plugin.id, keyword, page))
                        } catch (e: Exception) {
                            MistyLogger.e("PluginService", "Search failed for plugin ${plugin.id}: ${e.message}", e)
                        }
                    }
                }
                results
            }
        } catch (e: Exception) {
            MistyLogger.e("PluginService", "Search failed: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * 获取音频资源
     */
    suspend fun getAudioResource(
        pluginId: String,
        songId: String,
        quality: MistyAudioQuality
    ): MistyAudioResourceResult {
        val manager = pluginManager ?: return MistyAudioResourceResult(
            songId = songId,
            requestedQuality = quality,
            resource = null,
            error = "Plugin service not initialized"
        )
        
        return manager.getAudioResource(pluginId, songId, quality)
    }
    
    /**
     * 获取歌词
     */
    suspend fun getLyrics(pluginId: String, songId: String): MistyLyricBundle? {
        val manager = pluginManager ?: return null
        
        return try {
            manager.getLyrics(pluginId, songId)
        } catch (e: Exception) {
            MistyLogger.e("PluginService", "Get lyrics failed: ${e.message}", e)
            null
        }
    }
    
    /**
     * 获取歌单详情
     */
    suspend fun getPlaylist(pluginId: String, playlistId: String): MistyPlaylist? {
        val manager = pluginManager ?: return null
        
        return try {
            manager.getPlaylist(pluginId, playlistId)
        } catch (e: Exception) {
            MistyLogger.e("PluginService", "Get playlist failed: ${e.message}", e)
            null
        }
    }
    
    /**
     * 获取专辑详情
     */
    suspend fun getAlbum(pluginId: String, albumId: String): MistyAlbum? {
        val manager = pluginManager ?: return null
        
        return try {
            manager.getAlbum(pluginId, albumId)
        } catch (e: Exception) {
            MistyLogger.e("PluginService", "Get album failed: ${e.message}", e)
            null
        }
    }
    
    /**
     * 获取搜索联想词
     * @param pluginId 插件 ID
     * @param keyword 当前输入的关键词
     */
    suspend fun getSearchSuggestions(pluginId: String, keyword: String): List<String> {
        val manager = pluginManager ?: return emptyList()
        
        return try {
            manager.getSearchSuggestions(pluginId, keyword)
        } catch (e: Exception) {
            MistyLogger.e("PluginService", "Get search suggestions failed: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * 获取有搜索能力的插件列表
     */
    fun getSearchablePlugins(): List<LoadedPlugin> {
        return _state.value.loadedPlugins.filter { 
            it.meta.capabilities.contains(MistyPluginCapability.SEARCH) 
        }
    }
    
    /**
     * 获取有搜索联想能力的插件列表
     */
    fun getSuggestablePlugins(): List<LoadedPlugin> {
        return _state.value.loadedPlugins.filter { 
            it.meta.capabilities.contains(MistyPluginCapability.SEARCH_SUGGEST) 
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        jsEngine?.close()
        jsEngine = null
        pluginManager = null
        _state.value = PluginServiceState()
    }
}
