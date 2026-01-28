package com.guang.misty.ui.util

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.onClick
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton

/**
 * JVM (Desktop) 平台响应右键点击
 */
@OptIn(ExperimentalFoundationApi::class)
actual fun Modifier.onRightClick(onClick: () -> Unit): Modifier = this.onClick(
    matcher = PointerMatcher.mouse(PointerButton.Secondary),
    onClick = onClick
)
