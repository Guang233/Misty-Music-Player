package com.guang.misty.util

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import java.io.BufferedReader

/**
 * Android 文件选择器实现
 */
@Composable
actual fun FilePicker(
    show: Boolean,
    fileExtensions: List<String>,
    onResult: (FilePickerResult?) -> Unit
) {
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
                
                // 从 URI 获取文件名
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "plugin.js"
                
                onResult(FilePickerResult(
                    fileName = fileName,
                    content = content
                ))
            } catch (e: Exception) {
                onResult(null)
            }
        } else {
            onResult(null)
        }
    }
    
    LaunchedEffect(show) {
        if (show) {
            // MIME 类型：JavaScript 文件
            val mimeTypes = arrayOf(
                "application/javascript",
                "text/javascript", 
                "application/x-javascript",
                "text/plain",  // 备用，有些文件管理器不识别 js
                "*/*"  // 最后的备用
            )
            launcher.launch(mimeTypes)
        }
    }
}
