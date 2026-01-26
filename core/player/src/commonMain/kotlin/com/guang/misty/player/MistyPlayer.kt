package com.guang.misty.player

import com.guang.misty.model.MistySong
import kotlinx.coroutines.flow.StateFlow

/**
 * 播放状态
 */
enum class PlaybackState {
    IDLE,       // 空闲状态
    LOADING,    // 加载中
    READY,      // 就绪（暂停）
    PLAYING,    // 播放中
    ENDED,      // 播放结束
    ERROR       // 错误状态
}

/**
 * 循环模式
 */
enum class RepeatMode {
    OFF,        // 不循环
    ONE,        // 单曲循环
    ALL         // 列表循环
}

/**
 * 播放器状态
 */
data class PlayerState(
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val isPlaying: Boolean = false,
    val currentSong: MistySong? = null,
    val currentIndex: Int = -1,
    val duration: Long = 0L,         // 总时长（毫秒）
    val position: Long = 0L,         // 当前位置（毫秒）
    val bufferedPosition: Long = 0L, // 缓冲位置（毫秒）
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val playlist: List<MistySong> = emptyList(),
    val error: String? = null
)

/**
 * 播放队列项
 */
data class QueueItem(
    val song: MistySong,
    val audioUrl: String,
    val headers: Map<String, String> = emptyMap()
)

/**
 * Misty 播放器接口
 * 
 * 跨平台音乐播放器，支持：
 * - 播放/暂停/停止
 * - 上一曲/下一曲
 * - 进度控制
 * - 播放队列管理
 * - 循环模式/随机播放
 */
interface MistyPlayer {
    
    /**
     * 播放器状态流
     */
    val state: StateFlow<PlayerState>
    
    /**
     * 播放指定歌曲
     * @param song 歌曲信息
     * @param audioUrl 音频 URL
     * @param headers 可选的 HTTP 头（如 Cookie）
     */
    fun play(song: MistySong, audioUrl: String, headers: Map<String, String> = emptyMap())
    
    /**
     * 设置播放队列并开始播放
     * @param items 播放队列
     * @param startIndex 开始播放的索引
     */
    fun setQueue(items: List<QueueItem>, startIndex: Int = 0)
    
    /**
     * 添加到播放队列末尾
     */
    fun addToQueue(item: QueueItem)
    
    /**
     * 添加到下一首播放
     */
    fun addNext(item: QueueItem)
    
    /**
     * 从队列中移除
     */
    fun removeFromQueue(index: Int)
    
    /**
     * 清空播放队列
     */
    fun clearQueue()
    
    /**
     * 播放/暂停切换
     */
    fun playPause()
    
    /**
     * 播放
     */
    fun resume()
    
    /**
     * 暂停
     */
    fun pause()
    
    /**
     * 停止
     */
    fun stop()
    
    /**
     * 跳转到指定位置
     * @param position 位置（毫秒）
     */
    fun seekTo(position: Long)
    
    /**
     * 播放上一曲
     */
    fun previous()
    
    /**
     * 播放下一曲
     */
    fun next()
    
    /**
     * 跳转到队列中的指定歌曲
     */
    fun skipToIndex(index: Int)
    
    /**
     * 设置循环模式
     */
    fun setRepeatMode(mode: RepeatMode)
    
    /**
     * 设置随机播放
     */
    fun setShuffleEnabled(enabled: Boolean)
    
    /**
     * 释放资源
     */
    fun release()
}

/**
 * 创建播放器实例
 */
expect fun createMistyPlayer(): MistyPlayer
