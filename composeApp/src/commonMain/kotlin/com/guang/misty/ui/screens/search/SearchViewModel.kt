package com.guang.misty.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guang.misty.data.settings.MistySettings
import com.guang.misty.data.settings.createSettingsStorage
import com.guang.misty.model.MistyPluginCapability
import com.guang.misty.model.MistySong
import com.guang.misty.service.LoadedPlugin
import com.guang.misty.service.PluginService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 搜索状态
 */
data class SearchState(
    // 可用的搜索源（插件）
    val availableSources: List<LoadedPlugin> = emptyList(),
    // 当前选中的源 ID（null 表示搜索所有源）
    val selectedSourceId: String? = null,
    // 搜索历史
    val searchHistory: List<String> = emptyList(),
    // 是否正在加载源
    val isLoadingSources: Boolean = false,
    // 搜索结果状态
    val searchQuery: String = "",
    val searchResults: List<MistySong> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val currentPage: Int = 1,
    val hasMoreResults: Boolean = true,
    // 搜索联想词
    val suggestions: List<String> = emptyList(),
    val isLoadingSuggestions: Boolean = false,
)

/**
 * 搜索 ViewModel
 */
class SearchViewModel : ViewModel() {
    
    private val settingsStorage = createSettingsStorage()
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()
    
    // 联想词请求的防抖 Job
    private var suggestionsJob: Job? = null
    private val suggestDebounceMs = 300L
    
    init {
        // 监听插件服务状态
        viewModelScope.launch {
            PluginService.state.collect { serviceState ->
                val searchablePlugins = serviceState.loadedPlugins.filter { plugin ->
                    plugin.meta.capabilities.contains(MistyPluginCapability.SEARCH)
                }
                _state.update { it.copy(
                    availableSources = searchablePlugins,
                    isLoadingSources = serviceState.isLoading,
                    // 默认选中第一个源
                    selectedSourceId = it.selectedSourceId ?: searchablePlugins.firstOrNull()?.id
                ) }
            }
        }
        
        // 加载搜索历史
        viewModelScope.launch {
            settingsStorage.settingsFlow.collect { settings ->
                _state.update { it.copy(searchHistory = settings.searchHistory) }
            }
        }
        
        // 初始化插件服务
        PluginService.initialize()
    }
    
    /**
     * 选择搜索源
     */
    fun selectSource(sourceId: String?) {
        _state.update { it.copy(selectedSourceId = sourceId) }
    }
    
    /**
     * 搜索
     */
    fun search(keyword: String) {
        if (keyword.isBlank()) return
        
        viewModelScope.launch {
            _state.update { it.copy(
                searchQuery = keyword,
                isSearching = true,
                searchError = null,
                searchResults = emptyList(),
                currentPage = 1,
                hasMoreResults = true
            ) }
            
            // 添加到搜索历史
            addToHistory(keyword)
            
            try {
                val results = PluginService.search(
                    pluginId = _state.value.selectedSourceId,
                    keyword = keyword,
                    page = 1
                )
                
                _state.update { it.copy(
                    searchResults = results,
                    isSearching = false,
                    hasMoreResults = results.isNotEmpty()
                ) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isSearching = false,
                    searchError = e.message ?: "Search failed"
                ) }
            }
        }
    }
    
    /**
     * 加载更多结果
     */
    fun loadMore() {
        val currentState = _state.value
        if (currentState.isSearching || !currentState.hasMoreResults || currentState.searchQuery.isBlank()) {
            return
        }
        
        viewModelScope.launch {
            val nextPage = currentState.currentPage + 1
            _state.update { it.copy(isSearching = true) }
            
            try {
                val results = PluginService.search(
                    pluginId = currentState.selectedSourceId,
                    keyword = currentState.searchQuery,
                    page = nextPage
                )
                
                _state.update { it.copy(
                    searchResults = it.searchResults + results,
                    currentPage = nextPage,
                    isSearching = false,
                    hasMoreResults = results.isNotEmpty()
                ) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isSearching = false,
                    searchError = e.message
                ) }
            }
        }
    }
    
    /**
     * 添加到搜索历史
     */
    private suspend fun addToHistory(keyword: String) {
        settingsStorage.updateSettings { settings ->
            val newHistory = (listOf(keyword) + settings.searchHistory.filter { it != keyword })
                .take(MistySettings.MAX_SEARCH_HISTORY)
            settings.copy(searchHistory = newHistory)
        }
    }
    
    /**
     * 删除单条搜索历史
     */
    fun deleteHistoryItem(keyword: String) {
        viewModelScope.launch {
            settingsStorage.updateSettings { settings ->
                settings.copy(searchHistory = settings.searchHistory.filter { it != keyword })
            }
        }
    }
    
    /**
     * 清除所有搜索历史
     */
    fun clearHistory() {
        viewModelScope.launch {
            settingsStorage.updateSettings { settings ->
                settings.copy(searchHistory = emptyList())
            }
        }
    }
    
    /**
     * 清除搜索结果
     */
    fun clearResults() {
        _state.update { it.copy(
            searchQuery = "",
            searchResults = emptyList(),
            searchError = null,
            currentPage = 1,
            hasMoreResults = true
        ) }
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _state.update { it.copy(searchError = null) }
    }
    
    /**
     * 获取搜索联想词（带防抖）
     */
    fun fetchSuggestions(keyword: String) {
        // 取消之前的请求
        suggestionsJob?.cancel()
        
        // 如果关键词为空，清空联想词
        if (keyword.isBlank()) {
            _state.update { it.copy(suggestions = emptyList(), isLoadingSuggestions = false) }
            return
        }
        
        suggestionsJob = viewModelScope.launch {
            // 防抖延迟
            delay(suggestDebounceMs)
            
            val pluginId = _state.value.selectedSourceId ?: return@launch
            
            // 检查当前选中的插件是否支持联想词
            val plugin = _state.value.availableSources.find { it.id == pluginId }
            if (plugin?.meta?.capabilities?.contains(MistyPluginCapability.SEARCH_SUGGEST) != true) {
                _state.update { it.copy(suggestions = emptyList(), isLoadingSuggestions = false) }
                return@launch
            }
            
            _state.update { it.copy(isLoadingSuggestions = true) }
            
            try {
                val suggestions = PluginService.getSearchSuggestions(pluginId, keyword)
                _state.update { it.copy(
                    suggestions = suggestions,
                    isLoadingSuggestions = false
                ) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    suggestions = emptyList(),
                    isLoadingSuggestions = false
                ) }
            }
        }
    }
    
    /**
     * 清除联想词
     */
    fun clearSuggestions() {
        suggestionsJob?.cancel()
        _state.update { it.copy(suggestions = emptyList(), isLoadingSuggestions = false) }
    }
}
