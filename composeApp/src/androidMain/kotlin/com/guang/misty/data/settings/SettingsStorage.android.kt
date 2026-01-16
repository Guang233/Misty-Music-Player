package com.guang.misty.data.settings

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    encodeDefaults = true
}

/**
 * Android 应用上下文持有者
 * 需要在 Application 或 Activity 中初始化
 */
object AndroidContextHolder {
    @Volatile  // 确保多线程可见性
    private var _context: Context? = null
    
    val context: Context
        get() = _context ?: throw IllegalStateException("AndroidContextHolder not initialized")
    
    fun init(context: Context) {
        _context = context.applicationContext
    }
}

/**
 * 获取应用数据目录
 */
private fun getAppDataDirectory(): File {
    val filesDir = AndroidContextHolder.context.filesDir
    val appDir = File(filesDir, "misty_data")
    if (!appDir.exists()) {
        appDir.mkdirs()
    }
    return appDir
}

/**
 * Android 平台设置存储实现
 */
class AndroidSettingsStorage : SettingsStorage {
    private val settingsFile by lazy { File(getAppDataDirectory(), "settings.json") }
    private val _settingsFlow = MutableStateFlow(MistySettings.Default)
    private val mutex = Mutex()
    
    init {
        // 初始化时加载设置
        val settings = loadSettingsSync()
        _settingsFlow.value = settings
    }
    
    override val settingsFlow: Flow<MistySettings> = _settingsFlow.asStateFlow()
    
    override suspend fun getSettings(): MistySettings = _settingsFlow.value
    
    override suspend fun updateSettings(settings: MistySettings) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    settingsFile.writeText(json.encodeToString(settings))
                    _settingsFlow.value = settings
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    override suspend fun updateSettings(update: (MistySettings) -> MistySettings) {
        mutex.withLock {
            val current = _settingsFlow.value
            val updated = update(current)
            withContext(Dispatchers.IO) {
                try {
                    settingsFile.writeText(json.encodeToString(updated))
                    _settingsFlow.value = updated
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun loadSettingsSync(): MistySettings {
        return try {
            if (settingsFile.exists()) {
                json.decodeFromString(settingsFile.readText())
            } else {
                MistySettings.Default
            }
        } catch (e: Exception) {
            e.printStackTrace()
            MistySettings.Default
        }
    }
}

/**
 * Android 平台插件存储实现
 */
class AndroidPluginStorage : PluginStorage {
    private val pluginsDir by lazy { 
        val dir = File(getAppDataDirectory(), "plugins")
        if (!dir.exists()) dir.mkdirs()
        dir
    }
    private val pluginDataFile by lazy { File(getAppDataDirectory(), "plugins.json") }
    private val _pluginDataFlow = MutableStateFlow(PluginStorageData())
    private val mutex = Mutex()
    
    init {
        // 初始化时加载插件数据
        val data = loadPluginDataSync()
        _pluginDataFlow.value = data
    }
    
    override val pluginDataFlow: Flow<PluginStorageData> = _pluginDataFlow.asStateFlow()
    
    override suspend fun getPluginData(): PluginStorageData = _pluginDataFlow.value
    
    override suspend fun updatePluginData(data: PluginStorageData) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    pluginDataFile.writeText(json.encodeToString(data))
                    _pluginDataFlow.value = data
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    override fun getPluginsDirectory(): String = pluginsDir.absolutePath
    
    override suspend fun savePluginFile(fileName: String, content: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(pluginsDir, fileName)
                file.writeText(content)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    override suspend fun readPluginFile(fileName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(pluginsDir, fileName)
                if (file.exists()) file.readText() else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    override suspend fun deletePluginFile(fileName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(pluginsDir, fileName)
                if (file.exists()) file.delete() else true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    override suspend fun listPluginFiles(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                pluginsDir.listFiles()
                    ?.filter { it.isFile && it.extension == "js" }
                    ?.map { it.name }
                    ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    private fun loadPluginDataSync(): PluginStorageData {
        return try {
            if (pluginDataFile.exists()) {
                json.decodeFromString(pluginDataFile.readText())
            } else {
                PluginStorageData()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            PluginStorageData()
        }
    }
}

// 单例实例
private val settingsStorageInstance by lazy { AndroidSettingsStorage() }
private val pluginStorageInstance by lazy { AndroidPluginStorage() }

actual fun createSettingsStorage(): SettingsStorage = settingsStorageInstance

actual fun createPluginStorage(): PluginStorage = pluginStorageInstance
