package com.guang.misty.player

import com.guang.misty.model.MistySong
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.media.Media
import uk.co.caprica.vlcj.media.MediaEventAdapter
import uk.co.caprica.vlcj.media.MediaParsedStatus
import uk.co.caprica.vlcj.media.Meta
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter

/**
 * Desktop (JVM) 播放器实现
 * 使用 VLCJ (VLC Java 绑定)
 * 
 * 支持格式：MP3, AAC, FLAC, WAV, OGG, WMA, M4A 等几乎所有音频格式
 * 
 * 注意：需要用户系统安装 VLC 播放器（或 libvlc）
 */
class DesktopMistyPlayer : MistyPlayer {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()
    
    // 播放队列
    private val queue = mutableListOf<QueueItem>()
    
    // VLC 组件
    private var mediaPlayerFactory: MediaPlayerFactory? = null
    private var mediaPlayer: MediaPlayer? = null
    
    // 位置更新任务
    private var positionUpdateJob: Job? = null
    
    // 是否已初始化
    private var isInitialized = false
    private var initError: String? = null
    
    init {
        initializeVlc()
    }
    
    private fun initializeVlc() {
        try {
            // 创建 VLC 工厂，使用无头模式（不显示视频窗口）
            mediaPlayerFactory = MediaPlayerFactory(
                "--no-video",           // 禁用视频输出
                "--quiet",              // 安静模式
                "--no-metadata-network-access"  // 禁止网络访问元数据
            )
            
            mediaPlayer = mediaPlayerFactory?.mediaPlayers()?.newMediaPlayer()
            
            // 设置事件监听
            mediaPlayer?.events()?.addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
                override fun playing(mediaPlayer: MediaPlayer) {
                    _state.update { it.copy(
                        playbackState = PlaybackState.PLAYING,
                        isPlaying = true
                    ) }
                    startPositionUpdates()
                }
                
                override fun paused(mediaPlayer: MediaPlayer) {
                    _state.update { it.copy(
                        playbackState = PlaybackState.READY,
                        isPlaying = false
                    ) }
                    stopPositionUpdates()
                }
                
                override fun stopped(mediaPlayer: MediaPlayer) {
                    _state.update { it.copy(
                        playbackState = PlaybackState.IDLE,
                        isPlaying = false,
                        position = 0L
                    ) }
                    stopPositionUpdates()
                }
                
                override fun finished(mediaPlayer: MediaPlayer) {
                    stopPositionUpdates()
                    handlePlaybackEnded()
                }
                
                override fun error(mediaPlayer: MediaPlayer) {
                    _state.update { it.copy(
                        playbackState = PlaybackState.ERROR,
                        isPlaying = false,
                        error = "Playback error occurred"
                    ) }
                    stopPositionUpdates()
                }
                
                override fun lengthChanged(mediaPlayer: MediaPlayer, newLength: Long) {
                    _state.update { it.copy(duration = newLength) }
                }
                
                override fun buffering(mediaPlayer: MediaPlayer, newCache: Float) {
                    if (newCache < 100f) {
                        _state.update { it.copy(playbackState = PlaybackState.LOADING) }
                    }
                }
            })
            
