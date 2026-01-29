package com.guang.misty.util

import java.awt.Desktop
import java.io.File

actual fun openFolder(path: String) {
    try {
        val file = File(path)
        if (file.exists()) {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

actual fun shareFile(filePath: String, mimeType: String) {
    // 桌面端不支持分享，直接打开文件夹
    openFolder(File(filePath).parent ?: filePath)
}

actual val isDesktopPlatform: Boolean = true
