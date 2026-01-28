package com.guang.misty.ui.util

import androidx.compose.ui.Modifier

/**
 * Android 平台不需要单独处理右键点击
 * 使用 combinedClickable 的 onLongClick 即可
 */
actual fun Modifier.onRightClick(onClick: () -> Unit): Modifier = this
