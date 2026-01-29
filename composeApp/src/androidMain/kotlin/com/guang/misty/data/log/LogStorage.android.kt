package com.guang.misty.data.log

import com.guang.misty.data.settings.AndroidContextHolder
import com.guang.misty.util.LogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Android 平台日志存储实现
 */
class AndroidLogStorage : LogStorage {
    private val logDir: File by lazy {
        val filesDir = AndroidContextHolder.context.filesDir
        val dir = File(filesDir, "logs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }
    private val mutex = Mutex()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    private fun getCurrentLogFileName(): String {
        return "misty_${dateFormat.format(Date())}.log"
    }
    
    override suspend fun appendLog(entry: LogEntry) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    val logFile = File(logDir, getCurrentLogFileName())
                    logFile.appendText(entry.format() + "\n")
                } catch (e: Exception) {
                    // 日志写入失败时不抛出异常，避免影响主流程
                    e.printStackTrace()
                }
            }
        }
    }
    
    override fun getLogDirectory(): String = logDir.absolutePath
    
    override fun getCurrentLogFile(): String = File(logDir, getCurrentLogFileName()).absolutePath
    
    override suspend fun exportLogs(): String? {
        return withContext(Dispatchers.IO) {
            try {
                // 将日志文件复制到缓存目录以便分享
                val currentFile = File(logDir, getCurrentLogFileName())
                if (currentFile.exists()) {
                    val cacheDir = AndroidContextHolder.context.cacheDir
                    val exportFile = File(cacheDir, "misty_logs_export.txt")
                    currentFile.copyTo(exportFile, overwrite = true)
                    exportFile.absolutePath
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    override suspend fun clearLogFile() {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    val logFile = File(logDir, getCurrentLogFileName())
                    if (logFile.exists()) {
                        logFile.writeText("")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    override suspend fun listLogFiles(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                logDir.listFiles()
                    ?.filter { it.isFile && it.extension == "log" }
                    ?.sortedByDescending { it.lastModified() }
                    ?.map { it.name }
                    ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}

private val logStorageInstance by lazy { AndroidLogStorage() }

actual fun createLogStorage(): LogStorage = logStorageInstance
