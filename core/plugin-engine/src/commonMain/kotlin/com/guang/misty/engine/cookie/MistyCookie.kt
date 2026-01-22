package com.guang.misty.engine.cookie

import kotlinx.serialization.Serializable

/**
 * Cookie 数据模型
 */
@Serializable
data class MistyCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String = "/",
    val expiresAt: Long? = null, // Unix timestamp in milliseconds, null = session cookie
    val secure: Boolean = false,
    val httpOnly: Boolean = false,
    val sameSite: String? = null // "Strict" | "Lax" | "None"
) {
    /**
     * 检查 Cookie 是否已过期
     */
    fun isExpired(): Boolean {
        val expires = expiresAt ?: return false
        return System.currentTimeMillis() > expires
    }

    /**
     * 检查 Cookie 是否匹配指定域名
     */
    fun matchesDomain(targetDomain: String): Boolean {
        if (domain == targetDomain) return true
        // 支持子域名匹配 (例如 .example.com 匹配 music.example.com)
        if (domain.startsWith(".")) {
            return targetDomain.endsWith(domain) || targetDomain == domain.substring(1)
        }
        return false
    }

    /**
     * 转换为 Cookie 字符串格式 (用于 HTTP 请求头)
     */
    fun toCookieString(): String = "$name=$value"

    companion object {
        /**
         * 从 Set-Cookie 字符串解析 Cookie
         */
        fun fromSetCookieString(setCookieHeader: String, defaultDomain: String): MistyCookie? {
            return try {
                val parts = setCookieHeader.split(";").map { it.trim() }
                if (parts.isEmpty()) return null

                // 第一部分是 name=value
                val nameValue = parts[0].split("=", limit = 2)
                if (nameValue.size != 2) return null

                val name = nameValue[0]
                val value = nameValue[1]

                var domain = defaultDomain
                var path = "/"
                var expiresAt: Long? = null
                var secure = false
                var httpOnly = false
                var sameSite: String? = null

                // 解析其他属性
                for (i in 1 until parts.size) {
                    val attr = parts[i]
                    when {
                        attr.startsWith("Domain=", ignoreCase = true) -> {
                            domain = attr.substring(7)
                        }
                        attr.startsWith("Path=", ignoreCase = true) -> {
                            path = attr.substring(5)
                        }
                        attr.startsWith("Expires=", ignoreCase = true) -> {
                            // 简化处理：跳过日期解析，使用 Max-Age 更可靠
                        }
                        attr.startsWith("Max-Age=", ignoreCase = true) -> {
                            val maxAge = attr.substring(8).toLongOrNull()
                            if (maxAge != null) {
                                expiresAt = System.currentTimeMillis() + maxAge * 1000
                            }
                        }
                        attr.equals("Secure", ignoreCase = true) -> {
                            secure = true
                        }
                        attr.equals("HttpOnly", ignoreCase = true) -> {
                            httpOnly = true
                        }
                        attr.startsWith("SameSite=", ignoreCase = true) -> {
                            sameSite = attr.substring(9)
                        }
                    }
                }

                MistyCookie(
                    name = name,
                    value = value,
                    domain = domain,
                    path = path,
                    expiresAt = expiresAt,
                    secure = secure,
                    httpOnly = httpOnly,
                    sameSite = sameSite
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * 登录结果
 */
@Serializable
data class LoginResult(
    val success: Boolean,
    val cookies: List<MistyCookie> = emptyList(),
    val error: String? = null
)
