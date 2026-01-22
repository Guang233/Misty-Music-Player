package com.guang.misty.engine.cookie

/**
 * 创建 LoginHandler 实例（Android 实现）
 */
actual fun createLoginHandler(): LoginHandler {
    return LoginHandler()
}

/**
 * Android 端登录处理器实现
 * 使用 WebView 拦截 Cookie
 *
 * 注意：此类需要与 Android UI 层配合使用
 * 实际的 WebView 实现应在 composeApp 模块中完成
 */
actual class LoginHandler {
    // 登录回调处理器（由外部设置）
    private var loginCallback: ((String, String) -> LoginResult)? = null

    /**
     * 设置登录回调处理器
     * 应用层需要调用此方法注入实际的登录逻辑
     */
    fun setLoginCallback(callback: (pluginId: String, loginUrl: String) -> LoginResult) {
        loginCallback = callback
    }

    actual suspend fun requestLogin(pluginId: String, loginUrl: String): LoginResult {
        val callback = loginCallback
        if (callback == null) {
            return LoginResult(
                success = false,
                error = "LoginHandler not initialized. Please set login callback first."
            )
        }

        return try {
            callback(pluginId, loginUrl)
        } catch (e: Exception) {
            LoginResult(
                success = false,
                error = e.message ?: "登录失败"
            )
        }
    }
}

/**
 * Android WebView Cookie 拦截器辅助类
 * 提供从 WebView CookieManager 中提取 Cookie 的工具方法
 */
object WebViewCookieHelper {
    /**
     * 从 WebView CookieManager 的 Cookie 字符串解析为 MistyCookie 列表
     * 
     * @param cookieString Cookie 字符串，格式: "name1=value1; name2=value2"
     * @param domain 域名
     * @return Cookie 列表
     */
    fun parseCookieString(cookieString: String, domain: String): List<MistyCookie> {
        if (cookieString.isBlank()) return emptyList()

        return cookieString.split(";").mapNotNull { pair ->
            val trimmed = pair.trim()
            if (trimmed.isEmpty()) return@mapNotNull null

            val parts = trimmed.split("=", limit = 2)
            if (parts.size != 2) return@mapNotNull null

            MistyCookie(
                name = parts[0].trim(),
                value = parts[1].trim(),
                domain = domain,
                path = "/"
            )
        }
    }

    /**
     * 从 android.webkit.CookieManager 获取指定 URL 的所有 Cookie
     * 
     * @param url 目标 URL
     * @return Cookie 列表
     * 
     * 使用示例（在 Android Activity/Fragment 中）：
     * ```kotlin
     * val cookieManager = android.webkit.CookieManager.getInstance()
     * val cookieString = cookieManager.getCookie(url)
     * val cookies = WebViewCookieHelper.parseCookieString(cookieString, domain)
     * ```
     */
    fun getCookiesFromWebView(url: String): List<MistyCookie> {
        return try {
            // 需要在 Android 平台上调用
            // val cookieManager = android.webkit.CookieManager.getInstance()
            // val cookieString = cookieManager.getCookie(url) ?: ""
            // parseCookieString(cookieString, extractDomain(url))
            
            // 这里返回空列表，实际实现需要在 composeApp 模块中完成
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun extractDomain(url: String): String {
        return try {
            val uri = java.net.URI(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }
}
