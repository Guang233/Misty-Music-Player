package com.guang.misty.crypto

// 对于 Android（JVM），直接复用 JVM 实现

actual fun md5Bytes(input: ByteArray): ByteArray =
    md5Bytes(input)

actual fun sha1Bytes(input: ByteArray): ByteArray =
    sha1Bytes(input)

actual fun sha256Bytes(input: ByteArray): ByteArray =
    sha256Bytes(input)

actual fun hmacSha256Bytes(data: ByteArray, key: ByteArray): ByteArray =
    hmacSha256Bytes(data, key)

actual fun aesEncrypt(plain: ByteArray, key: ByteArray, iv: ByteArray?): ByteArray =
    aesEncrypt(plain, key, iv)

actual fun aesDecrypt(cipher: ByteArray, key: ByteArray, iv: ByteArray?): ByteArray =
    aesDecrypt(cipher, key, iv)

actual fun aesEcbEncrypt(plain: ByteArray, key: ByteArray): ByteArray =
    aesEcbEncrypt(plain, key)

actual fun aesEcbDecrypt(cipher: ByteArray, key: ByteArray): ByteArray =
    aesEcbDecrypt(cipher, key)

actual fun ByteArray.encodeBase64(): String =
    encodeBase64()

actual fun String.decodeBase64(): ByteArray =
    decodeBase64()

actual fun ByteArray.encodeHex(): String =
    encodeHex()

actual fun String.decodeHex(): ByteArray =
    decodeHex()

