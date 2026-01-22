package com.guang.misty.ui.screens.settings.plugins

import androidx.compose.runtime.Composable
import com.guang.misty.engine.cookie.MistyCookie
import com.guang.misty.ui.components.WebViewLoginDialog

/**
 * Android 平台的登录对话框实现
 * 使用 WebView 打开登录页面
 */
@Composable
actual fun PlatformLoginDialog(
    pluginId: String,
    pluginName: String,
    loginUrl: String,
    onDismiss: () -> Unit,
    onLoginSuccess: (List<MistyCookie>) -> Unit
) {
    WebViewLoginDialog(
        pluginId = pluginId,
        pluginName = pluginName,
        loginUrl = loginUrl,
        onDismiss = onDismiss,
        onLoginSuccess = onLoginSuccess
    )
}
