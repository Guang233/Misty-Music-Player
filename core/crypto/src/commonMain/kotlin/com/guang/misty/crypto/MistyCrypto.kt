package com.guang.misty.crypto

/**
 * Misty 公共加解密工具。
 *
 * 设计目标：
 * - 为 JS 脚本提供常见加解密能力（MD5/SHA 系列、HMAC、AES 等）
 * - 在 commonMain 定义 API，平台实现放在 actual 中
 */
object MistyCrypto {

    // region Hash 系列

    fun md5(text: String): String =
        md5Bytes(text.encodeToByteArray()).encodeHex()

    fun sha1(text: String): String =
        sha1Bytes(text.encodeToByteArray()).encodeHex()

    fun sha256(text: String): String =
        sha256Bytes(text.encodeToByteArray()).encodeHex()

    fun hmacSha256(data: String, key: String): String =
        hmacSha256Bytes(data.encodeToByteArray(), key.encodeToByteArray()).encodeHex()

    // endregion

    // region AES（CBC/PKCS5Padding，Base64 编码）

    /**
     * AES-CBC 加密，返回 Base64 编码结果。
     *
     * @param plainText 明文字符串（UTF-8）
     * @param key       密钥字符串（UTF-8，会在平台侧标准化到 16 字节）
     * @param iv        可选初始向量（UTF-8，16 字节，不传则使用全 0 IV）
     */
    fun aesEncryptToBase64(plainText: String, key: String, iv: String? = null): String {
        val plainBytes = plainText.encodeToByteArray()
        val keyBytes = key.encodeToByteArray()
        val ivBytes = iv?.encodeToByteArray()
        val cipherBytes = aesEncrypt(plainBytes, keyBytes, ivBytes)
        return cipherBytes.encodeBase64()
    }

    /**
     * AES-CBC 解密，输入为 Base64 编码的密文。
     */
    fun aesDecryptFromBase64(cipherBase64: String, key: String, iv: String? = null): String {
        val cipherBytes = cipherBase64.decodeBase64()
        val keyBytes = key.encodeToByteArray()
        val ivBytes = iv?.encodeToByteArray()
        val plainBytes = aesDecrypt(cipherBytes, keyBytes, ivBytes)
        return plainBytes.decodeToString()
    }

    // endregion

    // region AES-ECB（无 IV，Base64 编码）

    /**
     * AES-ECB 加密，返回 Base64 编码结果。
     *
     * @param plainText 明文字符串（UTF-8）
     * @param key       密钥字符串（UTF-8，会在平台侧规范化到 16 字节）
     */
    fun aesEcbEncryptToBase64(plainText: String, key: String): String {
        val plainBytes = plainText.encodeToByteArray()
        val keyBytes = key.encodeToByteArray()
        val cipherBytes = aesEcbEncrypt(plainBytes, keyBytes)
        return cipherBytes.encodeBase64()
    }

    /**
     * AES-ECB 解密，输入为 Base64 编码的密文。
     */
    fun aesEcbDecryptFromBase64(cipherBase64: String, key: String): String {
        val cipherBytes = cipherBase64.decodeBase64()
        val keyBytes = key.encodeToByteArray()
        val plainBytes = aesEcbDecrypt(cipherBytes, keyBytes)
        return plainBytes.decodeToString()
    }

    // endregion
}

// region expect API：各平台具体实现

expect fun md5Bytes(input: ByteArray): ByteArray

expect fun sha1Bytes(input: ByteArray): ByteArray

expect fun sha256Bytes(input: ByteArray): ByteArray

expect fun hmacSha256Bytes(data: ByteArray, key: ByteArray): ByteArray

expect fun aesEncrypt(plain: ByteArray, key: ByteArray, iv: ByteArray?): ByteArray

expect fun aesDecrypt(cipher: ByteArray, key: ByteArray, iv: ByteArray?): ByteArray

// AES-ECB（无 IV）
expect fun aesEcbEncrypt(plain: ByteArray, key: ByteArray): ByteArray

expect fun aesEcbDecrypt(cipherBytes: ByteArray, key: ByteArray): ByteArray

expect fun ByteArray.encodeBase64(): String

expect fun String.decodeBase64(): ByteArray

// Hex 工具
expect fun ByteArray.encodeHex(): String

expect fun String.decodeHex(): ByteArray

// endregion

