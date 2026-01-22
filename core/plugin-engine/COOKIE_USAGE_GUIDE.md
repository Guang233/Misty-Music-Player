# Cookie 管理功能使用指南

## ✅ 实现状态

所有核心功能已完全实现并通过编译测试：

- ✅ `CookieStorage` (expect/actual)
  - ✅ Android 实现 (SharedPreferences + AES 加密)
  - ✅ Desktop 实现 (文件存储 + AES 加密)
- ✅ `LoginHandler` (expect/actual)
  - ✅ Android 实现 (WebView 回调接口)
  - ✅ Desktop 实现 (系统浏览器 + 对话框)
- ✅ `MistyCookieManager` (跨平台核心逻辑)
- ✅ `StandardMistyBridge` (集成 Cookie 功能)
- ✅ JavaScript API (`misty.auth`)
- ✅ 文档完善

## 快速开始

### 1. Android 平台初始化

在你的 `Application` 类中初始化 `CookieStorage`:

```kotlin
// Application.kt
package com.guang.misty

import android.app.Application
import com.guang.misty.engine.cookie.CookieStorage

class MistyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 初始化 Cookie 存储 (必须)
        CookieStorage.initialize(this)
    }
}
```

在 `AndroidManifest.xml` 中注册:

```xml
<application
    android:name=".MistyApplication"
    ...>
</application>
```

### 2. Desktop 平台

Desktop 平台无需额外初始化，开箱即用！

### 3. 在代码中使用

#### 方式 A: 使用 `StandardMistyBridge` (推荐)

```kotlin
import com.guang.misty.engine.MistyJsEngine
import com.guang.misty.engine.MistyPluginManager
import com.guang.misty.engine.bridge.StandardMistyBridge
import com.guang.misty.engine.cookie.CookieStorage
import com.guang.misty.engine.cookie.LoginHandler
import com.guang.misty.network.MistyHttpClient

// 创建 Bridge
val httpClient = MistyHttpClient()
val cookieStorage = CookieStorage()
val loginHandler = LoginHandler()
val bridge = StandardMistyBridge(httpClient, cookieStorage, loginHandler)

// 创建引擎
val jsEngine = MistyJsEngine(bridge)
val pluginManager = MistyPluginManager(jsEngine)

// 加载插件
pluginManager.loadPlugin("my-plugin", pluginCode)
```

#### 方式 B: Android WebView 登录集成

如果你想在 Android 端实现自定义的 WebView 登录界面:

```kotlin
// 在创建 LoginHandler 后设置回调
val loginHandler = LoginHandler()

loginHandler.setLoginCallback { pluginId, loginUrl ->
    // 在主线程显示 WebView
    runOnUiThread {
        showLoginWebView(pluginId, loginUrl) { cookies ->
            // 用户登录成功后返回 Cookie
            LoginResult(
                success = true,
                cookies = cookies
            )
        }
    }
}
```

WebView Cookie 提取示例:

```kotlin
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.CookieManager
import com.guang.misty.engine.cookie.WebViewCookieHelper

fun showLoginWebView(pluginId: String, loginUrl: String, onComplete: (List<MistyCookie>) -> Unit) {
    val webView = WebView(context)
    webView.settings.javaScriptEnabled = true
    
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            // 用户可以手动触发"登录完成"按钮
            // 或者检测特定 URL 模式自动完成
        }
    }
    
    // 显示 WebView 对话框
    val dialog = AlertDialog.Builder(context)
        .setTitle("登录 - $pluginId")
        .setView(webView)
        .setPositiveButton("完成") { _, _ ->
            // 提取 Cookie
            val cookieManager = CookieManager.getInstance()
            val cookieString = cookieManager.getCookie(loginUrl) ?: ""
            val domain = extractDomain(loginUrl)
            val cookies = WebViewCookieHelper.parseCookieString(cookieString, domain)
            onComplete(cookies)
        }
        .setNegativeButton("取消") { _, _ ->
            onComplete(emptyList())
        }
        .create()
    
    webView.loadUrl(loginUrl)
    dialog.show()
}

private fun extractDomain(url: String): String {
    return try {
        val uri = java.net.URI(url)
        uri.host ?: url
    } catch (e: Exception) {
        url
    }
}
```

