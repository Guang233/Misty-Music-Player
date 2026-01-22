package com.guang.misty.engine.cookie

import com.guang.misty.crypto.MistyCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

/**
 * 创建 CookieStorage 实例（JVM 实现）
 */
actual fun createCookieStorage(): CookieStorage {
    return CookieStorage()
}

/**
 * Desktop 端 Cookie 存储实现
 * 使用 Properties 文件存储，AES 加密
 */
actual class CookieStorage {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    // 存储目录：用户目录/.misty/cookies/
    private val storageDir: File by lazy {
        val userHome = System.getProperty("user.home")
        File(userHome, ".misty/cookies").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    // 加密密钥（基于设备标识生成）
    private val encryptionKey: String by lazy {
        // 使用系统属性生成设备唯一密钥
        val deviceId = "${System.getProperty("user.name")}-${System.getProperty("os.name")}"
        MistyCrypto.md5(deviceId).take(16)
    }

    actual suspend fun saveCookies(pluginId: String, cookies: List<MistyCookie>) {
        withContext(Dispatchers.IO) {
            try {
                val file = getPluginCookieFile(pluginId)
                val cookiesJson = json.encodeToString(cookies)
                
                // 加密后保存
                val encrypted = MistyCrypto.aesEcbEncryptToBase64(cookiesJson, encryptionKey)
                file.writeText(encrypted)
            } catch (e: Exception) {
                println("[CookieStorage] Failed to save cookies for $pluginId: ${e.message}")
                throw e
            }
        }
    }

    actual suspend fun getCookies(pluginId: String, domain: String?): List<MistyCookie> {
        return withContext(Dispatchers.IO) {
            try {
                val file = getPluginCookieFile(pluginId)
                if (!file.exists()) {
                    return@withContext emptyList()
                }

                // 读取并解密
                val encrypted = file.readText()
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
                val file = getPluginCookieFile(pluginId)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                println("[CookieStorage] Failed to clear cookies for $pluginId: ${e.message}")
            }
        }
    }

    actual suspend fun clearAllCookies() {
        withContext(Dispatchers.IO) {
            try {
                storageDir.listFiles()?.forEach { file ->
                    if (file.extension == "cookies") {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                println("[CookieStorage] Failed to clear all cookies: ${e.message}")
            }
        }
    }

    /**
     * 获取插件的 Cookie 存储文件
     */
    private fun getPluginCookieFile(pluginId: String): File {
        // 文件名格式: pluginId.cookies
        return File(storageDir, "$pluginId.cookies")
    }
}
