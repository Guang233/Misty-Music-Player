package com.guang.misty.ui.util

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import okio.FileSystem

/**
 * JVM (Desktop) 平台的 ImageLoader 实现
 * 使用 Ktor CIO 引擎来处理网络请求
 */
actual fun createImageLoader(context: PlatformContext): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            // JVM 平台使用默认的 Ktor 配置（CIO 引擎）
            add(KtorNetworkFetcherFactory())
        }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizeBytes(256 * 1024 * 1024) // 256MB
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache")
                .maxSizeBytes(512L * 1024 * 1024) // 512MB
                .build()
        }
        .crossfade(true)
        .logger(DebugLogger())
        .build()
}
