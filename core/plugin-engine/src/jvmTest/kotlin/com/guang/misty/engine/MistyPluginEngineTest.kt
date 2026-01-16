package com.guang.misty.engine

import com.guang.misty.engine.bridge.MistyBridge
import com.guang.misty.model.MistyAudioQuality
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Misty 插件引擎测试
 * 
 * 测试脚本系统的核心功能：
 * 1. JS 引擎基本执行
 * 2. 插件注册与加载
 * 3. 搜索功能
 * 4. 音频资源获取
 * 5. 加解密功能
 */
class MistyPluginEngineTest {

    /**
     * 测试用的 Mock Bridge
     */
    private class TestBridge : MistyBridge {
        val logs = mutableListOf<Pair<String, String>>()

        override suspend fun networkRequest(json: String): String {
            // 模拟网络请求返回
            return """{"statusCode":200,"headers":{},"body":"{}","error":null}"""
        }

        override fun log(level: String, msg: String) {
            logs.add(level to msg)
            println("[$level] $msg")
        }
    }

    @Test
    fun testJsEngineBasicExecution() = runBlocking {
        val bridge = TestBridge()
        val engine = MistyJsEngine(bridge)

        try {
            // 测试基本 JS 执行（使用 String() 转换，因为 QuickJS 返回原始类型）
            val result = engine.executeScript("String(1 + 1)")
            assertEquals("2", result)

            // 测试字符串
            val strResult = engine.executeScript("'hello' + ' world'")
            assertEquals("hello world", strResult)

            // 测试 JSON
            val jsonResult = engine.executeScript("JSON.stringify({a: 1, b: 2})")
            assertEquals("""{"a":1,"b":2}""", jsonResult)

            println("✅ JS 引擎基本执行测试通过")
        } finally {
            engine.close()
        }
    }

    @Test
    fun testMistyPluginsGlobalObject() = runBlocking {
        val bridge = TestBridge()
        val engine = MistyJsEngine(bridge)

        try {
            // 测试 MistyPlugins 全局对象存在
            val result = engine.executeScript("typeof MistyPlugins")
            assertEquals("object", result)

            // 测试 misty 全局对象存在
            val mistyResult = engine.executeScript("typeof misty")
            assertEquals("object", mistyResult)

            // 测试 misty.audio 存在
            val audioResult = engine.executeScript("typeof misty.audio")
            assertEquals("object", audioResult)

            // 测试 misty.crypto 存在
            val cryptoResult = engine.executeScript("typeof misty.crypto")
            assertEquals("object", cryptoResult)

            println("✅ 全局对象测试通过")
        } finally {
            engine.close()
        }
    }

    @Test
    fun testCryptoMd5() = runBlocking {
        val bridge = TestBridge()
        val engine = MistyJsEngine(bridge)

        try {
            // 测试 MD5
            val md5Result = engine.executeScript("misty.crypto.md5('hello')")
            assertEquals("5d41402abc4b2a76b9719d911017c592", md5Result)

            println("✅ MD5 加密测试通过")
        } finally {
            engine.close()
        }
    }

    @Test
    fun testCryptoSha256() = runBlocking {
        val bridge = TestBridge()
        val engine = MistyJsEngine(bridge)

        try {
            // 测试 SHA256
            val sha256Result = engine.executeScript("misty.crypto.sha256('hello')")
            assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", sha256Result)

            println("✅ SHA256 加密测试通过")
        } finally {
            engine.close()
        }
    }

    @Test
    fun testCryptoAesCbc() = runBlocking {
        val bridge = TestBridge()
        val engine = MistyJsEngine(bridge)

        try {
            // 测试 AES-CBC 加密
            val encrypted = engine.executeScript(
                "misty.crypto.aesEncryptToBase64('hello world', '1234567890123456', '1234567890123456')"
            )
            assertNotNull(encrypted)
            assertTrue(encrypted.isNotEmpty())

            // 测试 AES-CBC 解密
            val decrypted = engine.executeScript(
                "misty.crypto.aesDecryptFromBase64('$encrypted', '1234567890123456', '1234567890123456')"
            )
            assertEquals("hello world", decrypted)

            println("✅ AES-CBC 加解密测试通过")
        } finally {
            engine.close()
        }
    }

    @Test
    fun testCryptoAesEcb() = runBlocking {
        val bridge = TestBridge()
        val engine = MistyJsEngine(bridge)

        try {
            // 测试 AES-ECB 加密
            val encrypted = engine.executeScript(
                "misty.crypto.aesEcbEncryptToBase64('hello world', '1234567890123456')"
            )
            assertNotNull(encrypted)
            assertTrue(encrypted.isNotEmpty())

            // 测试 AES-ECB 解密
            val decrypted = engine.executeScript(
                "misty.crypto.aesEcbDecryptFromBase64('$encrypted', '1234567890123456')"
            )
            assertEquals("hello world", decrypted)

            println("✅ AES-ECB 加解密测试通过")
        } finally {
            engine.close()
        }
    }

