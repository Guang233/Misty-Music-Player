package com.guang.misty.player

import android.content.Context

/**
 * Android Context 持有者
 * 需要在 Application 或 Activity 中初始化
 */
object PlayerContextHolder {
    @Volatile
    private var _context: Context? = null
    
    val context: Context
        get() = _context ?: throw IllegalStateException(
            "PlayerContextHolder not initialized. Call PlayerContextHolder.init(context) first."
        )
    
    fun init(context: Context) {
        _context = context.applicationContext
    }
}

/**
 * Android 平台创建播放器实例
 */
actual fun createMistyPlayer(): MistyPlayer {
    return AndroidMistyPlayer(PlayerContextHolder.context)
}