## 插件开发者使用示例

插件开发者只需使用 JavaScript API:

```javascript
// 插件代码
MistyPlugins['my-plugin'] = {
    meta: {
        id: 'my-plugin',
        name: 'My Music Plugin',
        capabilities: ['SEARCH', 'AUDIO_RESOURCES']
    },
    
    getAudioResource: function(songId, quality) {
        try {
            // 1. 尝试使用已保存的 Cookie
            var cookies = misty.auth.getCookies("music.example.com");
            var headers = {};
            
            if (cookies.length > 0) {
                headers["Cookie"] = misty.auth.toCookieString(cookies);
            }
            
            // 2. 请求音频资源
            var resp = misty.http.get(
                "https://api.music.example.com/song/" + songId,
                headers
            );
            var data = JSON.parse(resp);
            
            // 3. 如果需要登录
            if (data.code === 401 || data.code === 403) {
                misty.log.info("需要登录");
                
                var loginResult = misty.auth.login("https://music.example.com/login");
                
                if (!loginResult.success) {
                    return misty.audio.errorResult(songId, quality, "登录失败");
                }
                
                // 4. 登录成功，重试
                cookies = misty.auth.getCookies("music.example.com");
                headers["Cookie"] = misty.auth.toCookieString(cookies);
                
                resp = misty.http.get(
                    "https://api.music.example.com/song/" + songId,
                    headers
                );
                data = JSON.parse(resp);
            }
            
            // 5. 返回音频资源
            return misty.audio.successResult(
                songId,
                quality,
                data.quality,
                data.url,
                { format: data.format, bitrateKbps: data.bitrate }
            );
        } catch (err) {
            return misty.audio.errorResult(songId, quality, err.message);
        }
    }
};
```

## 测试

运行测试验证功能:

```bash
# 测试 JVM 平台
./gradlew :core:plugin-engine:jvmTest

# 测试 Android 平台
./gradlew :core:plugin-engine:testDebugUnitTest
```

## 故障排除

### Android: `CookieStorage not initialized` 错误

**原因**: 忘记在 `Application.onCreate()` 中调用 `CookieStorage.initialize()`

**解决方案**: 参考上方"Android 平台初始化"部分

### Desktop: Cookie 输入对话框不显示

**原因**: 可能是图形环境问题或 Swing 未正确初始化

**解决方案**: 
- 确保在 Desktop 环境运行
- 检查是否有 `java.awt.HeadlessException` 错误

### 插件: `misty.auth` 未定义

**原因**: 使用了旧版本的 `bootstrap.js`

**解决方案**: 
- 重新构建项目
- 确保使用最新的 `bootstrap.js`

## 安全建议

1. **不要在日志中输出 Cookie 值**
   ```javascript
   // ❌ 错误
   misty.log.info("Cookie: " + cookie.value);
   
   // ✅ 正确
   misty.log.info("Cookie count: " + cookies.length);
   ```

2. **定期清理过期 Cookie**
   - Cookie 会自动过期，但建议提供"退出登录"功能
   ```javascript
   // 退出登录
   misty.auth.clearCookies();
   ```

3. **使用 HTTPS**
   - 尽量使用 `secure: true` 的 Cookie
   - 只在 HTTPS 连接上传输敏感 Cookie

## 更多文档

- [PLUGIN_DEV_GUIDE.md](./PLUGIN_DEV_GUIDE.md) - 完整的插件开发指南
- [COOKIE_IMPLEMENTATION.md](./COOKIE_IMPLEMENTATION.md) - 技术实现细节
- [StandardMistyBridge.kt](./src/commonMain/kotlin/com/guang/misty/engine/bridge/StandardMistyBridge.kt) - Bridge 实现

## 支持

遇到问题？请参考:
- 文档: `PLUGIN_DEV_GUIDE.md`
- 示例: `core/plugin-engine/src/jvmTest/kotlin/com/guang/misty/engine/MistyPluginEngineTest.kt`
- Issues: 提交 GitHub Issue
