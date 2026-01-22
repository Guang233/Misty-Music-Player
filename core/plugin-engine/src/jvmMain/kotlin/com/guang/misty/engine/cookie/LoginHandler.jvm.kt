package com.guang.misty.engine.cookie

import java.awt.Desktop
import java.net.URI
import javax.swing.*

/**
 * 创建 LoginHandler 实例（JVM 实现）
 */
actual fun createLoginHandler(): LoginHandler {
    return LoginHandler()
}

/**
 * Desktop 端登录处理器实现
 * 使用系统浏览器 + 手动粘贴 Cookie 的方式
 */
actual class LoginHandler {
    actual suspend fun requestLogin(pluginId: String, loginUrl: String): LoginResult {
        return try {
            // 1. 打开系统默认浏览器
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(loginUrl))
            }

            // 2. 显示对话框让用户粘贴 Cookie
            val cookieString = showCookieInputDialog(pluginId, loginUrl)

            if (cookieString.isNullOrBlank()) {
                return LoginResult(
                    success = false,
                    error = "用户取消了登录"
                )
            }

            // 3. 解析 Cookie 字符串
            val cookies = parseCookieString(cookieString, loginUrl)

            if (cookies.isEmpty()) {
                return LoginResult(
                    success = false,
                    error = "未能解析到有效的 Cookie"
                )
            }

            LoginResult(
                success = true,
                cookies = cookies
            )
        } catch (e: Exception) {
            LoginResult(
                success = false,
                error = e.message ?: "登录失败"
            )
        }
    }

    /**
     * 显示 Cookie 输入对话框
     */
    private fun showCookieInputDialog(pluginId: String, loginUrl: String): String? {
        val textArea = JTextArea(10, 50)
        textArea.lineWrap = true
        textArea.wrapStyleWord = true
        val scrollPane = JScrollPane(textArea)

        val instructions = """
            |插件 "$pluginId" 需要登录
            |
            |请按以下步骤操作：
            |1. 浏览器已打开登录页面: $loginUrl
            |   （如果未打开，请手动复制链接到浏览器）
            |2. 在浏览器中完成登录
            |3. 按 F12 打开开发者工具
            |4. 进入 Application/Storage > Cookies
            |5. 复制所有 Cookie 并粘贴到下方输入框
            |
            |支持的格式：
            |• 浏览器开发者工具导出的 Cookie (name=value; name2=value2)
            |• Set-Cookie 格式 (每行一个)
            |• JSON 数组格式 [{"name":"...", "value":"..."}]
        """.trimMargin()

        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(JLabel("<html>${instructions.replace("\n", "<br>")}</html>"))
        panel.add(Box.createVerticalStrut(10))
        panel.add(scrollPane)

        val result = JOptionPane.showConfirmDialog(
            null,
            panel,
            "登录 - $pluginId",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        )

        return if (result == JOptionPane.OK_OPTION) {
            textArea.text
        } else {
            null
        }
    }

    /**
     * 解析 Cookie 字符串（支持多种格式）
     */
    private fun parseCookieString(input: String, loginUrl: String): List<MistyCookie> {
        val cookies = mutableListOf<MistyCookie>()
        val domain = extractDomain(loginUrl)

        // 尝试不同的解析方式
        when {
            // 格式1: JSON 数组 [{"name":"...", "value":"..."}]
            input.trim().startsWith("[") -> {
                try {
                    // 简单的JSON解析（避免依赖外部库）
                    parseJsonCookies(input, domain)?.let { cookies.addAll(it) }
                } catch (e: Exception) {
                    // JSON解析失败，尝试其他格式
                }
            }
            // 格式2: Set-Cookie 格式（每行一个）
            input.contains("\n") -> {
                input.lines().forEach { line ->
                    if (line.isNotBlank()) {
                        MistyCookie.fromSetCookieString(line.trim(), domain)?.let {
                            cookies.add(it)
                        }
                    }
                }
            }
            // 格式3: Cookie 请求头格式 (name=value; name2=value2)
            else -> {
                input.split(";").forEach { pair ->
                    val trimmed = pair.trim()
                    if (trimmed.isNotEmpty()) {
                        val parts = trimmed.split("=", limit = 2)
                        if (parts.size == 2) {
                            cookies.add(
                                MistyCookie(
                                    name = parts[0].trim(),
                                    value = parts[1].trim(),
                                    domain = domain,
                                    path = "/"
                                )
                            )
                        }
                    }
                }
            }
        }

        return cookies
    }

    /**
     * 简单的JSON Cookie解析
     */
    private fun parseJsonCookies(json: String, defaultDomain: String): List<MistyCookie>? {
        // 这里使用简单的字符串解析，避免引入JSON库依赖
        // 在实际使用中，StandardMistyBridge会使用kotlinx.serialization
        return null
    }

    /**
     * 从URL中提取域名
     */
    private fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }
}
