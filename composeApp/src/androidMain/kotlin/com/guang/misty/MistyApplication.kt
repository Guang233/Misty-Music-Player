package com.guang.misty

import android.app.Application
import com.guang.misty.engine.cookie.CookieStorage

class MistyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 初始化 CookieStorage
        CookieStorage.initialize(this)
    }
}
