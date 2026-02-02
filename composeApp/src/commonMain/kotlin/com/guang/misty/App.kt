package com.guang.misty

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guang.misty.player.PlayerService
import com.guang.misty.player.PlayerState
import com.guang.misty.player.PlaybackState
import com.guang.misty.ui.components.CurrentSongInfo
import com.guang.misty.ui.components.MiniPlayer
import com.guang.misty.ui.navigation.MainDestination
import com.guang.misty.ui.navigation.MistyNavigation
import com.guang.misty.ui.navigation.SubScreen
import com.guang.misty.ui.screens.library.LibraryScreen
import com.guang.misty.ui.screens.playing.PlayingScreen
import com.guang.misty.ui.screens.search.SearchScreen
import com.guang.misty.ui.screens.settings.SettingsScreen
import com.guang.misty.ui.screens.player.PlayerScreen
import com.guang.misty.ui.screens.settings.debug.DebugScreen
import com.guang.misty.ui.screens.settings.plugins.PluginManagementScreen
import com.guang.misty.ui.theme.*
import com.guang.misty.util.BackHandler
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    val themeState = remember { ThemeState() }
    
    MistyTheme(themeState = themeState) {
        MistyApp()
    }
}

@Composable
private fun MistyApp() {
    // 计算窗口尺寸类别
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val windowSizeClass = remember(maxWidth, maxHeight) {
            val widthSizeClass = when {
                maxWidth < 600.dp -> WindowWidthSizeClass.Compact
                maxWidth < 840.dp -> WindowWidthSizeClass.Medium
                else -> WindowWidthSizeClass.Expanded
            }
            val heightSizeClass = when {
                maxHeight < 480.dp -> WindowHeightSizeClass.Compact
                maxHeight < 900.dp -> WindowHeightSizeClass.Medium
                else -> WindowHeightSizeClass.Expanded
            }
            WindowSizeClass(widthSizeClass, heightSizeClass)
        }
        
        CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
            MistyMainContent()
        }
    }
}

@Composable
private fun MistyMainContent() {
    // 当前导航目的地
    var currentDestination by remember { mutableStateOf<MainDestination>(MainDestination.Playing) }
    
    // 子页面导航状态
    var currentSubScreen by remember { mutableStateOf<SubScreen?>(null) }
    
    // 从 PlayerService 获取播放状态
    val playerState by PlayerService.state.collectAsState()
    
    // 转换为 MiniPlayer 所需的格式
    val currentSong = remember(playerState.currentSong) {
        playerState.currentSong?.let { song ->
            CurrentSongInfo(
                id = song.id,
                title = song.name,
                artist = song.artists.joinToString(", ") { it.name },
                coverUrl = song.coverUrl,
                duration = playerState.duration,
                currentPosition = playerState.position
            )
        }
    }
    val isPlaying = playerState.isPlaying
    val progress = remember(playerState.position, playerState.duration) {
        if (playerState.duration > 0) {
            playerState.position.toFloat() / playerState.duration.toFloat()
        } else 0f
    }
    
    // 处理系统返回键：如果在子页面，返回主导航
    BackHandler(enabled = currentSubScreen != null) {
        currentSubScreen = null
    }
    
    // 如果有子页面，显示子页面
    AnimatedContent(
        targetState = currentSubScreen,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        transitionSpec = {
            // 播放器界面使用上下滑动动画
            val isPlayerScreen = targetState is SubScreen.PlayerDetail || initialState is SubScreen.PlayerDetail
            
            if (isPlayerScreen) {
                if (targetState != null) {
                    // 进入播放器：从下滑入
                    slideInVertically { it } + fadeIn() togetherWith
                        fadeOut()
                } else {
                    // 退出播放器：向下滑出
                    fadeIn() togetherWith
                        slideOutVertically { it } + fadeOut()
                }
            } else if (targetState != null) {
                // 进入其他子页面：从右滑入
                slideInHorizontally { it } + fadeIn() togetherWith
                    slideOutHorizontally { -it / 3 } + fadeOut()
            } else {
                // 返回主页面：从左滑入
                slideInHorizontally { -it / 3 } + fadeIn() togetherWith
                    slideOutHorizontally { it } + fadeOut()
            }
        }
    ) { subScreen ->
        if (subScreen != null) {
            // 显示子页面
            when (subScreen) {
                is SubScreen.PluginManagement -> {
                    PluginManagementScreen(
                        onNavigateBack = { currentSubScreen = null }
                    )
                }

                is SubScreen.Debug -> {
                    DebugScreen { currentSubScreen = null }
                }
                is SubScreen.PlayerDetail -> {
                    PlayerScreen(
                        onNavigateBack = { currentSubScreen = null },
                        onNavigateToQueue = { /* TODO: 跳转到播放队列 */ }
                    )
                }
                else -> {
                    // 其他子页面待实现
                }
            }
        } else {
            // 显示主导航
    MistyNavigation(
        currentDestination = currentDestination,
        onNavigate = { currentDestination = it },
        miniPlayer = {
            MiniPlayer(
                currentSong = currentSong,
                isPlaying = isPlaying,
                progress = progress,
                onPlayPauseClick = { PlayerService.playPause() },
                onNextClick = { PlayerService.next() },
                onClick = { currentSubScreen = SubScreen.PlayerDetail }
            )
        }
    ) {
        // 根据当前目的地显示对应页面
        when (currentDestination) {
            MainDestination.Playing -> {
                PlayingScreen(
                    onNavigateToSearch = { currentDestination = MainDestination.Search },
                    onNavigateToLibrary = { currentDestination = MainDestination.Library },
                    onNavigateToPlayer = { currentSubScreen = SubScreen.PlayerDetail }
                )
            }
            MainDestination.Search -> {
                SearchScreen(
                    onNavigateToResult = { query ->
                        // 搜索结果已在 SearchScreen 内部显示
                    }
                )
            }
            MainDestination.Library -> {
                LibraryScreen(
                    onNavigateToLocalMusic = { /* TODO */ },
                    onNavigateToDownloads = { /* TODO */ },
                    onNavigateToPlaylist = { playlistId -> /* TODO */ },
                    onCreatePlaylist = { /* TODO */ }
                )
            }
            MainDestination.Settings -> {
                SettingsScreen(
                    onNavigateToPlugins = { currentSubScreen = SubScreen.PluginManagement },
                    onNavigateToAbout = { /* TODO */ },
                    onNavigateToDebug = { currentSubScreen = SubScreen.Debug }
                )
                    }
                }
            }
        }
    }
}
