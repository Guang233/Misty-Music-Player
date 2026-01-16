package com.guang.misty.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guang.misty.model.MistyPluginCapability
import com.guang.misty.model.MistySong
import com.guang.misty.service.LoadedPlugin
import com.guang.misty.service.PluginService
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
)

/**
 * 搜索 ViewModel
 */
class SearchViewModel : ViewModel() {
    
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()
    
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
    private fun addToHistory(keyword: String) {
        _state.update { state ->
            val newHistory = (listOf(keyword) + state.searchHistory.filter { it != keyword })
                .take(10)
            state.copy(searchHistory = newHistory)
        }
        // TODO: 持久化搜索历史
    }
    
    /**
     * 清除搜索历史
     */
    fun clearHistory() {
        _state.update { it.copy(searchHistory = emptyList()) }
        // TODO: 持久化
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
}