            isInitialized = true
            
        } catch (e: Exception) {
            initError = "Failed to initialize VLC: ${e.message}. Please ensure VLC is installed on your system."
            _state.update { it.copy(
                playbackState = PlaybackState.ERROR,
                error = initError
            ) }
            e.printStackTrace()
        }
    }
    
    private fun handlePlaybackEnded() {
        val currentState = _state.value
        
        scope.launch(Dispatchers.Main) {
            when (currentState.repeatMode) {
                RepeatMode.ONE -> {
                    // 单曲循环
                    seekTo(0)
                    resume()
                }
                RepeatMode.ALL -> {
                    // 列表循环
                    if (currentState.currentIndex < queue.size - 1) {
                        next()
                    } else if (queue.isNotEmpty()) {
                        skipToIndex(0)
                    } else {
                        _state.update { it.copy(playbackState = PlaybackState.ENDED) }
                    }
                }
                RepeatMode.OFF -> {
                    // 不循环
                    if (currentState.currentIndex < queue.size - 1) {
                        next()
                    } else {
                        _state.update { it.copy(playbackState = PlaybackState.ENDED) }
                    }
                }
            }
        }
    }
    
    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.status().isPlaying) {
                        val position = player.status().time()
                        val buffered = (player.media()?.info()?.duration() ?: 0L)
                        _state.update { it.copy(
                            position = position,
                            bufferedPosition = buffered
                        ) }
                    }
                }
                delay(200L)
            }
        }
    }
    
    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }
    
    override fun play(song: MistySong, audioUrl: String, headers: Map<String, String>) {
        val item = QueueItem(song, audioUrl, headers)
        synchronized(queue) {
            queue.clear()
            queue.add(item)
        }
        
        _state.update { it.copy(
            playlist = listOf(song),
            currentIndex = 0
        ) }
        
        playItem(item, 0)
    }
    
    override fun setQueue(items: List<QueueItem>, startIndex: Int) {
        synchronized(queue) {
            queue.clear()
            queue.addAll(items)
        }
        
        _state.update { it.copy(
            playlist = items.map { it.song },
            currentIndex = if (items.isNotEmpty()) startIndex.coerceIn(0, items.size - 1) else -1
        ) }
        
        if (items.isNotEmpty() && startIndex in items.indices) {
            playItem(items[startIndex], startIndex)
        }
    }
    
    override fun addToQueue(item: QueueItem) {
        synchronized(queue) {
            queue.add(item)
        }
        _state.update { it.copy(playlist = queue.map { q -> q.song }) }
    }
    
    override fun addNext(item: QueueItem) {
        synchronized(queue) {
            val insertIndex = (_state.value.currentIndex + 1).coerceIn(0, queue.size)
            queue.add(insertIndex, item)
        }
        _state.update { it.copy(playlist = queue.map { q -> q.song }) }
    }
    
    override fun removeFromQueue(index: Int) {
        val currentIndex: Int
        val newIndex: Int
        val shouldPlayNew: Boolean
        
        synchronized(queue) {
            if (index !in queue.indices) return
            
            currentIndex = _state.value.currentIndex
            queue.removeAt(index)
            
            newIndex = when {
                queue.isEmpty() -> -1
                index < currentIndex -> currentIndex - 1
                index == currentIndex && index >= queue.size -> queue.size - 1
                else -> currentIndex
            }
            
            shouldPlayNew = index == currentIndex && queue.isNotEmpty() && newIndex in queue.indices
        }
        
        _state.update { it.copy(
            playlist = queue.map { q -> q.song },
            currentIndex = newIndex
        ) }
        
        if (shouldPlayNew) {
            playItem(queue[newIndex], newIndex)
        } else if (queue.isEmpty()) {
            stop()
        }
    }
    
    override fun clearQueue() {
        synchronized(queue) {
            queue.clear()
        }
        stop()
        _state.update { it.copy(
            playlist = emptyList(),
            currentIndex = -1,
            currentSong = null
        ) }
    }
    
    private fun playItem(item: QueueItem, index: Int) {
        if (!isInitialized) {
            _state.update { it.copy(
                playbackState = PlaybackState.ERROR,
                error = initError ?: "Player not initialized"
            ) }
            return
        }
        
        _state.update { it.copy(
            currentSong = item.song,
            currentIndex = index,
            playbackState = PlaybackState.LOADING,
            position = 0L,
            duration = 0L,
            error = null
        ) }
        
        scope.launch {
            try {
                // 构建 URL，如果有 headers 需要通过 VLC 选项传递
                val options = mutableListOf<String>()
                
                // 添加 HTTP 头
                if (item.headers.isNotEmpty()) {
                    val headerString = item.headers.entries.joinToString("\r\n") { "${it.key}: ${it.value}" }
                    options.add(":http-user-agent=Misty/1.0")
                    
                    // Cookie 特殊处理
                    item.headers["Cookie"]?.let { cookie ->
                        options.add(":http-cookies=$cookie")
                    }
                    
                    // 其他 headers
                    item.headers.filterKeys { it != "Cookie" }.forEach { (key, value) ->
                        options.add(":http-header=\"$key: $value\"")
                    }
                }
                
                // 播放媒体
                val result = if (options.isNotEmpty()) {
                    mediaPlayer?.media()?.play(item.audioUrl, *options.toTypedArray())
                } else {
                    mediaPlayer?.media()?.play(item.audioUrl)
                }
                
                if (result != true) {
                    _state.update { it.copy(
                        playbackState = PlaybackState.ERROR,
                        error = "Failed to start playback"
                    ) }
                }
                
            } catch (e: Exception) {
                _state.update { it.copy(
                    playbackState = PlaybackState.ERROR,
                    error = e.message ?: "Unknown error"
                ) }
                e.printStackTrace()
            }
        }
    }
    
    override fun playPause() {
        mediaPlayer?.let { player ->
            if (player.status().isPlaying) {
                pause()
            } else {
                resume()
            }
        }
    }
    
    override fun resume() {
        mediaPlayer?.controls()?.play()
    }
    
    override fun pause() {
        mediaPlayer?.controls()?.pause()
    }
    
    override fun stop() {
        mediaPlayer?.controls()?.stop()
        stopPositionUpdates()
        _state.update { it.copy(
            playbackState = PlaybackState.IDLE,
            isPlaying = false,
            position = 0L
        ) }
    }
    
    override fun seekTo(position: Long) {
        mediaPlayer?.controls()?.setTime(position)
        _state.update { it.copy(position = position) }
    }
    
    override fun previous() {
        val currentIndex = _state.value.currentIndex
        val shuffleEnabled = _state.value.shuffleEnabled
        
        // 如果播放超过 3 秒，回到开头
        if (_state.value.position > 3000) {
            seekTo(0)
            return
        }
        
        if (currentIndex > 0) {
            val newIndex = if (shuffleEnabled) {
                (0 until queue.size).filter { it != currentIndex }.randomOrNull() ?: (currentIndex - 1)
            } else {
                currentIndex - 1
            }
            skipToIndex(newIndex)
        } else if (_state.value.repeatMode == RepeatMode.ALL && queue.isNotEmpty()) {
            skipToIndex(queue.size - 1)
        }
    }
    
    override fun next() {
        val currentIndex = _state.value.currentIndex
        val shuffleEnabled = _state.value.shuffleEnabled
        
        if (currentIndex < queue.size - 1) {
            val newIndex = if (shuffleEnabled) {
                (0 until queue.size).filter { it != currentIndex }.randomOrNull() ?: (currentIndex + 1)
            } else {
                currentIndex + 1
            }
            skipToIndex(newIndex)
        } else if (_state.value.repeatMode == RepeatMode.ALL && queue.isNotEmpty()) {
            skipToIndex(0)
        }
    }
    
    override fun skipToIndex(index: Int) {
        if (index !in queue.indices) return
        playItem(queue[index], index)
    }
    
    override fun setRepeatMode(mode: RepeatMode) {
        _state.update { it.copy(repeatMode = mode) }
    }
    
    override fun setShuffleEnabled(enabled: Boolean) {
        _state.update { it.copy(shuffleEnabled = enabled) }
    }
    
    override fun release() {
        stopPositionUpdates()
        mediaPlayer?.release()
        mediaPlayerFactory?.release()
        mediaPlayer = null
        mediaPlayerFactory = null
        scope.cancel()
    }
    
    companion object {
        /**
         * 检查 VLC 是否可用
         */
        fun isVlcAvailable(): Boolean {
            return try {
                val factory = MediaPlayerFactory()
                factory.release()
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
