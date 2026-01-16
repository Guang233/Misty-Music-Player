package com.guang.misty.util

import androidx.activity.compose.BackHandler as AndroidBackHandler
import androidx.compose.runtime.Composable

/**
 * Android 实现：使用 Activity 的 BackHandler
 * 
 * 预见性返回动画由系统自动处理（需要在 AndroidManifest.xml 中启用
 * android:enableOnBackInvokedCallback="true"）
 */
@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    AndroidBackHandler(enabled = enabled, onBack = onBack)
}
