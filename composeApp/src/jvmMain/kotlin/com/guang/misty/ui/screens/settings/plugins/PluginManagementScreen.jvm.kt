package com.guang.misty.ui.screens.settings.plugins

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.guang.misty.engine.cookie.MistyCookie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import misty.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import java.awt.Desktop
import java.net.URI

/**
 * Desktop (JVM) implementation of PlatformLoginDialog
 *
 * Opens the login URL in the system browser and shows a dialog
 * with instructions for manually pasting cookies.
 */
@Composable
actual fun PlatformLoginDialog(
    pluginId: String,
    pluginName: String,
    loginUrl: String,
    onDismiss: () -> Unit,
    onLoginSuccess: (List<MistyCookie>) -> Unit
) {
    val scope = rememberCoroutineScope()
    var cookieText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Pre-fetch localized strings in Composable context
    val errorEmptyMsg = stringResource(Res.string.plugin_login_desktop_error_empty)
    val errorParsePrefix = stringResource(Res.string.plugin_login_desktop_error_parse, "PLACEHOLDER")
        .substringBefore("PLACEHOLDER") // Extract prefix before placeholder

    // Open system browser when dialog appears
    LaunchedEffect(loginUrl) {
        withContext(Dispatchers.IO) {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI(loginUrl))
                }
            } catch (e: Exception) {
                errorMessage = "无法打开浏览器: ${e.message}"
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(600.dp)
                .heightIn(min = 400.dp, max = 600.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = stringResource(Res.string.plugin_login_title, pluginName),
                    style = MaterialTheme.typography.headlineSmall
                )

                Divider()

                // Instructions
                Text(
                    text = stringResource(Res.string.plugin_login_desktop_title),
                    style = MaterialTheme.typography.titleMedium
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(Res.string.plugin_login_desktop_step1),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(Res.string.plugin_login_desktop_step2),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(Res.string.plugin_login_desktop_step3),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedCard(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "document.cookie",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                    Text(
                        text = stringResource(Res.string.plugin_login_desktop_step4),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(Res.string.plugin_login_desktop_step5),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // URL display
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = loginUrl,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Cookie input field
                OutlinedTextField(
                    value = cookieText,
                    onValueChange = {
                        cookieText = it
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = { Text(stringResource(Res.string.plugin_login_desktop_cookie_hint)) },
                    placeholder = { Text(stringResource(Res.string.plugin_login_desktop_cookie_placeholder)) },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    enabled = !isProcessing
                )

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isProcessing
                    ) {
                        Text(stringResource(Res.string.action_cancel))
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isProcessing = true
                                errorMessage = null

                                try {
                                    val cookies = parseCookieString(cookieText, loginUrl)
                                    if (cookies.isEmpty()) {
                                        errorMessage = errorEmptyMsg
                                    } else {
                                        onLoginSuccess(cookies)
                                        onDismiss()
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "$errorParsePrefix${e.message ?: "Unknown"}"
                                } finally {
                                    isProcessing = false
                                }
                            }
                        },
                        enabled = !isProcessing && cookieText.isNotBlank()
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(stringResource(Res.string.action_done))
                    }
                }
            }
        }
    }
}

/**
 * Parse cookie string from browser console output
 * Supports format: "name1=value1; name2=value2; ..."
 */
private fun parseCookieString(cookieString: String, loginUrl: String): List<MistyCookie> {
    val domain = try {
        URI(loginUrl).host
    } catch (e: Exception) {
        "localhost"
    }

    return cookieString
        .split(";")
        .mapNotNull { part ->
            val trimmed = part.trim()
            if (trimmed.isEmpty()) return@mapNotNull null

            val equalIndex = trimmed.indexOf('=')
            if (equalIndex <= 0) return@mapNotNull null

            val name = trimmed.substring(0, equalIndex).trim()
            val value = trimmed.substring(equalIndex + 1).trim()

            if (name.isEmpty()) return@mapNotNull null

            MistyCookie(
                name = name,
                value = value,
                domain = domain,
                path = "/",
                expiresAt = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000, // 30 days
                secure = loginUrl.startsWith("https://", ignoreCase = true),
                httpOnly = false
            )
        }
}
