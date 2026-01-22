// ===== 示例插件：需要登录的音乐平台 =====
// 展示如何使用 misty.auth API 处理需要登录的音乐服务

if (typeof MistyPlugins === 'undefined') {
    MistyPlugins = {};
}

const PLUGIN_ID = 'example_with_login';

MistyPlugins[PLUGIN_ID] = {
    // ===== 插件元信息 =====
    meta: {
        id: PLUGIN_ID,
        name: '示例音乐平台 (需要登录)',
        author: 'Misty Team',
        version: '1.0.0',
        description: '展示如何处理需要登录的音乐平台',
        homepage: 'https://github.com/your/repo',
        license: 'MIT',
        sourceName: 'Example Music',
        sourceHomepage: 'https://music.example.com',
        capabilities: ['SEARCH', 'AUDIO_RESOURCES', 'LYRICS'],
        supportRegions: ['CN'],
        disclaimer: 'This is an example plugin for demonstration purposes.',
        auth: {
            required: false,
            loginUrl: 'https://music.example.com/login',
            loginMethod: 'COOKIE',
            supportAutoLogin: false
        }
    },

    // ===== 辅助方法 =====
    
    /**
     * 获取认证头（带 Cookie）
     */
    getAuthHeaders: function(domain) {
        var cookies = misty.auth.getCookies(domain);
        var headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        };
        
        if (cookies.length > 0) {
            headers['Cookie'] = misty.auth.toCookieString(cookies);
            misty.log.debug('[' + PLUGIN_ID + '] Using ' + cookies.length + ' cookies');
        }
        
        return headers;
    },

    /**
     * 检查响应是否需要登录
     */
    needsLogin: function(data) {
        // 根据实际 API 返回判断
        return data.code === 401 || 
               data.code === 403 || 
               data.message === 'Need login' ||
               data.needLogin === true;
    },

    /**
     * 请求用户登录
     */
    requestLogin: function() {
        misty.log.info('[' + PLUGIN_ID + '] Requesting user login...');
        
        var result = misty.auth.login('https://music.example.com/login');
        
        if (result.success) {
            misty.log.info('[' + PLUGIN_ID + '] Login successful! Got ' + result.cookies.length + ' cookies');
            return true;
        } else {
            misty.log.error('[' + PLUGIN_ID + '] Login failed: ' + result.error);
            return false;
        }
    },

    /**
     * 带自动登录的 HTTP 请求
     * @param {string} url - 请求 URL
     * @param {boolean} retryLogin - 是否在需要登录时自动重试
     */
    requestWithAuth: function(url, retryLogin) {
        var domain = 'music.example.com';
        var headers = this.getAuthHeaders(domain);
        
        // 第一次请求
        var resp = misty.http.get(url, headers);
        var data = JSON.parse(resp);
        
        // 检查是否需要登录
        if (this.needsLogin(data) && retryLogin) {
            misty.log.info('[' + PLUGIN_ID + '] Authentication required, requesting login...');
            
            // 请求登录
            if (this.requestLogin()) {
                // 登录成功，重新获取认证头并重试
                headers = this.getAuthHeaders(domain);
                resp = misty.http.get(url, headers);
                data = JSON.parse(resp);
                
                // 再次检查
                if (this.needsLogin(data)) {
                    throw new Error('Still requires login after authentication');
                }
            } else {
                throw new Error('User cancelled login or login failed');
            }
        }
        
        return data;
    },

    // ===== 插件功能实现 =====

    /**
     * 搜索歌曲
     */
    search: function(keyword, page) {
        misty.log.info('[' + PLUGIN_ID + '] Searching: ' + keyword + ', page: ' + page);
        
        try {
            var url = 'https://api.music.example.com/search?q=' + 
                      encodeURIComponent(keyword) + '&page=' + page;
            
            var data = this.requestWithAuth(url, true);
            
            // 解析结果（根据实际 API 格式）
            var songs = data.songs || data.result || [];
            
            return songs.map(function(item) {
                return {
                    id: String(item.id),
                    source: PLUGIN_ID,
                    name: item.name,
                    artists: (item.artists || []).map(function(artist) {
                        return {
                            id: String(artist.id),
                            source: PLUGIN_ID,
                            name: artist.name,
                            coverUrl: artist.avatar || null
                        };
                    }),
                    album: item.album ? {
                        id: String(item.album.id),
                        source: PLUGIN_ID,
                        name: item.album.name,
                        coverUrl: item.album.cover || null
                    } : null,
                    coverUrl: item.cover || (item.album ? item.album.cover : null),
                    extras: {}
                };
            });
        } catch (err) {
            misty.log.error('[' + PLUGIN_ID + '] Search failed: ' + err.message);
            throw err;
        }
    },

    /**
     * 获取音频资源
     */
    getAudioResource: function(songId, quality) {
        misty.log.info('[' + PLUGIN_ID + '] Getting audio resource: ' + songId + ', quality: ' + quality);
        
        try {
            var url = 'https://api.music.example.com/song/' + 
                      encodeURIComponent(songId) + '/url?quality=' + quality;
            
            var data = this.requestWithAuth(url, true);
            
            // 检查是否有可用资源
            if (!data.url || data.url === '') {
                return misty.audio.errorResult(
                    songId, 
                    quality, 
                    'No audio resource available for this quality'
                );
            }
            
            // 检查是否降级
            var actualQuality = data.quality || quality;
            
            if (actualQuality !== quality) {
                misty.log.warn('[' + PLUGIN_ID + '] Quality degraded: ' + quality + ' -> ' + actualQuality);
            }
            
            return misty.audio.successResult(
                songId,
                quality,
                actualQuality,
                data.url,
                {
                    format: data.format || 'mp3',
                    bitrateKbps: data.bitrate || 320,
                    fileSizeBytes: data.size || null,
                    md5: data.md5 || null
                }
            );
        } catch (err) {
            misty.log.error('[' + PLUGIN_ID + '] Get audio resource failed: ' + err.message);
            return misty.audio.errorResult(songId, quality, err.message);
        }
    },

    /**
     * 获取歌词
     */
    getLyrics: function(songId) {
        misty.log.info('[' + PLUGIN_ID + '] Getting lyrics: ' + songId);
        
        try {
            var url = 'https://api.music.example.com/song/' + 
                      encodeURIComponent(songId) + '/lyrics';
            
            var data = this.requestWithAuth(url, true);
            
            var lyrics = [];
            
            // 原文歌词
            if (data.original) {
                lyrics.push({
                    content: data.original,
                    type: 'ORIGINAL',
                    format: 'LINE_BY_LINE'
                });
            }
            
            // 翻译歌词
            if (data.translation) {
                lyrics.push({
                    content: data.translation,
                    type: 'TRANSLATION',
                    format: 'LINE_BY_LINE'
                });
            }
            
            // 罗马音
            if (data.romanization) {
                lyrics.push({
                    content: data.romanization,
                    type: 'ROMANIZATION',
                    format: 'LINE_BY_LINE'
                });
            }
            
            return {
                songId: songId,
                lyrics: lyrics
            };
        } catch (err) {
            misty.log.error('[' + PLUGIN_ID + '] Get lyrics failed: ' + err.message);
            throw err;
        }
    },

    /**
     * 退出登录（可选功能）
     */
    logout: function() {
        misty.log.info('[' + PLUGIN_ID + '] Logging out...');
        misty.auth.clearCookies();
        misty.log.info('[' + PLUGIN_ID + '] Logged out successfully');
    }
};

// 返回 undefined 避免脚本返回对象
undefined;
