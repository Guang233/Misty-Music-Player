package com.guang.misty.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.guang.misty.model.MistySong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.core.net.toUri

/**
 * Android 播放器实现
 * 使用 Media3 ExoPlayer + MediaSession
 * 
 * 支持：
 * - 通知栏播放控制
 * - 锁屏显示
 * - 蓝牙/耳机控制
 * - 后台播放
 */
@OptIn(UnstableApi::class)
class AndroidMistyPlayer(
    private val context: Context
) : MistyPlayer {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionUpdateJob: Job? = null
    
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()
    
    // 播放队列
    private val queue = mutableListOf<QueueItem>()
    
    // MediaController 用于与 MediaSession 通信
    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    
    // 直接使用的 ExoPlayer（当服务尚未启动时）
    private var localPlayer: ExoPlayer? = null
    
    // 当前使用的 Player（可能是服务的共享 player 或本地 player）
    private val player: Player?
        get() = MistyPlaybackService.sharedPlayer ?: localPlayer
    
    init {
        // 启动媒体服务
        startMediaService()
    }
    
    private fun startMediaService() {
        // 启动前台服务
        val serviceIntent = Intent(context, MistyPlaybackService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        
        // 连接到 MediaSession
        val sessionToken = SessionToken(context, ComponentName(context, MistyPlaybackService::class.java))
        mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        mediaControllerFuture?.addListener({
            try {
                mediaController = mediaControllerFuture?.get()
                setupPlayerListener()
            } catch (e: Exception) {
                // 连接失败，使用本地播放器
                setupLocalPlayer()
            }
        }, MoreExecutors.directExecutor())
    }
    
    private fun setupLocalPlayer() {
        if (localPlayer != null) return
        
        localPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        
        setupPlayerListener()
    }
    
    private fun setupPlayerListener() {
        val currentPlayer = player ?: return
        
        currentPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val newState = when (playbackState) {
                    Player.STATE_IDLE -> PlaybackState.IDLE
                    Player.STATE_BUFFERING -> PlaybackState.LOADING
                    Player.STATE_READY -> PlaybackState.READY
                    Player.STATE_ENDED -> PlaybackState.ENDED
                    else -> PlaybackState.IDLE
                }
                
                _state.update { it.copy(
                    playbackState = newState,
                    duration = if (currentPlayer.duration > 0) currentPlayer.duration else 0L
                ) }
                
                if (playbackState == Player.STATE_ENDED) {
                    handlePlaybackEnded()
                }
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(
                    isPlaying = isPlaying,
                    playbackState = if (isPlaying) PlaybackState.PLAYING else _state.value.playbackState
                ) }
                
                if (isPlaying) {
                    startPositionUpdates()
                } else {
                    stopPositionUpdates()
                }
            }
            
            override fun onPlayerError(error: PlaybackException) {
                _state.update { it.copy(
                    playbackState = PlaybackState.ERROR,
                    error = error.message ?: "Unknown playback error"
                ) }
            }
            
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                // 可以在这里更新 UI 显示的元数据
            }
        })
    }
    
    private fun handlePlaybackEnded() {
        val currentState = _state.value
        scope.launch {
            when (currentState.repeatMode) {
                RepeatMode.ONE -> {
                    player?.seekTo(0)
                    player?.play()
                }
                RepeatMode.ALL -> {
                    if (currentState.currentIndex < queue.size - 1) {
                        next()
                    } else if (queue.isNotEmpty()) {
                        skipToIndex(0)
                    }
                }
                RepeatMode.OFF -> {
                    if (currentState.currentIndex < queue.size - 1) {
                        next()
                    }
                }
            }
        }
    }
    
    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (isActive) {
                player?.let { p ->
                    _state.update { it.copy(
                        position = p.currentPosition,
                        bufferedPosition = p.bufferedPosition
                    ) }
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
        queue.clear()
        queue.add(item)
        
        _state.update { it.copy(
            playlist = queue.map { q -> q.song },
            currentIndex = 0
        ) }
        
        playItem(item, 0)
    }
    
    override fun setQueue(items: List<QueueItem>, startIndex: Int) {
        queue.clear()
        queue.addAll(items)
        
        _state.update { it.copy(
            playlist = queue.map { q -> q.song },
            currentIndex = if (items.isNotEmpty()) startIndex.coerceIn(0, items.size - 1) else -1
        ) }
        
        if (items.isNotEmpty() && startIndex in items.indices) {
            playItem(items[startIndex], startIndex)
        }
    }
    
    override fun addToQueue(item: QueueItem) {
        queue.add(item)
        _state.update { it.copy(playlist = queue.map { q -> q.song }) }
    }
    
    override fun addNext(item: QueueItem) {
        val insertIndex = (_state.value.currentIndex + 1).coerceIn(0, queue.size)
        queue.add(insertIndex, item)
        _state.update { it.copy(playlist = queue.map { q -> q.song }) }
    }
    
    override fun removeFromQueue(index: Int) {
        if (index !in queue.indices) return
        
        val currentIndex = _state.value.currentIndex
        queue.removeAt(index)
        
        val newIndex = when {
            queue.isEmpty() -> -1
            index < currentIndex -> currentIndex - 1
            index == currentIndex && index >= queue.size -> queue.size - 1
            else -> currentIndex
        }
        
        _state.update { it.copy(
            playlist = queue.map { q -> q.song },
            currentIndex = newIndex
        ) }
        
        if (index == currentIndex && queue.isNotEmpty() && newIndex in queue.indices) {
            playItem(queue[newIndex], newIndex)
        } else if (queue.isEmpty()) {
            stop()
        }
    }
    
    override fun clearQueue() {
        queue.clear()
        stop()
        _state.update { it.copy(
            playlist = emptyList(),
            currentIndex = -1,
            currentSong = null
        ) }
    }
    
    private fun playItem(item: QueueItem, index: Int) {
        _state.update { it.copy(
            currentSong = item.song,
            currentIndex = index,
            playbackState = PlaybackState.LOADING,
            position = 0L,
            duration = 0L,
            error = null
        ) }
        
        val currentPlayer = player
        if (currentPlayer == null) {
            setupLocalPlayer()
        }
        
        scope.launch {
            try {
                val p = player ?: return@launch
                
                // 创建带自定义 Header 的数据源
                val dataSourceFactory = if (item.headers.isNotEmpty()) {
                    DefaultHttpDataSource.Factory()
                        .setDefaultRequestProperties(item.headers)
                        .setUserAgent("Misty/1.0")
                        .setConnectTimeoutMs(15000)
                        .setReadTimeoutMs(15000)
                        .setAllowCrossProtocolRedirects(true)
                } else {
                    DefaultHttpDataSource.Factory()
                        .setUserAgent("Misty/1.0")
                        .setConnectTimeoutMs(15000)
                        .setReadTimeoutMs(15000)
                        .setAllowCrossProtocolRedirects(true)
                }
                
                val mediaSourceFactory = DefaultMediaSourceFactory(context)
                    .setDataSourceFactory(dataSourceFactory)
                
                // 创建 MediaItem
                val mediaItem = MediaItem.Builder()
                    .setUri(item.audioUrl)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(item.song.name)
                            .setArtist(item.song.artists.joinToString { it.name })
                            .setAlbumTitle(item.song.album?.name)
                            .setArtworkUri(item.song.coverUrl?.toUri())
                            .build()
                    )
                    .build()
                
                // 使用 ExoPlayer 直接设置 MediaSource
                if (p is ExoPlayer) {
                    p.setMediaSource(mediaSourceFactory.createMediaSource(mediaItem))
                    p.prepare()
                    p.play()
                } else {
                    // MediaController 模式
                    p.setMediaItem(mediaItem)
                    p.prepare()
                    p.play()
                }
                
            } catch (e: Exception) {
                _state.update { it.copy(
                    playbackState = PlaybackState.ERROR,
                    error = e.message ?: "Failed to play"
                ) }
                e.printStackTrace()
            }
        }
    }
    
    override fun playPause() {
        player?.let { p ->
            if (p.isPlaying) {
                pause()
            } else {
                resume()
            }
        }
    }
    
    override fun resume() {
        player?.play()
    }
    
    override fun pause() {
        player?.pause()
    }
    
    override fun stop() {
        player?.stop()
        stopPositionUpdates()
        _state.update { it.copy(
            playbackState = PlaybackState.IDLE,
            isPlaying = false,
            position = 0L
        ) }
    }
    
    override fun seekTo(position: Long) {
        player?.seekTo(position)
        _state.update { it.copy(position = position) }
    }
    
    override fun previous() {
        val currentIndex = _state.value.currentIndex
        val shuffleEnabled = _state.value.shuffleEnabled
        
        // 如果播放超过 3 秒，回到开头
        val currentPosition = player?.currentPosition ?: 0L
        if (currentPosition > 3000) {
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
        
        player?.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }
    
    override fun setShuffleEnabled(enabled: Boolean) {
        _state.update { it.copy(shuffleEnabled = enabled) }
        player?.shuffleModeEnabled = enabled
    }
    
    override fun release() {
        stopPositionUpdates()
        
        // 释放 MediaController
        mediaControllerFuture?.let { future ->
            MediaController.releaseFuture(future)
        }
        mediaController = null
        mediaControllerFuture = null
        
        // 释放本地 Player
        localPlayer?.release()
        localPlayer = null
        
        scope.cancel()
    }
}
