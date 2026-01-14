package com.guang.misty.network

import java.util.*

actual fun ByteArray.encodeBase64(): String {
    return Base64.getEncoder().encodeToString(this)
}

actual fun String.decodeBase64(): ByteArray {
    return Base64.getDecoder().decode(this)
}

actual fun ByteArray.encodeHex(): String {
    return joinToString("") { "%02x".format(it) }
}

actual fun String.decodeHex(): ByteArray {
    require(length % 2 == 0) { "Hex string must have even length" }
    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}
