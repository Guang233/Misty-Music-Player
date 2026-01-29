package com.guang.misty.util

import android.content.Intent
import androidx.core.content.FileProvider
import com.guang.misty.data.settings.AndroidContextHolder
import java.io.File

actual fun openFolder(path: String) {
    // Android 不支持直接打开文件夹
    // 可以使用文件管理器 Intent，但体验不好
    // 这里留空，Android 端使用分享功能
}

actual fun shareFile(filePath: String, mimeType: String) {
    try {
        val context = AndroidContextHolder.context
        val file = File(filePath)
        
        if (!file.exists()) return
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        val chooserIntent = Intent.createChooser(shareIntent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

actual val isDesktopPlatform: Boolean = false
