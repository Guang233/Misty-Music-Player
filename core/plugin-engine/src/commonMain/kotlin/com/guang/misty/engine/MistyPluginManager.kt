package com.guang.misty.engine

import com.guang.misty.model.MistySong
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
                    json.decodeFromString<List<MistySong>>(resultJson)
                }
            } catch (e: Exception) {
                // 如果解析失败，尝试直接解析整个 JSON
                json.decodeFromString<List<MistySong>>(resultJson)
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
