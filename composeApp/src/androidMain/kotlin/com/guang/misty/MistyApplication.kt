package com.guang.misty

import android.app.Application
import com.guang.misty.engine.cookie.CookieStorage
import com.guang.misty.player.PlayerContextHolder
import com.guang.misty.player.PlayerService

class MistyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 初始化 CookieStorage
        CookieStorage.initialize(this)
        
        // 初始化播放器上下文
        PlayerContextHolder.init(this)
        
        // 初始化播放器服务
        PlayerService.initialize()
    }
    
    override fun onTerminate() {
        super.onTerminate()
        // 释放播放器
        PlayerService.release()
    }
}
