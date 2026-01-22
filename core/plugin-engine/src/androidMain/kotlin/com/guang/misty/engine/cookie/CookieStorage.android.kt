package com.guang.misty.engine.cookie

import android.content.Context
import android.content.SharedPreferences
import com.guang.misty.crypto.MistyCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 创建 CookieStorage 实例（Android 实现）
 */
actual fun createCookieStorage(): CookieStorage {
    return CookieStorage()
}

/**
 * Android 端 Cookie 存储实现
 * 使用 SharedPreferences 存储，AES 加密
 */
actual class CookieStorage(private val context: Context) {
    constructor() : this(getApplicationContext())

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("misty_cookies", Context.MODE_PRIVATE)
    }

    // 加密密钥（基于应用包名和设备标识生成）
    private val encryptionKey: String by lazy {
        val packageName = context.packageName
        val androidId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
        val deviceId = "$packageName-$androidId"
        MistyCrypto.md5(deviceId).take(16)
    }

    actual suspend fun saveCookies(pluginId: String, cookies: List<MistyCookie>) {
        withContext(Dispatchers.IO) {
            try {
                val cookiesJson = json.encodeToString(cookies)
                
                // 加密后保存
                val encrypted = MistyCrypto.aesEcbEncryptToBase64(cookiesJson, encryptionKey)
                
                prefs.edit()
                    .putString(getKey(pluginId), encrypted)
                    .apply()
            } catch (e: Exception) {
                println("[CookieStorage] Failed to save cookies for $pluginId: ${e.message}")
                throw e
            }
        }
    }

    actual suspend fun getCookies(pluginId: String, domain: String?): List<MistyCookie> {
        return withContext(Dispatchers.IO) {
            try {
                val encrypted = prefs.getString(getKey(pluginId), null)
                    ?: return@withContext emptyList()

                // 解密
                val decrypted = MistyCrypto.aesEcbDecryptFromBase64(encrypted, encryptionKey)
                val cookies = json.decodeFromString<List<MistyCookie>>(decrypted)

                // 过滤已过期的 Cookie 和域名匹配
                cookies.filter { cookie ->
                    !cookie.isExpired() && (domain == null || cookie.matchesDomain(domain))
                }
            } catch (e: Exception) {
                println("[CookieStorage] Failed to get cookies for $pluginId: ${e.message}")
                emptyList()
            }
        }
    }

    actual suspend fun clearCookies(pluginId: String) {
        withContext(Dispatchers.IO) {
            try {
                prefs.edit()
                    .remove(getKey(pluginId))
                    .apply()
            } catch (e: Exception) {
                println("[CookieStorage] Failed to clear cookies for $pluginId: ${e.message}")
            }
        }
    }

    actual suspend fun clearAllCookies() {
        withContext(Dispatchers.IO) {
            try {
                prefs.edit()
                    .clear()
                    .apply()
            } catch (e: Exception) {
                println("[CookieStorage] Failed to clear all cookies: ${e.message}")
            }
        }
    }

    /**
     * 获取插件的存储 key
     */
    private fun getKey(pluginId: String): String = "cookie_$pluginId"

    companion object {
        // 全局 Context 持有者（需要在应用初始化时设置）
        private var applicationContext: Context? = null

        /**
         * 设置全局 Application Context
         * 应在 Application.onCreate() 中调用
         */
        fun initialize(context: Context) {
            applicationContext = context.applicationContext
        }

        private fun getApplicationContext(): Context {
            return applicationContext
                ?: throw IllegalStateException(
                    "CookieStorage not initialized. " +
                    "Please call CookieStorage.initialize(context) in Application.onCreate()"
                )
        }
    }
}
