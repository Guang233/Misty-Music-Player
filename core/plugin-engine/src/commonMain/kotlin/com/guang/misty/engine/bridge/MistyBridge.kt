package com.guang.misty.engine.bridge

/**
 * Misty 插件系统与 Kotlin 代码之间的桥接接口
 * 插件通过此接口访问网络功能、日志功能和 Cookie 管理功能
 */
interface MistyBridge {
    /**
     * 执行网络请求
     * @param json JSON 格式的请求字符串
     * @return JSON 格式的响应字符串
     */
    suspend fun networkRequest(json: String): String

    /**
     * 记录日志
     * @param level 日志级别（如 "INFO", "ERROR", "WARN"）
     * @param msg 日志消息
     */
    fun log(level: String, msg: String)

    // ===== Cookie 管理功能 =====

    /**
     * 请求用户登录并获取 Cookie
     * @param pluginId 插件 ID
     * @param loginUrl 登录页面 URL
     * @return JSON 格式的登录结果 (LoginResult)
     */
    suspend fun requestLogin(pluginId: String, loginUrl: String): String

    /**
     * 获取指定插件的 Cookies
     * @param pluginId 插件 ID
     * @param domain 可选的域名过滤，空字符串表示返回所有 Cookie
     * @return JSON 格式的 Cookie 列表
     */
    suspend fun getCookies(pluginId: String, domain: String): String

    /**
     * 手动保存 Cookies
     * @param pluginId 插件 ID
     * @param cookiesJson JSON 格式的 Cookie 列表
     * @return 是否成功
     */
    suspend fun setCookies(pluginId: String, cookiesJson: String): Boolean

    /**
     * 清除指定插件的所有 Cookies
     * @param pluginId 插件 ID
     * @return 是否成功
     */
    suspend fun clearCookies(pluginId: String): Boolean
}
