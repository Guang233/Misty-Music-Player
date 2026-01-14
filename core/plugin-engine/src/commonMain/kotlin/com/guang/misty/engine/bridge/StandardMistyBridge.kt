package com.guang.misty.engine.bridge

import com.guang.misty.network.MistyHttpClient

/**
 * MistyBridge 的标准实现，使用 MistyHttpClient 处理网络请求
 */
class StandardMistyBridge(
    private val httpClient: MistyHttpClient
) : MistyBridge {

    override suspend fun networkRequest(json: String): String {
        return httpClient.execute(json)
    }

    override fun log(level: String, msg: String) {
        // 简单的日志输出，可以根据需要扩展
        val logMessage = "[$level] $msg"
        println(logMessage)
    }
}
