package com.guang.misty.player

/**
 * JVM/Desktop 平台创建播放器实例
 */
actual fun createMistyPlayer(): MistyPlayer {
    return DesktopMistyPlayer()
}
