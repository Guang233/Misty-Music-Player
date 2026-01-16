package com.guang.misty.util

import androidx.compose.runtime.Composable

/**
 * 文件选择结果
 */
data class FilePickerResult(
    val fileName: String,
    val content: String
)

/**
 * 跨平台文件选择器
 * 
 * @param show 是否显示文件选择器
 * @param fileExtensions 允许的文件扩展名列表（如 ["js"]）
 * @param onResult 选择结果回调，null 表示取消
 */
@Composable
expect fun FilePicker(
    show: Boolean,
    fileExtensions: List<String> = listOf("js"),
    onResult: (FilePickerResult?) -> Unit
)
