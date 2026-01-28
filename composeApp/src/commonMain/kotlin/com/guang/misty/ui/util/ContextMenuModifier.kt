package com.guang.misty.ui.util

import androidx.compose.ui.Modifier

/**
 * 跨平台的上下文菜单修饰符
 * 在桌面端响应右键点击，在移动端通过 combinedClickable 的 onLongClick 处理
 */
expect fun Modifier.onRightClick(onClick: () -> Unit): Modifier
