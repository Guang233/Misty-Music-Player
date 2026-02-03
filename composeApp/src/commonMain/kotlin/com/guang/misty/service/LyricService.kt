package com.guang.misty.service

import com.guang.misty.model.*
import com.guang.misty.util.MistyLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 歌词服务
 * 
 * 负责：
 * - 从插件或本地加载歌词
 * - 根据播放位置计算当前行/字索引
 * - 管理歌词显示模式
 */
object LyricService {
    
    private const val TAG = "LyricService"
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // 当前歌词状态
    private val _state = MutableStateFlow<LyricState>(LyricState.Idle)
    val state: StateFlow<LyricState> = _state.asStateFlow()
    
    // 当前歌词列表（方便直接访问）
    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()
    
    // 当前播放行索引
    private val _currentLineIndex = MutableStateFlow(-1)
    val currentLineIndex: StateFlow<Int> = _currentLineIndex.asStateFlow()
    
    // 当前播放字索引（逐字模式）
    private val _currentWordIndex = MutableStateFlow(-1)
    val currentWordIndex: StateFlow<Int> = _currentWordIndex.asStateFlow()
    
    // 当前播放位置（毫秒）
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    // 显示模式
    private val _displayMode = MutableStateFlow(LyricDisplayMode.DUAL)
    val displayMode: StateFlow<LyricDisplayMode> = _displayMode.asStateFlow()
    
    // 是否有译文
    private val _hasTranslation = MutableStateFlow(false)
    val hasTranslation: StateFlow<Boolean> = _hasTranslation.asStateFlow()
    
    // 是否有罗马音
    private val _hasRomanization = MutableStateFlow(false)
    val hasRomanization: StateFlow<Boolean> = _hasRomanization.asStateFlow()
    
    // 当前加载的歌曲 ID（用于避免重复加载）
    private var currentSongId: String? = null
    
