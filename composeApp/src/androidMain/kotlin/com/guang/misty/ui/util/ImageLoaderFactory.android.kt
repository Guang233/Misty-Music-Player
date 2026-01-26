package com.guang.misty.ui.util

import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import okio.FileSystem

/**
 * Android 平台的 ImageLoader 实现
 * 使用 Ktor 网络引擎来处理网络请求
 */
actual fun createImageLoader(context: PlatformContext): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            // 使用 Ktor 网络引擎（会自动使用 Android 引擎）
            add(KtorNetworkFetcherFactory())
        }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, 0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizeBytes(50L * 1024 * 1024) // 50MB
                .build()
        }
        .crossfade(true)
        .logger(DebugLogger())
        .build()
}
