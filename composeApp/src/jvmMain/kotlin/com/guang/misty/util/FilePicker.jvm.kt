package com.guang.misty.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

/**
 * Desktop/JVM 文件选择器实现
 */
@Composable
actual fun FilePicker(
    show: Boolean,
    fileExtensions: List<String>,
    onResult: (FilePickerResult?) -> Unit
) {
    LaunchedEffect(show) {
        if (show) {
            val result = withContext(Dispatchers.IO) {
                showFileDialog(fileExtensions)
            }
            onResult(result)
        }
    }
}

/**
 * 显示文件选择对话框
 */
private fun showFileDialog(fileExtensions: List<String>): FilePickerResult? {
    val dialog = FileDialog(null as Frame?, "选择插件文件", FileDialog.LOAD)
    
    // 设置文件过滤器
    if (fileExtensions.isNotEmpty()) {
        dialog.filenameFilter = FilenameFilter { _, name ->
            fileExtensions.any { ext -> name.endsWith(".$ext", ignoreCase = true) }
        }
    }
    
    dialog.isVisible = true
    
    val directory = dialog.directory
    val fileName = dialog.file
    
    if (directory != null && fileName != null) {
        val file = File(directory, fileName)
        return try {
            val content = file.readText(Charsets.UTF_8)
            FilePickerResult(
                fileName = fileName,
                content = content
            )
        } catch (e: Exception) {
            null
        }
    }
    
    return null
}
