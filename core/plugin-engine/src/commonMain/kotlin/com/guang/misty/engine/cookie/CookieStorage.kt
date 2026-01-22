package com.guang.misty.engine.cookie

/**
 * 创建 CookieStorage 实例的工厂函数
 */
expect fun createCookieStorage(): CookieStorage

/**
 * Cookie 存储接口 (跨平台)
 * 使用 expect/actual 模式实现不同平台的存储
 */
expect class CookieStorage {
    /**
     * 保存指定插件的 Cookies
     * @param pluginId 插件 ID
     * @param cookies Cookie 列表
     */
    suspend fun saveCookies(pluginId: String, cookies: List<MistyCookie>)

    /**
     * 获取指定插件的 Cookies
     * @param pluginId 插件 ID
     * @param domain 可选的域名过滤，null 表示返回所有 Cookie
     * @return Cookie 列表（自动过滤已过期的 Cookie）
     */
    suspend fun getCookies(pluginId: String, domain: String? = null): List<MistyCookie>

    /**
     * 清除指定插件的所有 Cookies
     * @param pluginId 插件 ID
     */
    suspend fun clearCookies(pluginId: String)

    /**
     * 清除所有插件的 Cookies
     */
    suspend fun clearAllCookies()
}
