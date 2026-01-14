// Misty 插件系统 JavaScript SDK
// 此文件在 JS 引擎初始化时加载，为插件提供 API

// 定义全局插件命名空间
if (typeof MistyPlugins === 'undefined') {
    MistyPlugins = {};
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
