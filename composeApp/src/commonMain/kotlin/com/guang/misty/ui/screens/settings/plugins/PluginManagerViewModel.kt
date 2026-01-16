package com.guang.misty.ui.screens.settings.plugins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guang.misty.data.settings.InstalledPlugin
import com.guang.misty.data.settings.PluginStorage
import com.guang.misty.data.settings.PluginStorageData
import com.guang.misty.data.settings.createPluginStorage
import com.guang.misty.engine.MistyJsEngine
import com.guang.misty.engine.MistyPluginManager
import com.guang.misty.engine.bridge.MistyBridge
import com.guang.misty.model.MistyPluginMeta
import com.guang.misty.network.MistyHttpClient
import com.guang.misty.service.PluginService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 插件错误类型（用于多语言支持）
 */
sealed class PluginError {
    data object FileNotFound : PluginError()
    data class LoadFailed(val message: String) : PluginError()
    data object SaveFailed : PluginError()
    data class ImportFailed(val message: String) : PluginError()
    data class DeleteFailed(val message: String) : PluginError()
    data object IdNotFound : PluginError()
    data class LoadListFailed(val message: String) : PluginError()
}

/**
 * 插件 UI 状态
 */
data class PluginUiState(
    val id: String,
    val fileName: String,
    val meta: MistyPluginMeta? = null,
    val enabled: Boolean = true,
    val isLoading: Boolean = false,
    val error: PluginError? = null,
)

/**
 * 插件管理页面状态
 */
data class PluginManagementState(
    val plugins: List<PluginUiState> = emptyList(),
    val isLoading: Boolean = false,
    val isImporting: Boolean = false,
    val error: PluginError? = null,
    val importDialogVisible: Boolean = false,
    val importText: String = "",
)

/**
 * 插件管理 ViewModel
 */
class PluginManagerViewModel : ViewModel() {
    
    private val pluginStorage: PluginStorage = createPluginStorage()
    
    // 插件引擎和管理器（懒加载）
    private var jsEngine: MistyJsEngine? = null
    private var pluginManager: MistyPluginManager? = null
    
    private val _state = MutableStateFlow(PluginManagementState())
    val state: StateFlow<PluginManagementState> = _state.asStateFlow()
    
    init {
        loadPlugins()
    }
    
    /**
     * 初始化插件引擎
     */
    private fun ensureEngineInitialized() {
        if (jsEngine == null) {
            val bridge = createBridge()
            jsEngine = MistyJsEngine(bridge)
            pluginManager = MistyPluginManager(jsEngine!!)
        }
    }
    
    /**
     * 创建 MistyBridge 实例
     */
    private fun createBridge(): MistyBridge {
        return object : MistyBridge {
            private val httpClient = MistyHttpClient()
            
            override suspend fun networkRequest(json: String): String {
                return httpClient.execute(json)
            }
            
            override fun log(level: String, msg: String) {
                println("[$level] $msg")
            }
        }
    }
    
    /**
     * 加载已安装的插件列表
     */
    fun loadPlugins() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                val pluginData = pluginStorage.getPluginData()
                val pluginStates = pluginData.installedPlugins.map { installed ->
                    PluginUiState(
                        id = installed.id,
                        fileName = installed.fileName,
                        enabled = installed.enabled,
                        isLoading = false
                    )
                }
                
                _state.update { it.copy(plugins = pluginStates, isLoading = false) }
                
