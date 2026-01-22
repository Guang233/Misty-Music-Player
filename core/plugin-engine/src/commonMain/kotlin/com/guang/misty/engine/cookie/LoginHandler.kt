package com.guang.misty.engine.cookie

/**
 * 创建 LoginHandler 实例的工厂函数
 */
expect fun createLoginHandler(): LoginHandler

/**
 * 登录处理器接口 (跨平台)
 * 使用 expect/actual 模式实现不同平台的登录逻辑
 *
 * Android: 使用 WebView 拦截 Cookie
 * Desktop: 使用系统浏览器 + 手动粘贴 Cookie
 */
expect class LoginHandler {
    /**
     * 请求用户登录并获取 Cookie
     * 
     * @param pluginId 插件 ID (用于日志和UI显示)
     * @param loginUrl 登录页面 URL
     * @return 登录结果，包含 Cookie 列表或错误信息
     */
    suspend fun requestLogin(pluginId: String, loginUrl: String): LoginResult
}
