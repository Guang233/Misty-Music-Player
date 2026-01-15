package com.guang.misty.engine

import com.guang.misty.model.*
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
            jsEngine.executeScript(
                """
                mistyInternal.log('ERROR', 'getPluginMeta failed for $pluginId: ${e.message}');
                """.trimIndent()
            )
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
            val script = """
                (async function() {
                    if (!MistyPlugins || !MistyPlugins['$pluginId']) {
                        throw new Error('Plugin not found: $pluginId');
                    }
                    const plugin = MistyPlugins['$pluginId'];
                    if (typeof plugin.search !== 'function') {
                        throw new Error('Plugin $pluginId does not have a search function');
                    }
                    const result = await plugin.search('$keyword', $page);
                    return JSON.stringify(result);
                })();
            """.trimIndent()

            val resultJson = jsEngine.executeAsyncScript(script)

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
            jsEngine.executeScript("""
                mistyInternal.log('ERROR', 'Search failed: ${e.message}');
            """.trimIndent())
            throw e
        }
    }

    /**
     * 获取某首歌的多音质音频资源。
     *
     * 插件侧约定：实现 plugin.getAudioResources(songId)，返回：
     * - 直接为 MistyAudioResourceBundle 对象，或
     * - 对象 { bundle: { ... }, ... } 或 { audioResourceBundle: { ... }, ... }。
     */
    suspend fun getAudioResources(pluginId: String, songId: String): MistyAudioResourceBundle {
        try {
            val script = """
                (async function() {
                    if (!MistyPlugins || !MistyPlugins['$pluginId']) {
                        throw new Error('Plugin not found: $pluginId');
                    }
                    const plugin = MistyPlugins['$pluginId'];
                    if (typeof plugin.getAudioResources !== 'function') {
                        throw new Error('Plugin $pluginId does not have a getAudioResources function');
                    }
                    const result = await plugin.getAudioResources('$songId');
                    return JSON.stringify(result);
                })();
            """.trimIndent()

            val resultJson = jsEngine.executeAsyncScript(script)

            val bundle: MistyAudioResourceBundle = try {
                val jsonElement = json.parseToJsonElement(resultJson)
                when {
                    jsonElement.jsonObject.containsKey("bundle") ->
                        json.decodeFromString(jsonElement.jsonObject["bundle"].toString())

                    jsonElement.jsonObject.containsKey("audioResourceBundle") ->
                        json.decodeFromString(jsonElement.jsonObject["audioResourceBundle"].toString())

                    else ->
                        json.decodeFromString(resultJson)
                }
            } catch (e: Exception) {
                // 退化为直接解析 bundle
                json.decodeFromString(resultJson)
            }

            return bundle
        } catch (e: Exception) {
            jsEngine.executeScript("""
                mistyInternal.log('ERROR', 'getAudioResources failed: ${e.message}');
            """.trimIndent())
            throw e
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
                (async function() {
                    if (!MistyPlugins || !MistyPlugins['$pluginId']) {
                        throw new Error('Plugin not found: $pluginId');
                    }
                    const plugin = MistyPlugins['$pluginId'];
                    if (typeof plugin.getPlaylist !== 'function') {
                        throw new Error('Plugin $pluginId does not have a getPlaylist function');
                    }
                    const result = await plugin.getPlaylist('$playlistId');
                    return JSON.stringify(result);
                })();
            """.trimIndent()

            val resultJson = jsEngine.executeAsyncScript(script)

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
            jsEngine.executeScript("""
                mistyInternal.log('ERROR', 'getPlaylist failed: ${e.message}');
            """.trimIndent())
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
                (async function() {
                    if (!MistyPlugins || !MistyPlugins['$pluginId']) {
                        throw new Error('Plugin not found: $pluginId');
                    }
                    const plugin = MistyPlugins['$pluginId'];
                    if (typeof plugin.getAlbum !== 'function') {
                        throw new Error('Plugin $pluginId does not have a getAlbum function');
                    }
                    const result = await plugin.getAlbum('$albumId');
                    return JSON.stringify(result);
                })();
            """.trimIndent()

            val resultJson = jsEngine.executeAsyncScript(script)

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
            jsEngine.executeScript("""
                mistyInternal.log('ERROR', 'getAlbum failed: ${e.message}');
            """.trimIndent())
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
                (async function() {
                    if (!MistyPlugins || !MistyPlugins['$pluginId']) {
                        throw new Error('Plugin not found: $pluginId');
                    }
                    const plugin = MistyPlugins['$pluginId'];
                    if (typeof plugin.getLyrics !== 'function') {
                        throw new Error('Plugin $pluginId does not have a getLyrics function');
                    }
                    const result = await plugin.getLyrics('$songId');
                    return JSON.stringify(result);
                })();
            """.trimIndent()

            val resultJson = jsEngine.executeAsyncScript(script)

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
            jsEngine.executeScript("""
                mistyInternal.log('ERROR', 'getLyrics failed: ${e.message}');
            """.trimIndent())
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
                })();
            """.trimIndent()

            jsEngine.executeScript(script)
        } catch (e: Exception) {
            jsEngine.executeScript("""
                mistyInternal.log('ERROR', 'Failed to load plugin $pluginId: ${e.message}');
            """.trimIndent())
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
                    return MistyPlugins && MistyPlugins['$pluginId'] !== undefined;
                })();
            """.trimIndent()

            val result = jsEngine.executeScript(script)
            result == "true"
        } catch (e: Exception) {
            false
        }
    }
}
