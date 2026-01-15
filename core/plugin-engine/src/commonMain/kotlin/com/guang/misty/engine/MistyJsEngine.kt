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
            // 执行网络请求（从 JS 调用，需要同步包装）
            function("performRequest") { args ->
                val requestJson = args[0] as String
                runBlocking {
                    bridge.networkRequest(requestJson)
                }
            }

            // 记录日志
            function("log") { args ->
                val level = args[0] as String
                val msg = args[1] as String
                bridge.log(level, msg)
            }

            // region Crypto API：给 JS 暴露常见加解密能力

            function("md5") { args ->
                val text = args[0] as String
                MistyCrypto.md5(text)
            }

            function("sha1") { args ->
                val text = args[0] as String
                MistyCrypto.sha1(text)
            }

            function("sha256") { args ->
                val text = args[0] as String
                MistyCrypto.sha256(text)
            }

            function("hmacSha256") { args ->
                val data = args[0] as String
                val key = args[1] as String
                MistyCrypto.hmacSha256(data, key)
            }

            // AES-CBC/PKCS5Padding，加密为 Base64 字符串
            function("aesEncryptToBase64") { args ->
                val plainText = args[0] as String
                val key = args[1] as String
                val iv = args[2] as? String
                MistyCrypto.aesEncryptToBase64(plainText, key, iv)
            }

            function("aesDecryptFromBase64") { args ->
                val cipherBase64 = args[0] as String
                val key = args[1] as String
                val iv = args[2] as? String
                MistyCrypto.aesDecryptFromBase64(cipherBase64, key, iv)
            }

            // AES-ECB/PKCS5Padding，加解密为 Base64 字符串
            function("aesEcbEncryptToBase64") { args ->
                val plainText = args[0] as String
                val key = args[1] as String
                MistyCrypto.aesEcbEncryptToBase64(plainText, key)
            }

            function("aesEcbDecryptFromBase64") { args ->
                val cipherBase64 = args[0] as String
                val key = args[1] as String
                MistyCrypto.aesEcbDecryptFromBase64(cipherBase64, key)
            }

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
            // 执行脚本，不需要返回值
            quickJs.evaluate<Unit>(bootstrapScript, "bootstrap.js")
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
            val result: String? = quickJs.evaluate<String?>(script)
            result ?: "null"
        } catch (e: Exception) {
            bridge.log("ERROR", "JavaScript execution error: ${e.message}")
            throw e
        }
    }

    /**
     * 执行异步 JavaScript 函数
     * @param script JavaScript 代码（应返回 Promise 或 async IIFE）
     * @return 执行结果（JSON 字符串）
     */
    suspend fun executeAsyncScript(script: String): String = withContext(Dispatchers.Default) {
        try {
            // QuickJs 在执行时会自动处理 Promise
            // 返回 Any? 然后转换为 String
            val result: Any? = quickJs.evaluate<Any?>(script)
            result?.toString() ?: "null"
        } catch (e: Exception) {
            bridge.log("ERROR", "JavaScript async execution error: ${e.message}")
            throw e
        }
    }

    /**
     * 记录日志（直接调用 bridge，不经过 JS）
     */
    fun log(level: String, msg: String) {
        bridge.log(level, msg)
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