    @Test
    fun testPluginRegistration() = runBlocking {
        val bridge = TestBridge()
        val engine = MistyJsEngine(bridge)

        try {
            // 注册一个测试插件（注意脚本末尾返回 undefined 避免返回对象）
            engine.executeScript(
                """
                MistyPlugins['test'] = {
                    meta: {
                        id: 'test',
                        name: 'Test Plugin',
                        version: '1.0.0',
                        capabilities: ['SEARCH', 'AUDIO_RESOURCES']
                    },
                    search: async function(keyword, page) {
                        return [{
                            id: '123',
                            source: 'test',
                            name: 'Test Song - ' + keyword,
                            artists: [{ id: '1', source: 'test', name: 'Test Artist', coverUrl: null }]
                        }];
                    },
                    getAudioResource: async function(songId, quality) {
                        return misty.audio.successResult(
                            songId,
                            quality,
                            quality,
                            'https://example.com/audio.mp3',
                            { format: 'mp3', bitrateKbps: 320 }
                        );
                    }
                };
                undefined;
                """.trimIndent()
            )

            // 验证插件已注册（使用 String() 转换布尔值）
            val hasPlugin = engine.executeScript("String(MistyPlugins['test'] !== undefined)")
            assertEquals("true", hasPlugin)

            // 验证 meta 信息
            val pluginName = engine.executeScript("MistyPlugins['test'].meta.name")
            assertEquals("Test Plugin", pluginName)

            println("✅ 插件注册测试通过")
        } finally {
            engine.close()
        }
    }

    @Test
    fun testPluginManagerSearch() = runBlocking {
        val bridge = TestBridge()
        val engine = MistyJsEngine(bridge)
        val manager = MistyPluginManager(engine)

        try {
            // 注册测试插件 - 使用同步函数避免 Promise 问题
            manager.loadPlugin(
                "test",
                """
                MistyPlugins['test'] = {
                    meta: {
                        id: 'test',
                        name: 'Test Plugin',
                        version: '1.0.0',
                        capabilities: ['SEARCH']
                    },
                    search: function(keyword, page) {
                        return [{
                            id: '123',
                            source: 'test',
                            name: 'Test Song - ' + keyword,
                            artists: [{ id: '1', source: 'test', name: 'Test Artist', coverUrl: null }]
                        }];
                    }
                };
                """.trimIndent()
            )

            // 测试搜索
            val results = manager.search("test", "test_keyword", 1)

            assertNotNull(results)
            assertTrue(results.isNotEmpty())
            assertEquals("123", results[0].id)
            assertEquals("test", results[0].source)
            assertTrue(results[0].name.contains("test_keyword"))

            println("✅ 插件管理器搜索测试通过")
        } finally {
            engine.close()
        }
    }

    @Test
    fun testPluginManagerGetAudioResource() = runBlocking {
        val bridge = TestBridge()
        val engine = MistyJsEngine(bridge)
        val manager = MistyPluginManager(engine)

        try {
            // 注册测试插件 - 使用同步函数
            manager.loadPlugin(
                "test",
                """
                MistyPlugins['test'] = {
                    meta: {
                        id: 'test',
                        name: 'Test Plugin',
                        capabilities: ['AUDIO_RESOURCES']
                    },
                    getAudioResource: function(songId, quality) {
                        // 模拟降级：请求 LOSSLESS 但返回 HIGH
                        var actualQuality = quality;
                        if (quality === 'LOSSLESS') {
                            actualQuality = 'HIGH';
                        }
                        return misty.audio.successResult(
                            songId,
                            quality,
                            actualQuality,
                            'https://example.com/audio.mp3',
                            { format: 'mp3', bitrateKbps: 320 }
                        );
                    }
                };
                """.trimIndent()
            )

            // 测试获取音频资源（正常情况）
            val result = manager.getAudioResource("test", "song123", MistyAudioQuality.HIGH)

            assertNotNull(result)
            assertEquals("song123", result.songId)
            assertEquals(MistyAudioQuality.HIGH, result.requestedQuality)
            assertNotNull(result.resource)
            assertEquals(MistyAudioQuality.HIGH, result.resource?.quality)
            assertEquals("https://example.com/audio.mp3", result.resource?.url)

            // 测试降级场景
            val degradedResult = manager.getAudioResource("test", "song123", MistyAudioQuality.LOSSLESS)

            assertNotNull(degradedResult)
            assertEquals(MistyAudioQuality.LOSSLESS, degradedResult.requestedQuality)
            assertEquals(MistyAudioQuality.HIGH, degradedResult.resource?.quality) // 降级为 HIGH

            println("✅ 音频资源获取测试通过（含降级场景）")
        } finally {
            engine.close()
        }
    }

