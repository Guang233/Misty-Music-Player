package com.guang.misty.engine

import java.io.InputStream

actual fun readResourceBytes(path: String): ByteArray {
    // 在 JVM 上，使用类加载器读取资源
    val classLoader = object {}.javaClass.classLoader
        ?: throw IllegalStateException("Cannot get class loader")

    val inputStream: InputStream = classLoader.getResourceAsStream(path)
        ?: throw IllegalArgumentException("Resource not found: $path")

    return inputStream.readBytes()
}
