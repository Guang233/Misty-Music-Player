package com.guang.misty.util

import androidx.compose.runtime.Composable

/**
 * 跨平台返回键处理
 * 
 * 在 Android 上会拦截系统返回键
 * 在 Desktop 上无操作（可扩展支持 ESC 键）
 */
@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
