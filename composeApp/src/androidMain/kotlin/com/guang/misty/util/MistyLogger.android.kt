package com.guang.misty.util

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 获取当前时间戳（毫秒）
 */
actual fun currentTimeMillis(): Long = System.currentTimeMillis()

/**
 * 格式化时间戳
 */
actual fun formatTimestamp(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    return sdf.format(Date(millis))
}

/**
 * Android 平台的日志输出实现
 * 使用 Android Log API 输出到 Logcat
 */
actual fun platformLog(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
    when (level) {
        LogLevel.DEBUG -> {
            if (throwable != null) Log.d(tag, message, throwable)
            else Log.d(tag, message)
        }
        LogLevel.INFO -> {
            if (throwable != null) Log.i(tag, message, throwable)
            else Log.i(tag, message)
        }
        LogLevel.WARN -> {
            if (throwable != null) Log.w(tag, message, throwable)
            else Log.w(tag, message)
        }
        LogLevel.ERROR -> {
            if (throwable != null) Log.e(tag, message, throwable)
            else Log.e(tag, message)
        }
    }
}
