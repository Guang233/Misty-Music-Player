// Misty 插件系统 JavaScript SDK
// 此文件在 JS 引擎初始化时加载，为插件提供 API

// 定义全局插件命名空间
if (typeof MistyPlugins === 'undefined') {
    MistyPlugins = {};
}

// 定义 misty SDK 根对象
if (typeof misty === 'undefined') {
    misty = {};
}

// HTTP 客户端 API
misty.http = {
    /**
     * 通用请求方法（最灵活）
     * @param {Object} options - 请求选项
     * @param {string} options.url - 请求 URL（必需）
     * @param {string} options.method - 请求方法，默认为 "GET"（"GET" 或 "POST"）
     * @param {Object} options.headers - 请求头（可选）
     * @param {string} options.body - 请求体（可选）
     * @param {string} options.bodyEncoding - 请求体编码方式，默认为 "string"（"string", "hex", "base64"）
     * @param {string} options.responseEncoding - 响应体编码方式，默认为 "string"（"string", "hex", "base64"）
     * @returns {Promise<Object>} 响应对象 { statusCode, headers, body, error? }
     */
    request: async function(options) {
        const request = {
            url: options.url,
            method: options.method || "GET",
            headers: options.headers || {},
            body: options.body || null,
            bodyEncoding: options.bodyEncoding || "string",
            responseEncoding: options.responseEncoding || "string"
        };
        const responseJson = await mistyInternal.performRequest(JSON.stringify(request));
        const response = JSON.parse(responseJson);
        if (response.error) {
            throw new Error(response.error);
        }
        return response;
    },

    /**
     * GET 请求（便捷方法）
     * @param {string} url - 请求 URL
     * @param {Object} headers - 请求头（可选）
     * @param {string} responseEncoding - 响应编码方式，默认为 "string"（"string", "hex", "base64"）
     * @returns {Promise<string>} 响应体（根据 responseEncoding 解码后的字符串）
     */
    get: async function(url, headers = {}, responseEncoding = "string") {
        const response = await misty.http.request({
            url: url,
            method: "GET",
            headers: headers,
            responseEncoding: responseEncoding
        });
        return response.body;
    },

    /**
     * POST 请求（便捷方法）
     * @param {string} url - 请求 URL
     * @param {string} body - 请求体
     * @param {Object} headers - 请求头（可选）
     * @param {string} bodyEncoding - 请求体编码方式，默认为 "string"（"string", "hex", "base64"）
     * @param {string} responseEncoding - 响应编码方式，默认为 "string"（"string", "hex", "base64"）
     * @returns {Promise<string>} 响应体（根据 responseEncoding 解码后的字符串）
     */
    post: async function(url, body, headers = {}, bodyEncoding = "string", responseEncoding = "string") {
        const response = await misty.http.request({
            url: url,
            method: "POST",
            headers: headers,
            body: body,
            bodyEncoding: bodyEncoding,
            responseEncoding: responseEncoding
        });
        return response.body;
    },

    /**
     * 获取二进制数据（返回 base64 编码，向后兼容）
     * @param {string} url - 请求 URL
     * @param {Object} headers - 请求头（可选）
     * @returns {Promise<string>} base64 编码的响应
     */
    getBinary: async function(url, headers = {}) {
        return await misty.http.get(url, headers, "base64");
    },

    /**
     * 获取二进制数据（返回 hex 编码）
     * @param {string} url - 请求 URL
     * @param {Object} headers - 请求头（可选）
     * @returns {Promise<string>} hex 编码的响应
     */
    getBinaryHex: async function(url, headers = {}) {
        return await misty.http.get(url, headers, "hex");
    }
};

// 日志 API
misty.log = {
    info: function(msg) {
        mistyInternal.log("INFO", msg);
    },
    warn: function(msg) {
        mistyInternal.log("WARN", msg);
    },
    error: function(msg) {
        mistyInternal.log("ERROR", msg);
    },
    debug: function(msg) {
        mistyInternal.log("DEBUG", msg);
    }
};

