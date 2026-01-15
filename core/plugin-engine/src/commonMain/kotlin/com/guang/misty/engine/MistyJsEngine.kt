package com.guang.misty.engine

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.define
import com.guang.misty.crypto.MistyCrypto
import com.guang.misty.engine.bridge.MistyBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Misty JavaScript 引擎，基于 QuickJS
 * 负责执行插件 JavaScript 代码并提供与 Kotlin 代码的桥接
 */
class MistyJsEngine(
    private val bridge: MistyBridge
) {
    private val quickJs = QuickJs.create(Dispatchers.Default)

    init {
        // 注入 mistyInternal 绑定
        injectMistyInternal()
        // 加载 bootstrap.js SDK
        runBlocking {
            loadBootstrapScript()
        }
    }

    /**
     * 注入 mistyInternal 对象到 JavaScript 环境
     */
    private fun injectMistyInternal() {
        // 创建桥接对象，将 suspend 函数包装为同步函数供 JS 调用
        quickJs.define("mistyInternal") {
            /**
             * 执行网络请求（从 JS 调用，需要同步包装）
             */
            fun performRequest(requestJson: String): String {
                return runBlocking {
                    bridge.networkRequest(requestJson)
                }
            }

            /**
             * 记录日志
             */
            fun log(level: String, msg: String) {
                bridge.log(level, msg)
            }

            // region Crypto API：给 JS 暴露常见加解密能力

            fun md5(text: String): String =
                MistyCrypto.md5(text)

            fun sha1(text: String): String =
                MistyCrypto.sha1(text)

            fun sha256(text: String): String =
                MistyCrypto.sha256(text)

            fun hmacSha256(data: String, key: String): String =
                MistyCrypto.hmacSha256(data, key)

            /**
             * AES-CBC/PKCS5Padding，加密为 Base64 字符串。
             * - key / iv 为 UTF-8 字符串，将在底层规范化到 16 字节（不足补 0，超出截断）。
             * - 如果 iv 为空或 null，则使用全 0 IV。
             */
            fun aesEncryptToBase64(plainText: String, key: String, iv: String?): String =
                MistyCrypto.aesEncryptToBase64(plainText, key, iv)

            fun aesDecryptFromBase64(cipherBase64: String, key: String, iv: String?): String =
                MistyCrypto.aesDecryptFromBase64(cipherBase64, key, iv)

            /**
             * AES-ECB/PKCS5Padding，加解密为 Base64 字符串。
             * - 无 IV，仅使用 key。
             */
            fun aesEcbEncryptToBase64(plainText: String, key: String): String =
                MistyCrypto.aesEcbEncryptToBase64(plainText, key)

            fun aesEcbDecryptFromBase64(cipherBase64: String, key: String): String =
                MistyCrypto.aesEcbDecryptFromBase64(cipherBase64, key)

            // endregion
        }
    }

    /**
     * 加载 bootstrap.js SDK
     */
    private suspend fun loadBootstrapScript() {
        try {
            // 使用 expect/actual 读取资源文件
            val bootstrapBytes = readResourceBytes("files/bootstrap.js")
            val bootstrapScript = bootstrapBytes.decodeToString()
            quickJs.evaluate(bootstrapScript, "bootstrap.js") ?: ""
        } catch (e: Exception) {
            bridge.log("ERROR", "Failed to load bootstrap.js: ${e.message}")
            throw e
        }
    }

    /**
     * 执行 JavaScript 脚本
     * @param script JavaScript 代码
     * @return 执行结果（JSON 字符串）
     */
    suspend fun executeScript(script: String): String = withContext(Dispatchers.Default) {
        try {
            val result = quickJs.evaluate(script) ?: "null"
            result
        } catch (e: Exception) {
            bridge.log("ERROR", "JavaScript execution error: ${e.message}")
            throw e
        }
    }

    /**
     * 执行异步 JavaScript 函数
     * @param script JavaScript 代码（应返回 Promise）
     * @return 执行结果（JSON 字符串）
     */
    suspend fun executeAsyncScript(script: String): String = withContext(Dispatchers.Default) {
        try {
            // QuickJS 支持 Promise，但需要确保脚本返回 Promise
            val wrappedScript = """
                (async function() {
                    return await ($script);
                })();
            """.trimIndent()
            val result = quickJs.evaluate(wrappedScript) ?: "null"
            result
        } catch (e: Exception) {
            bridge.log("ERROR", "JavaScript async execution error: ${e.message}")
            throw e
        }
    }

    /**
     * 关闭引擎
     */
    fun close() {
        try {
            quickJs.close()
        } catch (e: Exception) {
            bridge.log("ERROR", "Failed to close JS engine: ${e.message}")
        }
    }
}

/**
 * 读取资源文件字节
 * 使用 expect/actual 实现跨平台支持
 */
expect fun readResourceBytes(path: String): ByteArray
