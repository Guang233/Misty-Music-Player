package com.guang.misty

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.guang.misty.engine.cookie.CookieStorage
import com.guang.misty.player.PlayerContextHolder
import com.guang.misty.player.PlayerService
import com.guang.misty.ui.util.createImageLoader

class MistyApplication : Application(), SingletonImageLoader.Factory {
    
    companion object {
        lateinit var instance: MistyApplication
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this

        // 初始化 CookieStorage
        CookieStorage.initialize(this)

        // 初始化播放器上下文
        PlayerContextHolder.init(this)

        // 初始化播放器服务
        PlayerService.initialize()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return createImageLoader(this)
    }
    
    override fun onTerminate() {
        super.onTerminate()
        // 释放播放器
        PlayerService.release()
    }
}
