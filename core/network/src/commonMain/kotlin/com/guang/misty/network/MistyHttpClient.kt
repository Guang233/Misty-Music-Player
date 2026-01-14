package com.guang.misty.network

import com.guang.misty.model.MistyNetworkRequest
import com.guang.misty.model.MistyNetworkResponse
import io.ktor.client.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Misty 网络客户端，提供持久化的 HTTP 客户端和会话管理
 */
class MistyHttpClient {
    private val client = HttpClient {
        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /**
     * 执行网络请求
     * @param requestJson JSON 格式的 MistyNetworkRequest
     * @return JSON 格式的 MistyNetworkResponse
     */
    suspend fun execute(requestJson: String): String {
        try {
            // 解析请求
            val request = json.decodeFromString<MistyNetworkRequest>(requestJson)

            // 构建 HTTP 请求
            val response = client.request(request.url) {
                method = HttpMethod.parse(request.method.uppercase())

                // 设置请求头
                request.headers.forEach { (key, value) ->
                    headers.append(key, value)
                }

                // 设置请求体（根据编码方式解码）
                if (request.body != null) {
                    val bodyBytes = when (request.bodyEncoding.lowercase()) {
                        "hex" -> request.body!!.decodeHex()
                        "base64" -> request.body!!.decodeBase64()
                        "string", "text" -> request.body!!.encodeToByteArray()
                        else -> request.body!!.encodeToByteArray() // 默认使用字符串编码
                    }
                    setBody(bodyBytes)
                }
            }

            // 读取响应体（根据编码方式编码）
            val rawBytes = response.readRawBytes()
            val responseBody: String = when (request.responseEncoding.lowercase()) {
                "hex" -> rawBytes.encodeHex()
                "base64" -> rawBytes.encodeBase64()
                "string", "text" -> rawBytes.decodeToString()
                else -> rawBytes.decodeToString() // 默认使用字符串解码
            }

            // 构建响应对象
            val responseHeaders = response.headers.entries().associate {
                it.key to it.value.joinToString(", ")
            }

            val networkResponse = MistyNetworkResponse(
                statusCode = response.status.value,
                headers = responseHeaders,
                body = responseBody
            )

            // 返回 JSON 格式的响应
            return json.encodeToString(networkResponse)
        } catch (e: Exception) {
            // 错误处理
            val errorResponse = MistyNetworkResponse(
                statusCode = 0,
                headers = emptyMap(),
                body = "",
                error = e.message ?: "Unknown error"
            )
            return json.encodeToString(errorResponse)
        }
    }

    /**
     * 关闭客户端
     */
    fun close() {
        client.close()
    }
}
