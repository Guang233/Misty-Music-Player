package com.guang.misty.data.log

import com.guang.misty.util.LogEntry

/**
 * 日志存储接口
 */
interface LogStorage {
    /**
     * 追加日志到文件
     */
    suspend fun appendLog(entry: LogEntry)
    
    /**
     * 获取日志目录路径
     */
    fun getLogDirectory(): String
    
    /**
     * 获取当前日志文件路径
     */
    fun getCurrentLogFile(): String
    
    /**
     * 导出所有日志到临时文件（用于分享）
     * @return 导出的文件路径
     */
    suspend fun exportLogs(): String?
    
    /**
     * 清空日志文件
     */
    suspend fun clearLogFile()
    
    /**
     * 列出所有日志文件
     */
    suspend fun listLogFiles(): List<String>
}

/**
 * 创建日志存储实例
 */
expect fun createLogStorage(): LogStorage
