package com.guang.misty

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guang.misty.ui.components.CurrentSongInfo
import com.guang.misty.ui.components.MiniPlayer
import com.guang.misty.ui.navigation.MainDestination
import com.guang.misty.ui.navigation.MistyNavigation
import com.guang.misty.ui.navigation.SubScreen
import com.guang.misty.ui.screens.library.LibraryScreen
import com.guang.misty.ui.screens.playing.PlayingScreen
import com.guang.misty.ui.screens.search.SearchScreen
import com.guang.misty.ui.screens.settings.SettingsScreen
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
    
    // TODO: 从 ViewModel 获取播放状态
    var currentSong by remember { mutableStateOf<CurrentSongInfo?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    
    // 示例：模拟有歌曲在播放
    // currentSong = CurrentSongInfo(
    //     id = "1",
    //     title = "晴天",
    //     artist = "周杰伦",
    //     duration = 269000,
    //     currentPosition = 83000
    // )
    // progress = 0.31f
    
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
            if (targetState != null) {
                // 进入子页面：从右滑入
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
                onPlayPauseClick = { isPlaying = !isPlaying },
                onNextClick = { /* TODO */ },
                onClick = { /* TODO: 打开全屏播放器 */ }
            )
        }
    ) {
        // 根据当前目的地显示对应页面
        when (currentDestination) {
            MainDestination.Playing -> {
                PlayingScreen(
                    onNavigateToSearch = { currentDestination = MainDestination.Search },
                    onNavigateToLibrary = { currentDestination = MainDestination.Library },
                    onNavigateToPlayer = { /* TODO: 全屏播放器 */ }
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
