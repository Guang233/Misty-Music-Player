# Cookie 管理功能实现说明

## 概述

为 Misty 插件系统添加了完整的 Cookie 管理和用户认证功能,支持插件访问需要登录的音乐平台。

## 架构

```
Plugin (JavaScript)
    ↓ misty.auth API
MistyBridge
    ↓
MistyCookieManager
    ↓
┌──────────────┬──────────────┐
│ CookieStorage│ LoginHandler │
└──────────────┴──────────────┘
      ↓                ↓
┌─────────┐      ┌─────────┐
│ Android │      │ Desktop │
└─────────┘      └─────────┘
```

## 核心组件

### 1. 数据模型 (`MistyCookie`)
- 完整的 Cookie 属性支持
- 过期时间管理
- 域名匹配
- Set-Cookie 字符串解析

### 2. 存储层 (`CookieStorage` - expect/actual)

#### Android 实现
- 使用 `SharedPreferences` 存储
- AES-ECB 加密 (基于应用包名 + Android ID)
- **初始化**: 需要在 `Application.onCreate()` 中调用:
  ```kotlin
  CookieStorage.initialize(applicationContext)
  ```

#### Desktop 实现
- 使用文件存储 (`~/.misty/cookies/`)
- AES-ECB 加密 (基于用户名 + 操作系统)
- 无需额外初始化

### 3. 登录处理器 (`LoginHandler` - expect/actual)

#### Android 实现
- 预留 WebView 集成接口
- **需要在 UI 层实现**:
  ```kotlin
  val loginHandler = LoginHandler()
  loginHandler.setLoginCallback { pluginId, loginUrl ->
      // 打开 WebView 登录页面
      // 拦截 Cookie 并返回
      LoginResult(success = true, cookies = extractedCookies)
  }
  ```
- 提供 `WebViewCookieHelper` 辅助类用于提取 Cookie

#### Desktop 实现
- 打开系统默认浏览器
- 弹出 Swing 对话框让用户粘贴 Cookie
- 支持多种 Cookie 格式:
  - 浏览器开发者工具格式 (`name=value; name2=value2`)
  - Set-Cookie 格式 (每行一个)
  - JSON 数组格式

### 4. JavaScript API (`misty.auth`)

```javascript
// 登录
var result = misty.auth.login(loginUrl);

// 获取 Cookie
var cookies = misty.auth.getCookies(domain?);

// 手动设置 Cookie
misty.auth.setCookies(cookies);

// 清除 Cookie
misty.auth.clearCookies();

// 转换为请求头字符串
var header = misty.auth.toCookieString(cookies);
```

## 使用说明

### 插件开发者

参考 `PLUGIN_DEV_GUIDE.md` 中的 "Cookie 和认证管理" 章节。

完整示例:
```javascript
getAudioResource: function(songId, quality) {
    // 1. 获取已保存的 Cookie
    var cookies = misty.auth.getCookies("music.example.com");
    var headers = {};
    
    if (cookies.length > 0) {
        headers["Cookie"] = misty.auth.toCookieString(cookies);
    }
    
    // 2. 请求资源
    var resp = misty.http.get(url, headers);
    var data = JSON.parse(resp);
    
    // 3. 如果需要登录
    if (data.code === 401) {
        var loginResult = misty.auth.login("https://music.example.com/login");
        if (!loginResult.success) {
            return misty.audio.errorResult(songId, quality, "登录失败");
        }
        
        // 4. 重试请求
        cookies = misty.auth.getCookies("music.example.com");
        headers["Cookie"] = misty.auth.toCookieString(cookies);
        resp = misty.http.get(url, headers);
        data = JSON.parse(resp);
    }
    
    return misty.audio.successResult(songId, quality, data.actualQuality, data.url);
}
```

### 应用集成

#### Android

1. **初始化 CookieStorage** (Application.kt):
```kotlin
class MistyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CookieStorage.initialize(this)
    }
}
```

2. **实现 WebView 登录** (在 UI 模块):
```kotlin
// 创建 LoginHandler 并设置回调
val loginHandler = LoginHandler()
loginHandler.setLoginCallback { pluginId, loginUrl ->
    // 在主线程打开 WebView Activity/Dialog
    val cookies = openWebViewLogin(loginUrl)
    LoginResult(success = true, cookies = cookies)
}

// 创建 Bridge 时传入
val bridge = StandardMistyBridge(httpClient, cookieStorage, loginHandler)
```

