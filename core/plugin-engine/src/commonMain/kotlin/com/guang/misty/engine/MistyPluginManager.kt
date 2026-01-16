package com.guang.misty.engine

import com.guang.misty.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * Misty 插件管理器
 * 负责管理插件的加载、执行和结果解析
 */
class MistyPluginManager(
    private val jsEngine: MistyJsEngine
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /**
     * 列出当前已加载的插件 ID 列表（即 MistyPlugins 的 key）。
     */
    suspend fun listPluginIds(): List<String> {
        return try {
            val script = """
                (function() {
                    if (typeof MistyPlugins === 'undefined' || !MistyPlugins) {
                        return JSON.stringify([]);
                    }
                    return JSON.stringify(Object.keys(MistyPlugins));
                })();
            """.trimIndent()

            val jsonArray = jsEngine.executeScript(script)
            json.decodeFromString(jsonArray)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * 获取指定插件的元信息（基础信息、能力声明等）。
     *
     * 插件侧约定：在注册对象中提供 meta 字段：
     *
     * MistyPlugins["example"] = {
     *   meta: {
     *     id: "example",
     *     name: "Example Source",
     *     author: "someone",
     *     version: "1.0.0",
     *     description: "...",
     *     homepage: "https://...",
     *     license: "MIT",
     *     sourceName: "Example Music",
     *     sourceHomepage: "https://music.example.com",
     *     capabilities: ["SEARCH", "PLAYLIST", "ALBUM", "LYRICS", "AUDIO_RESOURCES"],
     *     supportRegions: ["CN", "US"],
     *     disclaimer: "This plugin is provided by the community, not by Misty."
     *   },
     *   // 其它函数：search/getPlaylist/getAlbum/getLyrics/getAudioResources...
     * };
     *
     * 如果 meta 不存在或解析失败，会抛出异常，调用方可据此做降级处理。
     */
    suspend fun getPluginMeta(pluginId: String): MistyPluginMeta {
        val script = """
            (function() {
                if (typeof MistyPlugins === 'undefined' || !MistyPlugins || !MistyPlugins['$pluginId']) {
                    throw new Error('Plugin not found: $pluginId');
                }
                const plugin = MistyPlugins['$pluginId'];
                const meta = plugin.meta || {};
                // 自动填充 id 字段，便于 Kotlin 端使用
                if (!meta.id) meta.id = '$pluginId';
                return JSON.stringify(meta);
            })();
        """.trimIndent()

        return try {
            val resultJson = jsEngine.executeScript(script)
            json.decodeFromString(resultJson)
        } catch (e: Exception) {
            jsEngine.log("ERROR", "getPluginMeta failed for $pluginId: ${e.message}")
            throw e
        }
    }

    /**
     * 搜索歌曲
     * @param pluginId 插件 ID
     * @param keyword 搜索关键词
     * @param page 页码（从 1 开始）
     * @return 歌曲列表
     */
    suspend fun search(pluginId: String, keyword: String, page: Int): List<MistySong> {
        try {
            // 使用同步 IIFE 调用插件函数（插件应使用同步函数）
            val script = """
                (function() {
                    if (!MistyPlugins || !MistyPlugins['$pluginId']) {
                        throw new Error('Plugin not found: $pluginId');
                    }
                    const plugin = MistyPlugins['$pluginId'];
                    if (typeof plugin.search !== 'function') {
                        throw new Error('Plugin $pluginId does not have a search function');
                    }
                    const result = plugin.search('$keyword', $page);
                    return JSON.stringify(result);
                })();
            """.trimIndent()

            val resultJson = jsEngine.executeScript(script)

            // 解析 JSON 结果
            // 假设插件返回的格式为 { songs: [...] } 或直接是数组
            val result = try {
                // 尝试解析为对象
                val jsonElement = json.parseToJsonElement(resultJson)
                if (jsonElement.jsonObject.containsKey("songs")) {
                    // 如果包含 songs 字段
                    json.decodeFromString<List<MistySong>>(
                        jsonElement.jsonObject["songs"]!!.toString()
                    )
                } else {
                    // 尝试直接解析为数组
                    json.decodeFromString(resultJson)
                }
            } catch (e: Exception) {
                // 如果解析失败，尝试直接解析整个 JSON
                json.decodeFromString(resultJson)
            }

            return result
        } catch (e: Exception) {
            jsEngine.log("ERROR", "Search failed: ${e.message}")
            throw e
        }
    }

    /**
     * 获取搜索联想词
     * 
     * 插件侧约定：实现 plugin.getSearchSuggestions(keyword)，返回字符串数组：
     * - 返回格式：["联想词1", "联想词2", ...]
     * 
     * @param pluginId 插件 ID
     * @param keyword 当前输入的关键词
     * @return 联想词列表
     */
    suspend fun getSearchSuggestions(pluginId: String, keyword: String): List<String> {
        try {
            val script = """
                (function() {
                    if (!MistyPlugins || !MistyPlugins['$pluginId']) {
                        throw new Error('Plugin not found: $pluginId');
                    }
                    const plugin = MistyPlugins['$pluginId'];
                    if (typeof plugin.getSearchSuggestions !== 'function') {
                        return JSON.stringify([]);
                    }
                    const result = plugin.getSearchSuggestions('$keyword');
                    return JSON.stringify(result || []);
                })();
            """.trimIndent()

            val resultJson = jsEngine.executeScript(script)
            return json.decodeFromString(resultJson)
        } catch (e: Exception) {
            jsEngine.log("WARN", "getSearchSuggestions failed: ${e.message}")
            return emptyList()
        }
    }

    /**
     * 获取指定音质的音频资源。
     *
     * 插件侧约定：实现 plugin.getAudioResource(songId, quality)，返回 MistyAudioResourceResult：
     * - songId: 歌曲 ID
     * - requestedQuality: 请求的音质
     * - resource: 实际返回的资源（可能为 null 表示失败）
     *   - resource.quality: 实际返回的音质（可能与请求不同，表示降级）
     * - error: 错误信息（如果失败）
     *
     * @param pluginId 插件 ID
     * @param songId 歌曲 ID
     * @param quality 请求的音质
     * @return 音频资源请求结果
     */
    suspend fun getAudioResource(
        pluginId: String,
        songId: String,
        quality: MistyAudioQuality
    ): MistyAudioResourceResult {
        try {
            val qualityStr = json.encodeToString(quality).trim('"')
            val script = """
                (function() {
                    if (!MistyPlugins || !MistyPlugins['$pluginId']) {
                        throw new Error('Plugin not found: $pluginId');
                    }
                    const plugin = MistyPlugins['$pluginId'];
                    if (typeof plugin.getAudioResource !== 'function') {
                        throw new Error('Plugin $pluginId does not have a getAudioResource function');
                    }
                    const result = plugin.getAudioResource('$songId', '$qualityStr');
                    return JSON.stringify(result);
                })();
            """.trimIndent()

            val resultJson = jsEngine.executeScript(script)
            return json.decodeFromString(resultJson)
        } catch (e: Exception) {
            jsEngine.log("ERROR", "getAudioResource failed: ${e.message}")
            // 返回错误结果而非抛出异常
            return MistyAudioResourceResult(
                songId = songId,
                requestedQuality = quality,
                resource = null,
                error = e.message ?: "Unknown error"
            )
        }
    }

    /**
     * 获取歌单详情。
     *
     * 插件侧约定：实现 plugin.getPlaylist(playlistId)，返回：
     * - 直接为 MistyPlaylist 对象，或
     * - 对象 { playlist: { ... }, ... }。
     */
    suspend fun getPlaylist(pluginId: String, playlistId: String): MistyPlaylist {
        try {
            val script = """
                (function() {
                    if (!MistyPlugins || !MistyPlugins['$pluginId']) {
                        throw new Error('Plugin not found: $pluginId');
                    }
                    const plugin = MistyPlugins['$pluginId'];
                    if (typeof plugin.getPlaylist !== 'function') {
                        throw new Error('Plugin $pluginId does not have a getPlaylist function');
                    }
                    const result = plugin.getPlaylist('$playlistId');
                    return JSON.stringify(result);
                })();
            """.trimIndent()

            val resultJson = jsEngine.executeScript(script)

            val playlist: MistyPlaylist = try {
                val jsonElement = json.parseToJsonElement(resultJson)
                if (jsonElement.jsonObject.containsKey("playlist")) {
                    json.decodeFromString(
                        jsonElement.jsonObject["playlist"].toString()
                    )
                } else {
                    json.decodeFromString(resultJson)
                }
            } catch (e: Exception) {
                json.decodeFromString(resultJson)
            }

            return playlist
        } catch (e: Exception) {
            jsEngine.log("ERROR", "getPlaylist failed: ${e.message}")
            throw e
        }
    }

    /**
     * 获取专辑详情。
     *
     * 插件侧约定：实现 plugin.getAlbum(albumId)，返回：
     * - 直接为 MistyAlbum 对象，或
     * - 对象 { album: { ... }, ... }。
     */
    suspend fun getAlbum(pluginId: String, albumId: String): MistyAlbum {
        try {
            val script = """
                (function() {
                    if (!MistyPlugins || !MistyPlugins['$pluginId']) {
                        throw new Error('Plugin not found: $pluginId');
                    }
                    const plugin = MistyPlugins['$pluginId'];
                    if (typeof plugin.getAlbum !== 'function') {
                        throw new Error('Plugin $pluginId does not have a getAlbum function');
                    }
                    const result = plugin.getAlbum('$albumId');
                    return JSON.stringify(result);
                })();
            """.trimIndent()

            val resultJson = jsEngine.executeScript(script)

            val album: MistyAlbum = try {
                val jsonElement = json.parseToJsonElement(resultJson)
                if (jsonElement.jsonObject.containsKey("album")) {
                    json.decodeFromString(
                        jsonElement.jsonObject["album"].toString()
                    )
                } else {
                    json.decodeFromString(resultJson)
                }
            } catch (e: Exception) {
                json.decodeFromString(resultJson)
            }

            return album
        } catch (e: Exception) {
            jsEngine.log("ERROR", "getAlbum failed: ${e.message}")
            throw e
        }
    }

    /**
     * 获取歌词（包含多种类型与格式）。
     *
     * 插件侧约定：实现 plugin.getLyrics(songId)，返回：
     * - 直接为 MistyLyricBundle 对象，或
     * - 对象 { bundle: { ... }, ... } 或 { lyricBundle: { ... }, ... }。
     *
     * 注意：JS 端须保证：
     * - type 对应 Kotlin 枚举 MistyLyricType 的名称（如 "ORIGINAL", "TRANSLATION"）
     * - format 对应 Kotlin 枚举 MistyLyricFormat 的名称（如 "LINE_BY_LINE"）
     */
    suspend fun getLyrics(pluginId: String, songId: String): MistyLyricBundle {
        try {
            val script = """
                (function() {
                    if (!MistyPlugins || !MistyPlugins['$pluginId']) {
                        throw new Error('Plugin not found: $pluginId');
                    }
                    const plugin = MistyPlugins['$pluginId'];
                    if (typeof plugin.getLyrics !== 'function') {
                        throw new Error('Plugin $pluginId does not have a getLyrics function');
                    }
                    const result = plugin.getLyrics('$songId');
                    return JSON.stringify(result);
                })();
            """.trimIndent()

            val resultJson = jsEngine.executeScript(script)

            val bundle: MistyLyricBundle = try {
                val jsonElement = json.parseToJsonElement(resultJson)
                when {
                    jsonElement.jsonObject.containsKey("bundle") ->
                        json.decodeFromString(jsonElement.jsonObject["bundle"].toString())

                    jsonElement.jsonObject.containsKey("lyricBundle") ->
                        json.decodeFromString(jsonElement.jsonObject["lyricBundle"].toString())

                    else ->
                        json.decodeFromString(resultJson)
                }
            } catch (e: Exception) {
                json.decodeFromString(resultJson)
            }

            return bundle
        } catch (e: Exception) {
            jsEngine.log("ERROR", "getLyrics failed: ${e.message}")
            throw e
        }
    }

    /**
     * 加载插件
     * @param pluginId 插件 ID
     * @param pluginCode JavaScript 插件代码
     */
    suspend fun loadPlugin(pluginId: String, pluginCode: String) {
        try {
            val script = """
                (function() {
                    if (!MistyPlugins) {
                        MistyPlugins = {};
                    }
                    $pluginCode
                    return undefined;
                })();
            """.trimIndent()

            jsEngine.executeScript(script)
        } catch (e: Exception) {
            jsEngine.log("ERROR", "Failed to load plugin $pluginId: ${e.message}")
            throw e
        }
    }

    /**
     * 检查插件是否存在
     * @param pluginId 插件 ID
     * @return 是否存在
     */
    suspend fun hasPlugin(pluginId: String): Boolean {
        return try {
            val script = """
                (function() {
                    return String(MistyPlugins && MistyPlugins['$pluginId'] !== undefined);
                })();
            """.trimIndent()

            val result = jsEngine.executeScript(script)
            result == "true"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 卸载指定插件
     * @param pluginId 插件 ID
     */
    suspend fun unloadPlugin(pluginId: String) {
        try {
            val script = """
                (function() {
                    if (MistyPlugins && MistyPlugins['$pluginId']) {
                        delete MistyPlugins['$pluginId'];
                    }
                    return undefined;
                })();
            """.trimIndent()

            jsEngine.executeScript(script)
        } catch (e: Exception) {
            jsEngine.log("ERROR", "Failed to unload plugin $pluginId: ${e.message}")
        }
    }

    /**
     * 清除所有已加载的插件
     */
    suspend fun clearAllPlugins() {
        try {
            val script = """
                (function() {
                    if (typeof MistyPlugins !== 'undefined') {
                        for (var key in MistyPlugins) {
                            if (MistyPlugins.hasOwnProperty(key)) {
                                delete MistyPlugins[key];
                            }
                        }
                    }
                    return undefined;
                })();
            """.trimIndent()

            jsEngine.executeScript(script)
        } catch (e: Exception) {
            jsEngine.log("ERROR", "Failed to clear all plugins: ${e.message}")
        }
    }
}
