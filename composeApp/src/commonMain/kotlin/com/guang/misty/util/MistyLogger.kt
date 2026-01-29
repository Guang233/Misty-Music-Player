package com.guang.misty.util

import com.guang.misty.data.log.LogStorage
import com.guang.misty.data.log.createLogStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 日志级别
 */
enum class LogLevel(val label: String, val priority: Int) {
    DEBUG("DEBUG", 0),
    INFO("INFO", 1),
    WARN("WARN", 2),
    ERROR("ERROR", 3)
}

/**
 * 日志条目
 */
data class LogEntry(
    val timestampMillis: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null
) {
    /**
     * 格式化为可读字符串
     */
    fun format(): String {
        val time = formatTimestamp(timestampMillis)
        val throwableStr = throwable?.let { "\n${it.stackTraceToString()}" } ?: ""
        return "[$time] [${level.label}] [$tag] $message$throwableStr"
    }
}

/**
 * 获取当前时间戳（毫秒）
 */
expect fun currentTimeMillis(): Long

/**
 * 平台特定的时间戳格式化
 */
expect fun formatTimestamp(millis: Long): String

/**
 * 日志存储 - 保存最近的日志条目
 */
object LogStore {
    private const val MAX_ENTRIES = 500
    
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()
    
    // 用于文件写入的协程作用域
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // 延迟初始化日志存储
    private val logStorage: LogStorage by lazy { createLogStorage() }
    
    /**
     * 添加日志条目
     */
    fun add(entry: LogEntry) {
        _logs.value = (_logs.value + entry).takeLast(MAX_ENTRIES)
        
        // 异步写入文件
        scope.launch {
            try {
                logStorage.appendLog(entry)
            } catch (_: Exception) {
                // 忽略写入异常
            }
        }
    }
    
    /**
     * 清空内存中的日志
     */
    fun clear() {
        _logs.value = emptyList()
    }
    
    /**
     * 清空日志文件
     */
    suspend fun clearLogFile() {
        logStorage.clearLogFile()
    }
    
    /**
     * 获取日志目录
     */
    fun getLogDirectory(): String = logStorage.getLogDirectory()
    
    /**
     * 获取当前日志文件路径
     */
    fun getCurrentLogFile(): String = logStorage.getCurrentLogFile()
    
    /**
     * 导出日志（用于分享）
     */
    suspend fun exportLogs(): String? = logStorage.exportLogs()
    
    /**
     * 获取指定级别及以上的日志
     */
    fun getByMinLevel(minLevel: LogLevel): List<LogEntry> {
        return _logs.value.filter { it.level.priority >= minLevel.priority }
    }
    
    /**
     * 获取指定标签的日志
     */
    fun getByTag(tag: String): List<LogEntry> {
        return _logs.value.filter { it.tag == tag }
    }
}

/**
 * 跨平台日志工具
 * 
 * 使用方式：
 * ```
 * MistyLogger.d("MyTag", "Debug message")
 * MistyLogger.i("MyTag", "Info message")
 * MistyLogger.w("MyTag", "Warning message")
 * MistyLogger.e("MyTag", "Error message", exception)
 * ```
 */
object MistyLogger {
    private const val DEFAULT_TAG = "Misty"
    
    /**
     * Debug 级别日志
     */
    fun d(tag: String = DEFAULT_TAG, message: String) {
        log(LogLevel.DEBUG, tag, message)
    }
    
    /**
     * Info 级别日志
     */
    fun i(tag: String = DEFAULT_TAG, message: String) {
        log(LogLevel.INFO, tag, message)
    }
    
    /**
     * Warning 级别日志
     */
    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        log(LogLevel.WARN, tag, message, throwable)
    }
    
    /**
     * Error 级别日志
     */
    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, tag, message, throwable)
    }
    
    /**
     * 通用日志方法
     */
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        val entry = LogEntry(
            timestampMillis = currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable
        )
        
        // 保存到日志存储
        LogStore.add(entry)
        
        // 输出到平台日志
        platformLog(level, tag, message, throwable)
    }
}

/**
 * 平台特定的日志输出
 */
expect fun platformLog(level: LogLevel, tag: String, message: String, throwable: Throwable?)
