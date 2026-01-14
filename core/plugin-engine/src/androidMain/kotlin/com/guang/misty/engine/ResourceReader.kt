package com.guang.misty.engine

import java.io.InputStream

actual fun readResourceBytes(path: String): ByteArray {
    // 在 Android 上，使用 Context 读取资源
    // 这里我们需要通过类加载器读取
    val classLoader = object {}.javaClass.classLoader
        ?: throw IllegalStateException("Cannot get class loader")

    val inputStream: InputStream = classLoader.getResourceAsStream(path)
        ?: throw IllegalArgumentException("Resource not found: $path")

    return inputStream.readBytes()
}