// 音频资源辅助 API（按指定音质请求）
misty.audio = {
    // 与 Kotlin 端 MistyAudioQuality 枚举保持一致
    Quality: {
        STANDARD: "STANDARD",   // 标准音质
        HIGH: "HIGH",           // 高音质
        LOSSLESS: "LOSSLESS",   // 无损音质
        HI_RES: "HI_RES",       // Hi-Res 高解析度
    },

    /**
     * 创建单个音频资源对象（结构与 MistyAudioResource 对应）
     * @param {Object} options
     * @param {string} options.quality - 实际音质（可能与请求不同，表示降级）
     * @param {string} options.url - 音频 URL
     * @param {string} options.format - 格式（如 "mp3", "flac"）
     * @param {number} options.bitrateKbps - 比特率（kbps）
     * @param {number} options.fileSizeBytes - 文件大小（字节）
     * @param {string} options.md5 - 校验和
     * @param {Object} options.extras - 额外信息
     */
    createResource: function (options) {
        return {
            quality: options.quality,
            url: options.url,
            format: options.format || null,
            bitrateKbps: options.bitrateKbps || null,
            fileSizeBytes: options.fileSizeBytes || null,
            md5: options.md5 || null,
            extras: options.extras || {},
        };
    },

    /**
     * 创建音频资源请求结果（与 MistyAudioResourceResult 对应）
     *
     * 用于处理「按指定音质请求」的场景：
     * - 成功：resource 不为 null，resource.quality 表示实际音质
     * - 降级：resource.quality != requestedQuality
     * - 失败：resource 为 null，error 包含错误信息
     *
     * @param {string} songId - 歌曲 ID
     * @param {string} requestedQuality - 请求的音质（如 "LOSSLESS"）
     * @param {Object|null} resource - 实际返回的资源（可能为 null）
     * @param {string|null} error - 错误信息（可选）
     */
    createResult: function (songId, requestedQuality, resource, error) {
        return {
            songId: songId,
            requestedQuality: requestedQuality,
            resource: resource || null,
            error: error || null,
        };
    },

    /**
     * 快速创建成功结果
     * @param {string} songId
     * @param {string} requestedQuality - 请求的音质
     * @param {string} actualQuality - 实际返回的音质（可能与请求不同，表示降级）
     * @param {string} url - 音频 URL
     * @param {Object} opts - 其它可选字段（format, bitrateKbps, fileSizeBytes, md5, extras）
     */
    successResult: function (songId, requestedQuality, actualQuality, url, opts = {}) {
        const resource = misty.audio.createResource({
            quality: actualQuality,
            url: url,
            format: opts.format || null,
            bitrateKbps: opts.bitrateKbps || null,
            fileSizeBytes: opts.fileSizeBytes || null,
            md5: opts.md5 || null,
            extras: opts.extras || {},
        });
        return misty.audio.createResult(songId, requestedQuality, resource, null);
    },

    /**
     * 快速创建失败结果
     * @param {string} songId
     * @param {string} requestedQuality - 请求的音质
     * @param {string} error - 错误信息
     */
    errorResult: function (songId, requestedQuality, error) {
        return misty.audio.createResult(songId, requestedQuality, null, error);
    },
};

// 加解密辅助 API
misty.crypto = {
    /**
     * 文本 Hash
     */
    md5: function (text) {
        return mistyInternal.md5(String(text));
    },
    sha1: function (text) {
        return mistyInternal.sha1(String(text));
    },
    sha256: function (text) {
        return mistyInternal.sha256(String(text));
    },
    hmacSha256: function (data, key) {
        return mistyInternal.hmacSha256(String(data), String(key));
    },

    /**
     * AES-CBC/PKCS5Padding，加解密（Base64 编码）
     *
     * 注意：
     * - key / iv 为 UTF-8 字符串，底层会规范化到 16 字节（不足补 0，超出截断）。
     * - 如果 iv 为空，则使用全 0 IV。
     */
    aesEncryptToBase64: function (plainText, key, iv) {
        return mistyInternal.aesEncryptToBase64(String(plainText), String(key), iv != null ? String(iv) : null);
    },
    aesDecryptFromBase64: function (cipherBase64, key, iv) {
        return mistyInternal.aesDecryptFromBase64(String(cipherBase64), String(key), iv != null ? String(iv) : null);
    },

    /**
     * AES-ECB/PKCS5Padding，加解密（Base64 编码）
     *
     * 注意：
     * - 无 IV，仅使用 key；key 为 UTF-8 字符串，底层会规范化到 16 字节。
     */
    aesEcbEncryptToBase64: function (plainText, key) {
        return mistyInternal.aesEcbEncryptToBase64(String(plainText), String(key));
    },
    aesEcbDecryptFromBase64: function (cipherBase64, key) {
        return mistyInternal.aesEcbDecryptFromBase64(String(cipherBase64), String(key));
    },
};

// 返回 undefined，避免脚本返回对象
undefined;