    @Test
    fun testPluginManagerGetLyrics() = runBlocking {
        val bridge = TestBridge()
        val engine = MistyJsEngine(bridge)
        val manager = MistyPluginManager(engine)

        try {
            // 注册测试插件 - 使用同步函数
            manager.loadPlugin(
                "test",
                """
                MistyPlugins['test'] = {
                    meta: {
                        id: 'test',
                        name: 'Test Plugin',
                        capabilities: ['LYRICS']
                    },
                    getLyrics: function(songId) {
                        return {
                            songId: songId,
                            lyrics: [
                                {
                                    content: '[00:00.00]Test lyrics line 1',
                                    type: 'ORIGINAL',
                                    format: 'LINE_BY_LINE'
                                },
                                {
                                    content: '[00:00.00]Translation line 1',
                                    type: 'TRANSLATION',
                                    format: 'LINE_BY_LINE'
                                }
                            ]
                        };
                    }
                };
                """.trimIndent()
            )

            // 测试获取歌词
            val result = manager.getLyrics("test", "song123")

            assertNotNull(result)
            assertEquals("song123", result.songId)
            assertEquals(2, result.lyrics.size)
            assertEquals("ORIGINAL", result.lyrics[0].type.name)
            assertEquals("TRANSLATION", result.lyrics[1].type.name)

            println("✅ 歌词获取测试通过")
        } finally {
            engine.close()
        }
    }

    @Test
    fun testPluginManagerListPlugins() = runBlocking {
        val bridge = TestBridge()
        val engine = MistyJsEngine(bridge)
        val manager = MistyPluginManager(engine)

        try {
            // 注册多个测试插件
            manager.loadPlugin(
                "plugin1",
                """
                MistyPlugins['plugin1'] = {
                    meta: { id: 'plugin1', name: 'Plugin 1' }
                };
                """.trimIndent()
            )

            manager.loadPlugin(
                "plugin2",
                """
                MistyPlugins['plugin2'] = {
                    meta: { id: 'plugin2', name: 'Plugin 2' }
                };
                """.trimIndent()
            )

            // 测试列出所有插件
            val pluginIds = manager.listPluginIds()

            assertNotNull(pluginIds)
            assertTrue(pluginIds.contains("plugin1"))
            assertTrue(pluginIds.contains("plugin2"))

            // 测试获取插件元信息
            val meta1 = manager.getPluginMeta("plugin1")
            assertEquals("plugin1", meta1.id)
            assertEquals("Plugin 1", meta1.name)

            println("✅ 插件列表测试通过")
        } finally {
            engine.close()
        }
    }

    @Test
    fun testAudioQualityEnum() = runBlocking {
        val bridge = TestBridge()
        val engine = MistyJsEngine(bridge)

        try {
            // 测试 misty.audio.Quality 枚举
            val standard = engine.executeScript("misty.audio.Quality.STANDARD")
            assertEquals("STANDARD", standard)

            val high = engine.executeScript("misty.audio.Quality.HIGH")
            assertEquals("HIGH", high)

            val lossless = engine.executeScript("misty.audio.Quality.LOSSLESS")
            assertEquals("LOSSLESS", lossless)

            val hiRes = engine.executeScript("misty.audio.Quality.HI_RES")
            assertEquals("HI_RES", hiRes)

            println("✅ 音质枚举测试通过")
        } finally {
            engine.close()
        }
    }

    @Test
    fun testMistyAudioHelpers() = runBlocking {
        val bridge = TestBridge()
        val engine = MistyJsEngine(bridge)

        try {
            // 测试 successResult
            val successResultJson = engine.executeScript(
                """
                JSON.stringify(misty.audio.successResult(
                    'song123',
                    'LOSSLESS',
                    'HIGH',
                    'https://example.com/audio.flac',
                    { format: 'flac', bitrateKbps: 1000 }
                ))
                """.trimIndent()
            )
            assertTrue(successResultJson.contains("song123"))
            assertTrue(successResultJson.contains("LOSSLESS"))
            assertTrue(successResultJson.contains("HIGH"))

            // 测试 errorResult
            val errorResultJson = engine.executeScript(
                """
                JSON.stringify(misty.audio.errorResult(
                    'song123',
                    'HI_RES',
                    'Resource not available'
                ))
                """.trimIndent()
            )
            assertTrue(errorResultJson.contains("song123"))
            assertTrue(errorResultJson.contains("HI_RES"))
            assertTrue(errorResultJson.contains("Resource not available"))
            assertTrue(errorResultJson.contains("\"resource\":null"))

            println("✅ misty.audio 辅助方法测试通过")
        } finally {
            engine.close()
        }
    }
}
