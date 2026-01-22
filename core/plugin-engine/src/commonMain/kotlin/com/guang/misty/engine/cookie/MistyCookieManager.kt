package com.guang.misty.engine.cookie

/**
 * Misty Cookie 管理器
 * 负责管理插件的 Cookie 存储和登录流程
 */
class MistyCookieManager(
    private val storage: CookieStorage,
    private val loginHandler: LoginHandler
) {
    /**
     * 请求用户登录并保存 Cookie
     * 
     * @param pluginId 插件 ID
     * @param loginUrl 登录页面 URL
     * @return 登录结果
     */
    suspend fun requestLogin(pluginId: String, loginUrl: String): LoginResult {
        val result = loginHandler.requestLogin(pluginId, loginUrl)
        
        if (result.success && result.cookies.isNotEmpty()) {
            // 自动保存获取到的 Cookie
            storage.saveCookies(pluginId, result.cookies)
        }
        
        return result
    }

    /**
     * 获取指定插件的 Cookies
     * 
     * @param pluginId 插件 ID
     * @param domain 可选的域名过滤
     * @return Cookie 列表（自动过滤已过期的 Cookie）
     */
    suspend fun getCookies(pluginId: String, domain: String? = null): List<MistyCookie> {
        return storage.getCookies(pluginId, domain)
    }

    /**
     * 手动保存 Cookies（用于插件直接设置 Cookie）
     * 
     * @param pluginId 插件 ID
     * @param cookies Cookie 列表
     */
    suspend fun saveCookies(pluginId: String, cookies: List<MistyCookie>) {
        storage.saveCookies(pluginId, cookies)
    }

    /**
     * 清除指定插件的所有 Cookies
     * 
     * @param pluginId 插件 ID
     */
    suspend fun clearCookies(pluginId: String) {
        storage.clearCookies(pluginId)
    }

    /**
     * 清除所有插件的 Cookies
     */
    suspend fun clearAllCookies() {
        storage.clearAllCookies()
    }

    /**
     * 将 Cookie 列表转换为 HTTP Cookie 请求头字符串
     * 
     * @param cookies Cookie 列表
     * @return Cookie 字符串，格式: "name1=value1; name2=value2"
     */
    fun cookiesToHeaderString(cookies: List<MistyCookie>): String {
        return cookies.joinToString("; ") { it.toCookieString() }
    }
}