    /**
     * 加载歌词
     * 
     * @param song 歌曲信息
     * @param pluginId 插件 ID（可选，如果为空则只尝试本地加载）
     */
    fun loadLyrics(song: MistySong, pluginId: String? = null) {
        // 避免重复加载
        if (song.globalId == currentSongId && _state.value is LyricState.Success) {
            return
        }
        
        currentSongId = song.globalId
        _state.value = LyricState.Loading
        
        scope.launch {
            try {
                var lines: List<LyricLine>? = null
                
                // 1. 尝试从插件获取
                if (pluginId != null && song.id != null) {
                    try {
                        val bundle = PluginService.getLyrics(pluginId, song.id)
                        if (bundle != null && bundle.lyrics.isNotEmpty()) {
                            lines = LrcParser.fromBundle(bundle)
                            MistyLogger.d(TAG, "Loaded lyrics from plugin: ${lines.size} lines")
                        }
                    } catch (e: Exception) {
                        MistyLogger.w(TAG, "Failed to load lyrics from plugin: ${e.message}")
                    }
                }
                
                // 2. TODO: 尝试从本地加载（暂时跳过，后续实现）
                // if (lines == null) {
                //     lines = loadLocalLyrics(song)
                // }
                
                // 3. 更新状态
                if (lines != null && lines.isNotEmpty()) {
                    val hasTranslation = lines.any { it.translation != null }
                    val hasRomanization = lines.any { it.romanization != null }
                    
                    _lyrics.value = lines
                    _hasTranslation.value = hasTranslation
                    _hasRomanization.value = hasRomanization
                    _currentLineIndex.value = -1
                    _currentWordIndex.value = -1
                    
                    _state.value = LyricState.Success(
                        lines = lines,
                        hasTranslation = hasTranslation,
                        hasRomanization = hasRomanization
                    )
                    
                    MistyLogger.i(TAG, "Lyrics loaded: ${lines.size} lines, " +
                            "hasTranslation=$hasTranslation, hasRomanization=$hasRomanization")
                } else {
                    _lyrics.value = emptyList()
                    _hasTranslation.value = false
                    _hasRomanization.value = false
                    _state.value = LyricState.NoLyrics
                    MistyLogger.i(TAG, "No lyrics found for song: ${song.name}")
                }
            } catch (e: Exception) {
                MistyLogger.e(TAG, "Failed to load lyrics: ${e.message}", e)
                _state.value = LyricState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * 从 LRC 文本加载歌词（用于本地文件）
     */
    fun loadFromLrc(lrcContent: String) {
        scope.launch {
            try {
                val lines = LrcParser.parse(lrcContent)
                
                if (lines.isNotEmpty()) {
                    val hasTranslation = lines.any { it.translation != null }
                    val hasRomanization = lines.any { it.romanization != null }
                    
                    _lyrics.value = lines
                    _hasTranslation.value = hasTranslation
                    _hasRomanization.value = hasRomanization
                    _currentLineIndex.value = -1
                    _currentWordIndex.value = -1
                    
                    _state.value = LyricState.Success(
                        lines = lines,
                        hasTranslation = hasTranslation,
                        hasRomanization = hasRomanization
                    )
                } else {
                    _lyrics.value = emptyList()
                    _state.value = LyricState.NoLyrics
                }
            } catch (e: Exception) {
                MistyLogger.e(TAG, "Failed to parse LRC: ${e.message}", e)
                _state.value = LyricState.Error(e.message ?: "Parse error")
            }
        }
    }
    
    /**
     * 更新播放位置
     * 
     * 根据播放位置计算当前行和字的索引
     */
    fun updatePosition(positionMs: Long) {
        _currentPosition.value = positionMs
        
        val lines = _lyrics.value
        if (lines.isEmpty()) {
            _currentLineIndex.value = -1
            _currentWordIndex.value = -1
            return
        }
        
        // 查找当前行（最后一个开始时间 <= 当前位置的行）
        val lineIndex = lines.indexOfLast { positionMs >= it.startTimeMs }
        _currentLineIndex.value = lineIndex
        
        // 计算当前字索引（仅逐字模式）
        if (lineIndex >= 0) {
            val currentLine = lines[lineIndex]
            _currentWordIndex.value = currentLine.getCurrentWordIndex(positionMs)
        } else {
            _currentWordIndex.value = -1
        }
    }
    
    /**
     * 设置显示模式
     */
    fun setDisplayMode(mode: LyricDisplayMode) {
        _displayMode.value = mode
    }
    
    /**
     * 切换到下一个显示模式
     */
    fun toggleDisplayMode() {
        val current = _displayMode.value
        val hasTranslation = _hasTranslation.value
        val hasRomanization = _hasRomanization.value
        
        // 根据可用内容决定可切换的模式
        val availableModes = mutableListOf(LyricDisplayMode.ORIGINAL)
        if (hasTranslation) {
            availableModes.add(LyricDisplayMode.TRANSLATION)
            availableModes.add(LyricDisplayMode.DUAL)
        }
        if (hasRomanization) {
            availableModes.add(LyricDisplayMode.ROMANIZATION)
        }
        
        // 切换到下一个模式
        val currentIndex = availableModes.indexOf(current)
        val nextIndex = if (currentIndex < 0) 0 else (currentIndex + 1) % availableModes.size
        _displayMode.value = availableModes[nextIndex]
    }
    
    /**
     * 清除歌词
     */
    fun clear() {
        currentSongId = null
        _lyrics.value = emptyList()
        _currentLineIndex.value = -1
        _currentWordIndex.value = -1
        _currentPosition.value = 0L
        _hasTranslation.value = false
        _hasRomanization.value = false
        _state.value = LyricState.Idle
    }
    
    /**
     * 根据行索引获取时间戳（用于点击跳转）
     */
    fun getTimeForLine(lineIndex: Int): Long? {
        val lines = _lyrics.value
        return lines.getOrNull(lineIndex)?.startTimeMs
    }
    
    /**
     * 获取当前行的歌词文本（根据显示模式）
     */
    fun getCurrentDisplayText(lineIndex: Int): String? {
        val lines = _lyrics.value
        val line = lines.getOrNull(lineIndex) ?: return null
        
        return when (_displayMode.value) {
            LyricDisplayMode.ORIGINAL -> line.text
            LyricDisplayMode.TRANSLATION -> line.translation ?: line.text
            LyricDisplayMode.ROMANIZATION -> line.romanization ?: line.text
            LyricDisplayMode.DUAL -> line.text
        }
    }
    
    /**
     * 获取当前行的副文本（双语模式下的译文）
     */
    fun getCurrentSubText(lineIndex: Int): String? {
        if (_displayMode.value != LyricDisplayMode.DUAL) return null
        
        val lines = _lyrics.value
        val line = lines.getOrNull(lineIndex) ?: return null
        return line.translation
    }
}
