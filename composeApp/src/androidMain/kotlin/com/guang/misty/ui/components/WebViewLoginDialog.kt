package com.guang.misty.ui.components

import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.guang.misty.engine.cookie.MistyCookie
import com.guang.misty.engine.cookie.WebViewCookieHelper
import misty.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import java.net.URI

/**
 * WebView 登录对话框
 * 
 * @param pluginId 插件 ID
 * @param pluginName 插件名称
 * @param loginUrl 登录页面 URL
 * @param onDismiss 关闭回调
 * @param onLoginSuccess 登录成功回调，返回提取的 Cookie 列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewLoginDialog(
    pluginId: String,
    pluginName: String,
    loginUrl: String,
    onDismiss: () -> Unit,
    onLoginSuccess: (List<MistyCookie>) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf(loginUrl) }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 顶部栏
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(Res.string.plugin_login_title, pluginName))
                            if (currentUrl != loginUrl) {
                                Text(
                                    text = stringResource(Res.string.plugin_login_current_url, currentUrl),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.action_close))
                        }
                    },
                    actions = {
                        // 完成按钮
                        TextButton(
                            onClick = {
                                // 提取 Cookie
                                val cookies = extractCookiesFromWebView(loginUrl)
                                if (cookies.isNotEmpty()) {
                                    onLoginSuccess(cookies)
                                    onDismiss()
                                }
                            }
                        ) {
                            Text(stringResource(Res.string.action_done))
                        }
                    }
                )

                // 加载进度条
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // WebView
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                useWideViewPort = true
                                loadWithOverviewMode = true
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: android.graphics.Bitmap?
                                ) {
                                    isLoading = true
                                    currentUrl = url ?: loginUrl
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    currentUrl = url ?: loginUrl
                                }
                            }

                            loadUrl(loginUrl)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )

                // 底部提示
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(Res.string.plugin_login_complete_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 从 WebView 的 CookieManager 中提取 Cookie
 */
private fun extractCookiesFromWebView(url: String): List<MistyCookie> {
    return try {
        val cookieManager = CookieManager.getInstance()
        val cookieString = cookieManager.getCookie(url) ?: return emptyList()
        
        val domain = try {
            URI(url).host ?: url
        } catch (e: Exception) {
            url
        }
        
        WebViewCookieHelper.parseCookieString(cookieString, domain)
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}
