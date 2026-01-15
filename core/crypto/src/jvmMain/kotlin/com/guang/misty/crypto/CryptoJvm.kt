package com.guang.misty.crypto

import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

// region Hash

actual fun md5Bytes(input: ByteArray): ByteArray =
    digest("MD5", input)

actual fun sha1Bytes(input: ByteArray): ByteArray =
    digest("SHA-1", input)

actual fun sha256Bytes(input: ByteArray): ByteArray =
    digest("SHA-256", input)

private fun digest(algorithm: String, input: ByteArray): ByteArray =
    MessageDigest.getInstance(algorithm).digest(input)

actual fun hmacSha256Bytes(data: ByteArray, key: ByteArray): ByteArray {
    val mac = Mac.getInstance("HmacSHA256")
    val keySpec = SecretKeySpec(key, "HmacSHA256")
    mac.init(keySpec)
    return mac.doFinal(data)
}

// endregion

// region AES (CBC/PKCS5Padding)

actual fun aesEncrypt(plain: ByteArray, key: ByteArray, iv: ByteArray?): ByteArray {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val keyBytes = normalizeKey(key)
    val ivBytes = normalizeIv(iv)
    val keySpec = SecretKeySpec(keyBytes, "AES")
    val ivSpec = IvParameterSpec(ivBytes)
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
    return cipher.doFinal(plain)
}

actual fun aesDecrypt(cipher: ByteArray, key: ByteArray, iv: ByteArray?): ByteArray {
    val aes = Cipher.getInstance("AES/CBC/PKCS5Padding")
    val keyBytes = normalizeKey(key)
    val ivBytes = normalizeIv(iv)
    val keySpec = SecretKeySpec(keyBytes, "AES")
    val ivSpec = IvParameterSpec(ivBytes)
    aes.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
    return aes.doFinal(cipher)
}

private fun normalizeKey(key: ByteArray): ByteArray {
    // 规范化到 16 字节（AES-128），不足补 0，超出截断
    val size = 16
    val result = ByteArray(size)
    val copyLen = minOf(key.size, size)
    System.arraycopy(key, 0, result, 0, copyLen)
    return result
}

private fun normalizeIv(iv: ByteArray?): ByteArray {
    val size = 16
    val result = ByteArray(size)
    if (iv == null || iv.isEmpty()) {
        return result // 默认全 0 IV
    }
    val copyLen = minOf(iv.size, size)
    System.arraycopy(iv, 0, result, 0, copyLen)
    return result
}

// endregion

// region AES-ECB

actual fun aesEcbEncrypt(plain: ByteArray, key: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    val keyBytes = normalizeKey(key)
    val keySpec = SecretKeySpec(keyBytes, "AES")
    cipher.init(Cipher.ENCRYPT_MODE, keySpec)
    return cipher.doFinal(plain)
}

actual fun aesEcbDecrypt(cipherBytes: ByteArray, key: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    val keyBytes = normalizeKey(key)
    val keySpec = SecretKeySpec(keyBytes, "AES")
    cipher.init(Cipher.DECRYPT_MODE, keySpec)
    return cipher.doFinal(cipherBytes)
}

// endregion

// region Base64

actual fun ByteArray.encodeBase64(): String =
    Base64.getEncoder().encodeToString(this)

actual fun String.decodeBase64(): ByteArray =
    Base64.getDecoder().decode(this)

// endregion

// region Hex

actual fun ByteArray.encodeHex(): String =
    joinToString("") { "%02x".format(it) }

actual fun String.decodeHex(): ByteArray {
    require(length % 2 == 0) { "Hex string must have even length" }
    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

// endregion

