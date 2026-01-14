package com.guang.misty.engine.bridge

/**
 * Misty 插件系统与 Kotlin 代码之间的桥接接口
 * 插件通过此接口访问网络功能和日志功能
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
}
