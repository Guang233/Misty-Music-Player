package com.guang.misty.data.log

import com.guang.misty.util.LogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * JVM 平台日志存储实现
 */
class JvmLogStorage : LogStorage {
    private val logDir: File
    private val mutex = Mutex()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    init {
        val userHome = System.getProperty("user.home")
        val appDir = File(userHome, ".misty")
        logDir = File(appDir, "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
    }
    
    private fun getCurrentLogFileName(): String {
        return "misty_${LocalDate.now().format(dateFormatter)}.log"
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
                val currentFile = File(logDir, getCurrentLogFileName())
                if (currentFile.exists()) {
                    currentFile.absolutePath
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

private val logStorageInstance by lazy { JvmLogStorage() }

actual fun createLogStorage(): LogStorage = logStorageInstance
