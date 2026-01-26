package com.guang.misty.player

import com.guang.misty.model.MistySong
import kotlinx.coroutines.flow.StateFlow

/**
 * 全局播放器服务
 * 
 * 提供单例访问播放器实例，便于在整个应用中使用
 */
object PlayerService {
    
    private var _player: MistyPlayer? = null
    
    /**
     * 播放器实例
     */
    val player: MistyPlayer
        get() = _player ?: throw IllegalStateException(
            "PlayerService not initialized. Call PlayerService.initialize() first."
        )
    
    /**
     * 播放器状态流
     */
    val state: StateFlow<PlayerState>
        get() = player.state
    
    /**
     * 是否已初始化
     */
    val isInitialized: Boolean
        get() = _player != null
    
    /**
     * 初始化播放器服务
     * 应在应用启动时调用
     */
    fun initialize() {
        if (_player == null) {
            _player = createMistyPlayer()
        }
    }
    
    /**
     * 释放播放器服务
     * 应在应用退出时调用
     */
    fun release() {
        _player?.release()
        _player = null
    }
    
    // ==================== 便捷方法 ====================
    
    /**
     * 播放单曲
     */
    fun play(song: MistySong, audioUrl: String, headers: Map<String, String> = emptyMap()) {
        player.play(song, audioUrl, headers)
    }
    
    /**
     * 设置播放队列并开始播放
     */
    fun setQueue(items: List<QueueItem>, startIndex: Int = 0) {
        player.setQueue(items, startIndex)
    }
    
    /**
     * 添加到队列末尾
     */
    fun addToQueue(item: QueueItem) {
        player.addToQueue(item)
    }
    
    /**
     * 添加到下一首播放
     */
    fun addNext(item: QueueItem) {
        player.addNext(item)
    }
    
    /**
     * 播放/暂停切换
     */
    fun playPause() {
        player.playPause()
    }
    
    /**
     * 播放
     */
    fun resume() {
        player.resume()
    }
    
    /**
     * 暂停
     */
    fun pause() {
        player.pause()
    }
    
    /**
     * 停止
     */
    fun stop() {
        player.stop()
    }
    
    /**
     * 跳转
     */
    fun seekTo(position: Long) {
        player.seekTo(position)
    }
    
    /**
     * 上一曲
     */
    fun previous() {
        player.previous()
    }
    
    /**
     * 下一曲
     */
    fun next() {
        player.next()
    }
    
    /**
     * 跳转到指定索引
     */
    fun skipToIndex(index: Int) {
        player.skipToIndex(index)
    }
    
    /**
     * 设置循环模式
     */
    fun setRepeatMode(mode: RepeatMode) {
        player.setRepeatMode(mode)
    }
    
    /**
     * 设置随机播放
     */
    fun setShuffleEnabled(enabled: Boolean) {
        player.setShuffleEnabled(enabled)
    }
    
    /**
     * 清空队列
     */
    fun clearQueue() {
        player.clearQueue()
    }
}

/**
 * 扩展函数：将歌曲转换为播放队列项
 */
fun MistySong.toQueueItem(audioUrl: String, headers: Map<String, String> = emptyMap()): QueueItem {
    return QueueItem(this, audioUrl, headers)
}

/**
 * 扩展函数：批量转换
 */
fun List<Pair<MistySong, String>>.toQueueItems(headers: Map<String, String> = emptyMap()): List<QueueItem> {
    return map { (song, url) -> QueueItem(song, url, headers) }
}
