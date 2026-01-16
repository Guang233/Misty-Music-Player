## Misty 插件开发规范（草案）

> 说明：本规范面向社区插件作者，当前仅覆盖 Android & Desktop 的 JS 插件。  
> 本文件暂时放在主仓库中，后续会移动到单独的插件仓库。

---

## 目录

- [插件总体设计](#插件总体设计)
- [运行环境与全局对象](#运行环境与全局对象)
- [插件基本结构](#插件基本结构)
- [网络访问规范（misty.http）](#网络访问规范mistyhttp)
- [日志输出规范（misty.log）](#日志输出规范mistylog)
- [音频资源（misty.audio）](#音频资源mistyaudio)
- [加解密工具（misty.crypto）](#加解密工具mistycrypto)
- [数据模型与返回格式](#数据模型与返回格式)
  - [插件元信息 `MistyPluginMeta`](#插件元信息-mistypluginmeta)
  - [艺术家 `MistyArtist`](#艺术家-mistyartist)
  - [歌曲 `MistySong`](#歌曲-mistysong)
  - [歌单 `MistyPlaylist`](#歌单-mistyplaylist)
  - [专辑 `MistyAlbum`](#专辑-mistyalbum)
  - [歌词 `MistyLyricBundle`](#歌词-mistylyricbundle)
  - [音频资源 `MistyAudioResourceResult`](#音频资源-mistyaudioresourceresult)
- [插件需要实现的接口](#插件需要实现的接口)
  - [`search(keyword, page)` 搜索歌曲](#searchkeyword-page-搜索歌曲)
  - [`getPlaylist(playlistId)` 获取歌单](#getplaylistplaylistid-获取歌单)
  - [`getAlbum(albumId)` 获取专辑](#getalbumalbumid-获取专辑)
  - [`getLyrics(songId)` 获取歌词](#getlyricssongid-获取歌词)
  - [`getAudioResource(songId, quality)` 获取指定音质音频资源](#getaudioresourcesongid-quality-获取指定音质音频资源)
- [错误处理与兼容性](#错误处理与兼容性)
- [完整示例模板](#完整示例模板)

---

## 插件总体设计

- **语言**：JavaScript / TypeScript（运行在 QuickJS 上，目前以 JavaScript 为主）。
- **加载方式**：核心引擎会加载插件 JS 文件，然后执行。
- **全局约定**：
  - 所有插件均挂在全局对象 `MistyPlugins` 下。
  - 每个插件使用自己的 `pluginId` 作为 key，例如：`MistyPlugins["netease"] = { ... }`。
- **核心职责**：
  - 搜索歌曲
  - 获取歌单与专辑详情
  - 获取歌词（多种类型、格式）
  - 获取多音质音频资源
  - 通过 `misty.http` 访问网络，通过 `misty.log` 输出日志

> ⚠️ **重要提示**：由于 QuickJS-kt 引擎的限制，**插件函数必须使用同步函数（普通 `function`）**，而不是 `async function`。详见下文「同步函数要求」章节。

---

## 运行环境与全局对象

运行环境为 QuickJS，加载 `bootstrap.js` 后会提供以下全局对象：

- **`MistyPlugins`**：插件注册表（Object）。
- **`misty`**：SDK 根命名空间（由系统注入），包含：
  - `misty.http`：网络请求。
  - `misty.log`：日志。
  - `misty.audio`：多音质辅助工具。

你**不需要**自己定义这些对象，只需要往 `MistyPlugins` 里挂插件即可。

---

## 同步函数要求（重要）

由于 Misty 使用的 QuickJS-kt 引擎的技术限制，**Kotlin 侧调用 `evaluate()` 时不会自动等待 JavaScript Promise**。这意味着如果你使用 `async function`，返回的将是 Promise 对象本身，而不是 Promise 解析后的值，导致序列化失败。

### ❌ 错误写法：使用 async function

```javascript
// 错误！async 函数返回 Promise，Kotlin 侧无法正确解析
async search(keyword, page) {
  const resp = await misty.http.get(...);
  return JSON.parse(resp);
}
```

### ✅ 正确写法：使用普通 function

```javascript
// 正确！使用普通函数，misty.http 内部会同步返回结果
search: function(keyword, page) {
  var resp = misty.http.get(...);
  return JSON.parse(resp);
}
```

### 为什么 `misty.http` 可以直接使用？

`misty.http` 的网络请求由 Kotlin 侧的桥接层（Bridge）实现，它会**同步阻塞**直到网络请求完成，然后返回结果。因此，即使网络请求本身是异步的，在 JavaScript 插件中调用 `misty.http.get()` 等方法时，你可以直接获取返回值，无需使用 `await`。

### 最佳实践

1. **不要使用 `async`/`await` 关键字**
2. **使用 `var` 或 `let` 声明变量**（QuickJS 支持 ES6，但建议保持兼容性）
3. **使用字符串拼接而非模板字符串**（提高兼容性）
4. **使用 `function` 关键字定义方法**

---

## 插件基本结构

每个插件文件应至少包含如下结构：

```javascript
// 1. 确保 MistyPlugins 存在（一般 bootstrap.js 已处理，写一遍也无妨）
if (typeof MistyPlugins === "undefined") {
  MistyPlugins = {};
}

// 2. 定义插件 ID（全局唯一，建议使用 "平台名" 或 "平台名_region"）
const PLUGIN_ID = "example";

// 3. 注册插件对象
MistyPlugins[PLUGIN_ID] = {
  // 必选：基础信息（MistyPluginMeta）
  meta: {
    id: PLUGIN_ID,                 // 必须：与 PLUGIN_ID 一致
    name: "Example Source",        // 插件名称（展示用）
    author: "Your Name",           // 作者
    version: "1.0.0",              // 版本号
    description: "Example music source plugin", // 简要描述
    homepage: "https://github.com/your/repo",   // 仓库或主页
    license: "MIT",                // 许可证

    // 与音源平台相关的信息（可选）
    sourceName: "Example Music",
    sourceHomepage: "https://music.example.com",

    // 声明该插件实现了哪些能力（全部大写）
    capabilities: [
      "SEARCH",
      "PLAYLIST",
      "ALBUM",
      "LYRICS",
      "AUDIO_RESOURCES",
    ],

    // 支持的地区/区域标识（可选），例如 ["CN", "US"]
    supportRegions: ["CN"],

    // 免责声明（可选）：
    // 例如说明：本插件由社区维护，与 Misty 作者及项目无关
    disclaimer: "This plugin is provided by the community. Misty only loads community scripts and does not provide any music sources.",
  },

  // 必选：搜索歌曲（必须实现）
  search: function(keyword, page) { /* ... */ },

  // 建议实现：获取歌单详情
  getPlaylist: function(playlistId) { /* ... */ },

  // 建议实现：获取专辑详情
  getAlbum: function(albumId) { /* ... */ },

  // 建议实现：获取歌词
  getLyrics: function(songId) { /* ... */ },

  // 必选：获取指定音质音频资源（必须实现，用于实际播放）
  getAudioResource: function(songId, quality) { /* ... */ },
};
```

---

## 网络访问规范（`misty.http`）

引擎为插件提供统一的 HTTP 客户端，自动管理 Cookie、编码等。

### 通用方法

```javascript
const resp = await misty.http.request({
  url: "https://api.example.com/path",
  method: "GET",                     // "GET" 或 "POST"，默认 "GET"
  headers: { "User-Agent": "..." },  // 可选
  body: "....",                      // 可选
  bodyEncoding: "string",            // "string" | "hex" | "base64"，默认 "string"
  responseEncoding: "string",        // "string" | "hex" | "base64"，默认 "string"
});
// resp = { statusCode, headers, body, error? }
```

### 快捷方法

```javascript
// GET：返回 body 字符串
const text = await misty.http.get(url, headers?, responseEncoding?);

// POST：返回 body 字符串
const text = await misty.http.post(
  url,
  body,
  headers?,
  bodyEncoding?,      // 默认 "string"
  responseEncoding?,  // 默认 "string"
);

// 兼容：获取二进制（base64/hex）
const base64Data = await misty.http.getBinary(url, headers?);
const hexData    = await misty.http.getBinaryHex(url, headers?);
```

**建议：**

- 大部分 Web API：使用 `bodyEncoding: "string"`, `responseEncoding: "string"`。
- 下载音频文件：使用 `responseEncoding: "base64"` 或 `"hex"`，再结合 `misty.audio` 构建 `MistyAudioResource`。

---

## 日志输出规范（`misty.log`）

```javascript
misty.log.info("search start: keyword=" + keyword);
misty.log.warn("rate limit approaching");
misty.log.error("request failed: " + err.message);
misty.log.debug("raw response: " + resp.body);
```

内部会由宿主应用收集并输出，方便调试与问题排查。

---

## 音频资源（`misty.audio`）

### 质量枚举

| 枚举值 | 说明 |
|--------|------|
| `STANDARD` | 标准音质 |
| `HIGH` | 高音质 |
| `LOSSLESS` | 无损音质 |
| `HI_RES` | Hi-Res 高解析度 |

**JS 必须返回这些大写字符串之一**，否则反序列化会失败。

### 按指定音质请求

Misty 采用「**按指定音质请求**」的设计：

1. 调用方（Misty App）请求某首歌的**指定音质**（如 `LOSSLESS`）
2. 插件尝试获取该音质资源
3. 如果请求音质不可用，插件应返回**实际可用的音质**（降级）
4. 调用方通过比较 `requestedQuality` 和 `resource.quality` 判断是否降级

### SDK 辅助方法

在 `bootstrap.js` 中，`misty.audio` 提供：

```javascript
misty.audio = {
  // 音质枚举
  Quality: {
    STANDARD: "STANDARD",
    HIGH: "HIGH",
    LOSSLESS: "LOSSLESS",
    HI_RES: "HI_RES",
  },

  // 创建单个资源对象
  createResource(options) { /* ... */ },

  // 创建请求结果（完整控制）
  createResult(songId, requestedQuality, resource, error) { /* ... */ },

  // 快速创建成功结果
  successResult(songId, requestedQuality, actualQuality, url, opts = {}) { /* ... */ },

  // 快速创建失败结果
  errorResult(songId, requestedQuality, error) { /* ... */ },
};
```

#### 方法说明

- **`createResource(options)`**：手动构建一个资源对象，字段与 `MistyAudioResource` 对应。
- **`createResult(songId, requestedQuality, resource, error)`**：创建完整的请求结果。
- **`successResult(songId, requestedQuality, actualQuality, url, opts)`**：快速创建成功结果。
- **`errorResult(songId, requestedQuality, error)`**：快速创建失败结果。

#### 示例：成功获取请求音质

```javascript
// 请求 LOSSLESS，成功返回 LOSSLESS
return misty.audio.successResult(
  songId,
  "LOSSLESS",    // 请求的音质
  "LOSSLESS",    // 实际返回的音质（相同，未降级）
  "https://.../lossless.flac",
  { format: "flac", bitrateKbps: 1000 }
);
```

#### 示例：降级返回

```javascript
// 请求 LOSSLESS，但该歌曲没有无损，降级返回 HIGH
return misty.audio.successResult(
  songId,
  "LOSSLESS",    // 请求的音质
  "HIGH",        // 实际返回的音质（不同，已降级）
  "https://.../320k.mp3",
  { format: "mp3", bitrateKbps: 320 }
);
```

#### 示例：获取失败

```javascript
// 请求 HI_RES，但该歌曲没有任何可用资源
return misty.audio.errorResult(
  songId,
  "HI_RES",
  "No audio resource available for this song"
);
```

---

## 加解密工具（`misty.crypto`）

`misty.crypto` 为插件提供常见的 Hash 和 AES 加解密能力，方便对接各个平台的签名与数据保护逻辑。

### Hash 系列

```javascript
// 文本 Hash（返回 hex 字符串）
const md5    = misty.crypto.md5("text");
const sha1   = misty.crypto.sha1("text");
const sha256 = misty.crypto.sha256("text");

// HMAC-SHA256（data 和 key 均为字符串，返回 hex）
const sign = misty.crypto.hmacSha256("data", "secretKey");
```

### AES-CBC（Base64 编码）

底层使用：`AES/CBC/PKCS5Padding`。

```javascript
// 加密：明文 -> Base64
const cipherBase64 = misty.crypto.aesEncryptToBase64(
  "plain text",
  "my-secret-key",   // key：UTF-8，会规范化到 16 字节（不足补 0，超出截断）
  "my-iv-string"     // iv：UTF-8，16 字节；可为 null/undefined 表示全 0 IV
);

// 解密：Base64 -> 明文
const plain = misty.crypto.aesDecryptFromBase64(
  cipherBase64,
  "my-secret-key",
  "my-iv-string"
);
```

**注意：**

- AES-CBC 适合大部分常见 Web/APP 协议的对称加解密场景；
- 请确保后端/目标平台的 AES 模式和填充方式与此一致（`CBC` + `PKCS5Padding`）。

### AES-ECB（Base64 编码）

底层使用：`AES/ECB/PKCS5Padding`，**无 IV，仅使用 key**。

```javascript
// 加密：明文 -> Base64
const cipherEcb = misty.crypto.aesEcbEncryptToBase64(
  "plain text",
  "my-secret-key"  // key：UTF-8，会规范化到 16 字节
);

// 解密：Base64 -> 明文
const plainEcb = misty.crypto.aesEcbDecryptFromBase64(
  cipherEcb,
  "my-secret-key"
);
```

**注意：**

- 同样需要确保与对端使用的模式/填充完全一致（`ECB` + `PKCS5Padding`）。

---

## 数据模型与返回格式

以下是插件需要返回的核心数据结构。插件无需完全包含所有字段，但字段名和类型需兼容。

### 插件元信息 `MistyPluginMeta`

每个插件必须在 `meta` 字段中提供基础信息：

```javascript
{
  id: "example",                     // 必须：插件 ID，与 MistyPlugins 的 key 一致
  name: "Example Source",            // 必须：插件名称（展示用）
  author: "Your Name",               // 可选：作者
  version: "1.0.0",                  // 可选：版本号
  description: "A music source plugin",  // 可选：简要描述
  homepage: "https://github.com/...",    // 可选：项目主页或仓库
  license: "MIT",                    // 可选：许可证

  // 与音源平台相关的信息（可选）
  sourceName: "Example Music",       // 音源平台名称
  sourceHomepage: "https://...",     // 平台官网

  // 声明插件支持的能力
  capabilities: [
    "SEARCH",           // 搜索歌曲
    "PLAYLIST",         // 获取歌单
    "ALBUM",            // 获取专辑
    "LYRICS",           // 获取歌词
    "AUDIO_RESOURCES",  // 获取音频资源
  ],

  // 支持的地区/区域标识（可选）
  supportRegions: ["CN", "US"],

  // 免责声明（可选）
  disclaimer: "This plugin is provided by the community...",
}
```

**能力枚举 `capabilities`：**

| 枚举值 | 说明 |
|--------|------|
| `SEARCH` | 搜索歌曲 |
| `PLAYLIST` | 获取歌单 |
| `ALBUM` | 获取专辑 |
| `LYRICS` | 获取歌词 |
| `AUDIO_RESOURCES` | 获取音频资源 |

### 艺术家 `MistyArtist`

```javascript
{
  id: "123",               // 可选：艺术家 ID（可为 null）
  source: "netease",       // 必须：来源标识
  name: "Artist Name",     // 必须：艺术家名称
  coverUrl: "https://...", // 可选：头像 URL（可为 null）
}
```

### 歌曲 `MistySong`

```javascript
{
  id: "123456",              // 必须：歌曲 ID
  source: "netease",         // 必须：来源标识（通常为 PLUGIN_ID）
  name: "Song Name",         // 必须：歌曲名称
  artists: [                 // 必须：艺术家列表（MistyArtist[]）
    { id: "1", source: "netease", name: "Artist", coverUrl: null }
  ],
  album: {                   // 可选：专辑信息（MistyAlbum）
    id: "10", source: "netease", name: "Album Name"
  },
  coverUrl: "https://...",   // 可选：封面 URL
  extras: {},                // 可选：额外信息
}
```

### 歌单 `MistyPlaylist`

```javascript
{
  id: "playlist123",         // 必须：歌单 ID
  source: "netease",         // 必须：来源标识
  name: "My Playlist",       // 必须：歌单名称
  creator: "User",           // 可选：创建者名字
  coverUrl: "https://...",   // 可选：封面
  description: "...",        // 可选：描述
  songCount: 100,            // 可选：歌曲数量
  playCount: 10000,          // 可选：播放量
  updateTime: "2024-01-01",  // 可选：最近更新时间
  songs: [ /* MistySong[] */ ],  // 可选：歌曲列表
  extras: {},                // 可选：额外信息
}
```

### 专辑 `MistyAlbum`

```javascript
{
  id: "album123",            // 必须：专辑 ID
  source: "netease",         // 必须：来源标识
  name: "Album Name",        // 必须：专辑名称
  artists: [ /* MistyArtist[] */ ],  // 可选：专辑艺术家（可能有多个）
  coverUrl: "https://...",   // 可选：封面
  trackCount: 10,            // 可选：歌曲总数
  releaseDate: "2024-01-01", // 可选：发行日期
  description: "...",        // 可选：专辑介绍
  songs: [ /* MistySong[] */ ],  // 可选：专辑内歌曲列表（按需加载）
  extras: {},                // 可选：额外信息
}
```

### 歌词 `MistyLyricBundle`

```javascript
{
  songId: "123456",          // 必须：歌曲 ID
  lyrics: [                  // 必须：歌词列表（MistyLyric[]）
    {
      content: "[00:00.00]...",   // 歌词文本内容
      type: "ORIGINAL",          // 类型
      format: "LINE_BY_LINE",    // 格式
    },
    {
      content: "[00:00.00]...",
      type: "TRANSLATION",
      format: "LINE_BY_LINE",
    },
  ],
}
```

**枚举值说明：**

| 字段 | 枚举值 | 说明 |
|------|--------|------|
| `type` | `ORIGINAL` | 原文 |
| `type` | `TRANSLATION` | 译文 |
| `type` | `ROMANIZATION` | 罗马音 |
| `format` | `LINE_BY_LINE` | 逐行 |
| `format` | `WORD_BY_WORD` | 逐字 |

### 音频资源 `MistyAudioResourceResult`

`getAudioResource(songId, quality)` 接口返回此结构：

```javascript
{
  songId: "123456",                  // 歌曲 ID
  requestedQuality: "LOSSLESS",      // 请求的音质
  resource: {                        // 实际返回的资源（可能为 null）
    quality: "HIGH",                 // 实际音质（可能与请求不同，表示降级）
    url: "https://...",
    format: "mp3",                   // 可选：格式（如 "mp3", "flac"）
    bitrateKbps: 320,                // 可选：比特率（kbps）
    fileSizeBytes: 10485760,         // 可选：文件大小（字节）
    md5: "abc123...",                // 可选：校验和
    extras: {},                      // 可选：额外信息
  },
  error: null,                       // 错误信息（如果失败）
}
```

**判断状态：**

| 条件 | 含义 |
|------|------|
| `resource != null && resource.quality == requestedQuality` | ✅ 成功获取请求音质 |
| `resource != null && resource.quality != requestedQuality` | ⚠️ 降级（返回了其他音质） |
| `resource == null` | ❌ 失败，查看 `error` 获取错误信息 |

---

## 插件需要实现的接口

### `search(keyword, page)` 搜索歌曲

**JS 签名：**

```javascript
function search(keyword, page) => MistySong[] | { songs: MistySong[], ... }
```

**参数：**
- `keyword`：搜索关键词
- `page`：页码（从 1 开始）

**返回：** `MistySong[]` 或 `{ songs: MistySong[], ... }`

---

### `getPlaylist(playlistId)` 获取歌单

**JS 签名：**

```javascript
function getPlaylist(playlistId) => MistyPlaylist | { playlist: MistyPlaylist, ... }
```

**参数：**
- `playlistId`：歌单 ID

**返回：** `MistyPlaylist` 对象

---

### `getAlbum(albumId)` 获取专辑

**JS 签名：**

```javascript
function getAlbum(albumId) => MistyAlbum | { album: MistyAlbum, ... }
```

**参数：**
- `albumId`：专辑 ID

**返回：** `MistyAlbum` 对象

---

### `getLyrics(songId)` 获取歌词

**JS 签名：**

```javascript
function getLyrics(songId) => MistyLyricBundle | { bundle: MistyLyricBundle, ... }
```

**参数：**
- `songId`：歌曲 ID

**返回：** `MistyLyricBundle` 对象

---

### `getAudioResource(songId, quality)` 获取指定音质音频资源

**JS 签名（插件必须实现）：**

```javascript
function getAudioResource(songId, quality) => MistyAudioResourceResult
```

**参数：**

- `songId`：歌曲 ID
- `quality`：请求的音质（`"STANDARD"`, `"HIGH"`, `"LOSSLESS"`, `"HI_RES"`）

**返回：** `MistyAudioResourceResult`

**推荐实现：**

```javascript
getAudioResource: function(songId, quality) {
  misty.log.info("[" + PLUGIN_ID + "] getAudioResource: songId=" + songId + ", quality=" + quality);

  try {
    // 1. 请求后端 API，传入期望音质（misty.http 会同步阻塞等待响应）
    var resp = misty.http.get(
      "https://api.example.com/audio/" + encodeURIComponent(songId) + "?quality=" + quality
    );
    var data = JSON.parse(resp);

    // 2. 检查是否有可用资源
    if (!data.url) {
      return misty.audio.errorResult(songId, quality, "No audio resource available");
    }

    // 3. 返回成功结果
    // 注意：data.actualQuality 为后端实际返回的音质，可能与请求不同（降级）
    return misty.audio.successResult(
      songId,
      quality,                        // 请求的音质
      data.actualQuality || quality,  // 实际返回的音质
      data.url,
      {
        format: data.format,
        bitrateKbps: data.bitrate,
        fileSizeBytes: data.fileSize,
        md5: data.md5,
      }
    );
  } catch (err) {
    misty.log.error("[" + PLUGIN_ID + "] getAudioResource failed: " + err.message);
    return misty.audio.errorResult(songId, quality, err.message);
  }
}
```

---

## 错误处理与兼容性

- 所有接口内部建议使用 `try/catch` 包裹网络调用，并在错误时：
  - 使用 `misty.log.error(...)` 打日志。
  - 抛出 JS `Error`，宿主会捕获并包装为错误信息。
- 返回结构中**不要**自行添加 `error` 字段（那是宿主侧协议），插件只需按数据模型返回正常结果或抛异常。
- 枚举类（音质、歌词类型等）必须使用**大写字符串**与 Kotlin 端枚举名称一致。

---

## 完整示例模板

下面是一个**最小但完整**的插件模板，你可以复制后改名与适配逻辑：

```javascript
if (typeof MistyPlugins === "undefined") {
  MistyPlugins = {};
}

const PLUGIN_ID = "example";

MistyPlugins[PLUGIN_ID] = {
  // 必选：插件元信息
  meta: {
    id: PLUGIN_ID,
    name: "Example Source",
    author: "Your Name",
    version: "1.0.0",
    description: "Example music source plugin",
    homepage: "https://github.com/your/repo",
    license: "MIT",
    sourceName: "Example Music",
    sourceHomepage: "https://music.example.com",
    capabilities: ["SEARCH", "PLAYLIST", "ALBUM", "LYRICS", "AUDIO_RESOURCES"],
    supportRegions: ["CN"],
    disclaimer: "This plugin is provided by the community. Misty only loads community scripts and does not provide any music sources.",
  },

  // 必选：搜索歌曲
  search: function(keyword, page) {
    misty.log.info("[" + PLUGIN_ID + "] search: keyword=" + keyword + ", page=" + page);
    var resp = misty.http.get(
      "https://api.example.com/search?q=" + encodeURIComponent(keyword) + "&page=" + page
    );
    var data = JSON.parse(resp);
    // 假设 data.songs 就是符合 MistySong 结构的数组
    return data.songs || [];
  },

  // 获取歌单
  getPlaylist: function(playlistId) {
    misty.log.info("[" + PLUGIN_ID + "] getPlaylist: id=" + playlistId);
    var resp = misty.http.get(
      "https://api.example.com/playlist/" + encodeURIComponent(playlistId)
    );
    var data = JSON.parse(resp);
    // 直接返回一个 MistyPlaylist 对象
    return {
      id: data.id,
      source: PLUGIN_ID,
      name: data.name,
      creator: data.creator,
      coverUrl: data.cover,
      description: data.desc,
      songCount: data.songCount,
      playCount: data.playCount,
      updateTime: data.updateTime,
      songs: data.songs, // 需符合 MistySong 结构
      extras: {},
    };
  },

  // 获取专辑
  getAlbum: function(albumId) {
    misty.log.info("[" + PLUGIN_ID + "] getAlbum: id=" + albumId);
    var resp = misty.http.get(
      "https://api.example.com/album/" + encodeURIComponent(albumId)
    );
    var data = JSON.parse(resp);
    return {
      id: data.id,
      source: PLUGIN_ID,
      name: data.name,
      artists: data.artists,
      coverUrl: data.cover,
      trackCount: data.trackCount,
      releaseDate: data.releaseDate,
      description: data.description,
      songs: data.songs,
      extras: {},
    };
  },

  // 获取歌词
  getLyrics: function(songId) {
    misty.log.info("[" + PLUGIN_ID + "] getLyrics: songId=" + songId);
    var resp = misty.http.get(
      "https://api.example.com/lyrics/" + encodeURIComponent(songId)
    );
    var data = JSON.parse(resp);
    // 返回 MistyLyricBundle
    var lyrics = [];
    if (data.original) {
      lyrics.push({
        content: data.original,
        type: "ORIGINAL",
        format: "LINE_BY_LINE",
      });
    }
    if (data.translation) {
      lyrics.push({
        content: data.translation,
        type: "TRANSLATION",
        format: "LINE_BY_LINE",
      });
    }
    return {
      songId: songId,
      lyrics: lyrics,
    };
  },

  // 获取指定音质音频资源
  getAudioResource: function(songId, quality) {
    misty.log.info("[" + PLUGIN_ID + "] getAudioResource: songId=" + songId + ", quality=" + quality);

    try {
      var resp = misty.http.get(
        "https://api.example.com/audio/" + encodeURIComponent(songId) + "?quality=" + quality
      );
      var data = JSON.parse(resp);

      if (!data.url) {
        return misty.audio.errorResult(songId, quality, "No audio resource available");
      }

      // 后端可能返回不同音质（降级）
      var actualQuality = data.actualQuality || quality;

      return misty.audio.successResult(
        songId,
        quality,          // 请求的音质
        actualQuality,    // 实际返回的音质
        data.url,
        {
          format: data.format,
          bitrateKbps: data.bitrate,
          fileSizeBytes: data.fileSize,
        }
      );
    } catch (err) {
      misty.log.error("[" + PLUGIN_ID + "] getAudioResource failed: " + err.message);
      return misty.audio.errorResult(songId, quality, err.message);
    }
  },
};
```

---

如有接口变更或新增（例如排行榜、每日推荐等），会在后续版本中扩充本规范，并为不同平台（如网易云、QQ 音乐等）提供示例插件。请持续关注文档更新。
