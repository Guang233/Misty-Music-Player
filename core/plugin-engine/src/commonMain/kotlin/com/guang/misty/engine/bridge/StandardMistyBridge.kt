package com.guang.misty.engine.bridge

import com.guang.misty.engine.cookie.CookieStorage
import com.guang.misty.engine.cookie.LoginHandler
import com.guang.misty.engine.cookie.MistyCookie
import com.guang.misty.engine.cookie.MistyCookieManager
import com.guang.misty.network.MistyHttpClient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 日志回调函数类型
 */
typealias LogCallback = (level: String, tag: String, message: String) -> Unit

/**
 * MistyBridge 的标准实现，使用 MistyHttpClient 处理网络请求
 */
class StandardMistyBridge(
    private val httpClient: MistyHttpClient,
    cookieStorage: CookieStorage,
    loginHandler: LoginHandler,
    private val logCallback: LogCallback? = null
) : MistyBridge {

    private val cookieManager = MistyCookieManager(cookieStorage, loginHandler)
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override suspend fun networkRequest(json: String): String {
        return httpClient.execute(json)
    }

    override fun log(level: String, msg: String) {
        // 使用回调或默认输出
        if (logCallback != null) {
            logCallback.invoke(level, "Plugin", msg)
        } else {
            val logMessage = "[$level] $msg"
            println(logMessage)
        }
    }

    // ===== Cookie 管理功能实现 =====

    override suspend fun requestLogin(pluginId: String, loginUrl: String): String {
        return try {
            val result = cookieManager.requestLogin(pluginId, loginUrl)
            json.encodeToString(result)
        } catch (e: Exception) {
            log("ERROR", "requestLogin failed: ${e.message}")
            json.encodeToString(
                com.guang.misty.engine.cookie.LoginResult(
                    success = false,
                    error = e.message ?: "Unknown error"
                )
            )
        }
    }

    override suspend fun getCookies(pluginId: String, domain: String): String {
        return try {
            val domainFilter = if (domain.isBlank()) null else domain
            val cookies = cookieManager.getCookies(pluginId, domainFilter)
            json.encodeToString(cookies)
        } catch (e: Exception) {
            log("ERROR", "getCookies failed: ${e.message}")
            "[]"
        }
    }

    override suspend fun setCookies(pluginId: String, cookiesJson: String): Boolean {
        return try {
            val cookies = json.decodeFromString<List<MistyCookie>>(cookiesJson)
            cookieManager.saveCookies(pluginId, cookies)
            true
        } catch (e: Exception) {
            log("ERROR", "setCookies failed: ${e.message}")
            false
        }
    }

    override suspend fun clearCookies(pluginId: String): Boolean {
        return try {
            cookieManager.clearCookies(pluginId)
            true
        } catch (e: Exception) {
            log("ERROR", "clearCookies failed: ${e.message}")
            false
        }
    }
}
