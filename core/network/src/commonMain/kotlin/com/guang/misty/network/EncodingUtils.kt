package com.guang.misty.network

/**
 * ByteArray 扩展函数：Base64 编码
 * 使用 expect/actual 实现跨平台支持
 */
expect fun ByteArray.encodeBase64(): String

/**
 * String 扩展函数：Base64 解码
 * 使用 expect/actual 实现跨平台支持
 */
expect fun String.decodeBase64(): ByteArray

/**
 * ByteArray 扩展函数：Hex 编码
 */
expect fun ByteArray.encodeHex(): String

/**
 * String 扩展函数：Hex 解码
 */
expect fun String.decodeHex(): ByteArray