3. **WebView Cookie 提取示例**:
```kotlin
val webView = WebView(context)
webView.settings.javaScriptEnabled = true

webView.webViewClient = object : WebViewClient() {
    override fun onPageFinished(view: WebView?, url: String?) {
        val cookieManager = CookieManager.getInstance()
        val cookieString = cookieManager.getCookie(url)
        val cookies = WebViewCookieHelper.parseCookieString(
            cookieString, 
            extractDomain(url)
        )
        // 返回给 LoginHandler
    }
}

webView.loadUrl(loginUrl)
```

#### Desktop

Desktop 端无需额外配置,开箱即用:
- 自动打开系统浏览器
- 自动弹出 Cookie 输入对话框
- 支持用户复制粘贴 Cookie

## 安全特性

- ✅ **AES 加密存储**: 所有 Cookie 使用 AES-ECB 加密
- ✅ **设备密钥绑定**: Android 使用 ANDROID_ID,Desktop 使用用户名
- ✅ **插件隔离**: 每个插件的 Cookie 独立存储
- ✅ **自动过期**: 读取时自动过滤过期 Cookie
- ✅ **域名匹配**: Cookie 仅在匹配域名下使用
- ✅ **沙箱安全**: 插件无法访问其他插件的 Cookie

## 文件结构

```
core/plugin-engine/src/
├── commonMain/kotlin/com/guang/misty/engine/
│   ├── cookie/
│   │   ├── MistyCookie.kt              # Cookie 数据模型
│   │   ├── CookieStorage.kt            # 存储接口 (expect)
│   │   ├── LoginHandler.kt             # 登录处理器接口 (expect)
│   │   └── MistyCookieManager.kt       # Cookie 管理器
│   ├── bridge/
│   │   ├── MistyBridge.kt              # 桥接接口 (扩展 Cookie API)
│   │   └── StandardMistyBridge.kt      # 标准实现
│   └── MistyJsEngine.kt                # JS 引擎 (注入 Cookie 函数)
│
├── androidMain/kotlin/com/guang/misty/engine/cookie/
│   ├── CookieStorage.android.kt        # SharedPreferences 实现
│   └── LoginHandler.android.kt         # WebView 回调实现
│
├── jvmMain/kotlin/com/guang/misty/engine/cookie/
│   ├── CookieStorage.jvm.kt            # 文件存储实现
│   └── LoginHandler.jvm.kt             # Swing 对话框实现
│
└── jvmMain/resources/files/
    └── bootstrap.js                    # 扩展 misty.auth API
```

## 后续优化

### 短期
- [ ] Android WebView 登录页面 UI 实现
- [ ] Cookie 自动刷新机制
- [ ] 支持 OAuth 2.0 回调

### 长期
- [ ] Desktop 端支持嵌入式浏览器 (JCEF)
- [ ] 支持本地 HTTP 服务器接收 OAuth 回调
- [ ] Cookie 同步功能 (跨设备)

## 测试建议

1. **单元测试**: Cookie 解析、过期判断、域名匹配
2. **集成测试**: 存储读写、加密解密
3. **E2E 测试**: 完整登录流程

## 常见问题

### Q: Android 端如何实现 WebView 登录?
A: 参考上方 "应用集成 > Android" 章节的示例代码。

### Q: Desktop 端用户不会复制 Cookie 怎么办?
A: 对话框中已提供详细步骤说明,未来可考虑添加视频教程链接。

### Q: 如何调试 Cookie 相关问题?
A: 使用 `misty.log` 输出 Cookie 信息,查看应用日志。

## 参考文档

- [PLUGIN_DEV_GUIDE.md](./PLUGIN_DEV_GUIDE.md) - 插件开发完整指南
- [MistyCookie.kt](./src/commonMain/kotlin/com/guang/misty/engine/cookie/MistyCookie.kt) - Cookie 数据模型
- [bootstrap.js](./src/jvmMain/resources/files/bootstrap.js) - JavaScript SDK