                // 尝试加载每个启用的插件并获取元信息
                pluginStates.filter { it.enabled }.forEach { plugin ->
                    loadPluginMeta(plugin.id, plugin.fileName)
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(isLoading = false, error = PluginError.LoadListFailed(e.message ?: "Unknown error")) 
                }
            }
        }
    }
    
    /**
     * 加载单个插件的元信息
     */
    private fun loadPluginMeta(pluginId: String, fileName: String) {
        viewModelScope.launch {
            updatePluginState(pluginId) { it.copy(isLoading = true, error = null) }
            
            try {
                ensureEngineInitialized()
                
                val code = pluginStorage.readPluginFile(fileName)
                if (code == null) {
                    updatePluginState(pluginId) { 
                        it.copy(isLoading = false, error = PluginError.FileNotFound) 
                    }
                    return@launch
                }
                
                // 加载插件到引擎
                pluginManager?.loadPlugin(pluginId, code)
                
                // 获取元信息
                val meta = pluginManager?.getPluginMeta(pluginId)
                
                updatePluginState(pluginId) { 
                    it.copy(isLoading = false, meta = meta, error = null) 
                }
            } catch (e: Exception) {
                updatePluginState(pluginId) { 
                    it.copy(isLoading = false, error = PluginError.LoadFailed(e.message ?: "Unknown error")) 
                }
            }
        }
    }
    
    /**
     * 显示导入对话框
     */
    fun showImportDialog() {
        _state.update { it.copy(importDialogVisible = true, importText = "") }
    }
    
    /**
     * 隐藏导入对话框
     */
    fun hideImportDialog() {
        _state.update { it.copy(importDialogVisible = false, importText = "") }
    }
    
    /**
     * 更新导入文本
     */
    fun updateImportText(text: String) {
        _state.update { it.copy(importText = text) }
    }
    
    /**
     * 从文本导入插件
     */
    fun importPluginFromText(code: String) {
        viewModelScope.launch {
            _state.update { it.copy(isImporting = true, error = null) }
            
            try {
                ensureEngineInitialized()
                
                // 先尝试解析插件获取 ID
                val tempPluginId = "temp_import_${System.currentTimeMillis()}"
                pluginManager?.loadPlugin(tempPluginId, code)
                
                // 获取已加载的插件 ID 列表
                val loadedIds = pluginManager?.listPluginIds() ?: emptyList()
                
                // 找出新加载的插件（排除 temp 开头的）
                val newPluginId = loadedIds.find { 
                    !it.startsWith("temp_") && 
                    _state.value.plugins.none { p -> p.id == it }
                }
                
                if (newPluginId == null) {
                    // 尝试从代码中提取插件 ID
                    val extractedId = extractPluginIdFromCode(code)
                    if (extractedId != null) {
                        saveAndRegisterPlugin(extractedId, code)
                    } else {
                        _state.update { 
                            it.copy(isImporting = false, error = PluginError.IdNotFound) 
                        }
                    }
                } else {
                    saveAndRegisterPlugin(newPluginId, code)
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(isImporting = false, error = PluginError.ImportFailed(e.message ?: "Unknown error")) 
                }
            }
        }
    }
    
    /**
     * 从代码中提取插件 ID
     */
    private fun extractPluginIdFromCode(code: String): String? {
        // 尝试匹配 MistyPlugins['xxx'] 或 MistyPlugins["xxx"]
        val regex = Regex("""MistyPlugins\s*\[\s*['"]([^'"]+)['"]\s*\]""")
        return regex.find(code)?.groupValues?.getOrNull(1)
    }
    
    /**
     * 保存并注册插件
     */
    private suspend fun saveAndRegisterPlugin(pluginId: String, code: String) {
        val fileName = "${pluginId}.js"
        
        // 保存插件文件
        val saved = pluginStorage.savePluginFile(fileName, code)
        if (!saved) {
            _state.update { 
                it.copy(isImporting = false, error = PluginError.SaveFailed) 
            }
            return
        }
        
        // 获取元信息
        val meta = try {
            pluginManager?.getPluginMeta(pluginId)
        } catch (e: Exception) {
            null
        }
        
        // 更新存储数据
        val currentData = pluginStorage.getPluginData()
        val existingIndex = currentData.installedPlugins.indexOfFirst { it.id == pluginId }
        
        val updatedPlugins = if (existingIndex >= 0) {
            // 更新现有插件
            currentData.installedPlugins.toMutableList().apply {
                this[existingIndex] = this[existingIndex].copy(
                    lastUpdatedAt = System.currentTimeMillis()
                )
            }
        } else {
            // 添加新插件
            currentData.installedPlugins + InstalledPlugin(
                id = pluginId,
                fileName = fileName,
                enabled = true
            )
        }
        
        pluginStorage.updatePluginData(currentData.copy(installedPlugins = updatedPlugins))
        
        // 更新 UI 状态
        val newPluginState = PluginUiState(
            id = pluginId,
            fileName = fileName,
            meta = meta,
            enabled = true,
            isLoading = false
        )
        
        _state.update { state ->
            val existingPluginIndex = state.plugins.indexOfFirst { it.id == pluginId }
            val newPlugins = if (existingPluginIndex >= 0) {
                state.plugins.toMutableList().apply {
                    this[existingPluginIndex] = newPluginState
                }
            } else {
                state.plugins + newPluginState
            }
            state.copy(
                plugins = newPlugins,
                isImporting = false,
                importDialogVisible = false,
                importText = ""
            )
        }
        
        // 通知 PluginService 重新加载插件
        PluginService.reloadPlugins()
    }
    
    /**
     * 切换插件启用状态
     */
    fun togglePluginEnabled(pluginId: String) {
        viewModelScope.launch {
            val currentState = _state.value.plugins.find { it.id == pluginId } ?: return@launch
            val newEnabled = !currentState.enabled
            
            // 更新存储
            val currentData = pluginStorage.getPluginData()
            val updatedPlugins = currentData.installedPlugins.map {
                if (it.id == pluginId) it.copy(enabled = newEnabled) else it
            }
            pluginStorage.updatePluginData(currentData.copy(installedPlugins = updatedPlugins))
            
            // 更新 UI 状态
            updatePluginState(pluginId) { it.copy(enabled = newEnabled) }
            
            // 如果启用，尝试加载插件
            if (newEnabled) {
                loadPluginMeta(pluginId, currentState.fileName)
            }
            
            // 通知 PluginService 重新加载插件
            PluginService.reloadPlugins()
        }
    }
    
    /**
     * 删除插件
     */
    fun deletePlugin(pluginId: String) {
        viewModelScope.launch {
            try {
                val plugin = _state.value.plugins.find { it.id == pluginId } ?: return@launch
                
                // 删除文件
                pluginStorage.deletePluginFile(plugin.fileName)
                
                // 更新存储
                val currentData = pluginStorage.getPluginData()
                val updatedPlugins = currentData.installedPlugins.filter { it.id != pluginId }
                pluginStorage.updatePluginData(currentData.copy(installedPlugins = updatedPlugins))
                
                // 更新 UI 状态
                _state.update { state ->
                    state.copy(plugins = state.plugins.filter { it.id != pluginId })
                }
                
                // 通知 PluginService 重新加载插件
                PluginService.reloadPlugins()
            } catch (e: Exception) {
                _state.update { it.copy(error = PluginError.DeleteFailed(e.message ?: "Unknown error")) }
            }
        }
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    /**
     * 更新单个插件的状态
     */
    private fun updatePluginState(pluginId: String, update: (PluginUiState) -> PluginUiState) {
        _state.update { state ->
            state.copy(
                plugins = state.plugins.map {
                    if (it.id == pluginId) update(it) else it
                }
            )
        }
    }
    
    /**
     * 获取已加载并启用的插件管理器
     * 供外部使用（如搜索等功能）
     */
    fun getPluginManager(): MistyPluginManager? {
        ensureEngineInitialized()
        return pluginManager
    }
    
    override fun onCleared() {
        super.onCleared()
        jsEngine?.close()
    }
}
