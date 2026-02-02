package com.guang.misty

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.guang.misty.player.PlayerService

fun main() = application {
    // 初始化播放器服务
    PlayerService.initialize()
    
    val windowState = rememberWindowState(
        size = DpSize(1200.dp, 800.dp)
    )
    
    Window(
        onCloseRequest = {
            // 释放播放器
            PlayerService.release()
            exitApplication()
        },
        title = "Misty",
        state = windowState
    ) {
        App()
    }
}