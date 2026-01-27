package com.guang.misty.util

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
 * JVM (Desktop) 平台的日志输出实现
 * 使用标准输出/错误输出到控制台
 */
actual fun platformLog(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
    val timestamp = formatTimestamp(System.currentTimeMillis())
    val logMessage = "[$timestamp] [${level.label}] [$tag] $message"
    
    when (level) {
        LogLevel.DEBUG, LogLevel.INFO -> {
            println(logMessage)
            throwable?.printStackTrace(System.out)
        }
        LogLevel.WARN, LogLevel.ERROR -> {
            System.err.println(logMessage)
            throwable?.printStackTrace(System.err)
        }
    }
}
