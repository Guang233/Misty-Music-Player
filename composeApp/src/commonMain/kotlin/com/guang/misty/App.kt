package com.guang.misty

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalMaterial3Api::class)
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
    
    // 播放器 BottomSheet 状态
    val isPlayerVisible = currentSubScreen is SubScreen.PlayerDetail
    val playerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    // 控制 BottomSheet 的显示（先渲染再动画展开）
    var showPlayerSheet by remember { mutableStateOf(false) }
    
    // 当 isPlayerVisible 变化时，控制 Sheet 展开/收起动画
    LaunchedEffect(isPlayerVisible) {
        if (isPlayerVisible) {
            showPlayerSheet = true
        } else {
            playerSheetState.hide()
            showPlayerSheet = false
        }
    }
    
    // 监听 Sheet 状态：当 sheet 被内部隐藏时（如下滑或内部 back handler），同步更新导航状态
    LaunchedEffect(playerSheetState.isVisible) {
        if (!playerSheetState.isVisible && isPlayerVisible) {
            currentSubScreen = null
        }
    }
    
    // 非播放器子页面
    val nonPlayerSubScreen = if (isPlayerVisible) null else currentSubScreen
    
    // 处理系统返回键（非播放器子页面）
    BackHandler(enabled = nonPlayerSubScreen != null) {
        currentSubScreen = null
    }
    
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ===== 底层：主导航 + 非播放器子页面 =====
        AnimatedContent(
            targetState = nonPlayerSubScreen,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                if (targetState != null) {
                    slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it / 3 } + fadeOut()
                } else {
                    slideInHorizontally { -it / 3 } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                }
            }
        ) { subScreen ->
            if (subScreen != null) {
                when (subScreen) {
                    is SubScreen.PluginManagement -> {
                        PluginManagementScreen(
                            onNavigateBack = { currentSubScreen = null }
                        )
                    }
                    is SubScreen.Debug -> {
                        DebugScreen { currentSubScreen = null }
                    }
                    else -> {}
                }
            } else {
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
                                onNavigateToResult = { query -> }
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
        
        // ===== 播放器：魔改 ModalBottomSheet =====
        if (showPlayerSheet) {
            // 根据 Sheet 展开程度动态计算圆角
            // offset: 0 = 完全展开, 正值 = 向下拖拽中
            val sheetOffset = try {
                playerSheetState.requireOffset()
            } catch (_: Exception) {
                0f
            }
            val density = LocalDensity.current
            val cornerRadius = with(density) {
                // offset > 0 时才有圆角，最大 28dp
                val maxCorner = 28.dp.toPx()
                val corner = (sheetOffset / 8f).coerceIn(0f, maxCorner)
                corner.toDp()
            }
            
            ModalBottomSheet(
                onDismissRequest = { currentSubScreen = null },
                sheetState = playerSheetState,
                shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onSurface,
                dragHandle = null,
                scrimColor = Color.Black.copy(alpha = 0.3f),
                contentWindowInsets = { WindowInsets(0) },
                modifier = Modifier.fillMaxSize()
            ) {
                // 在 Sheet 内容内部拦截返回，优先级高于 ModalBottomSheet 内部的预见性返回
                // 直接向下关闭，不触发默认的缩放预览动画
                BackHandler(enabled = true) {
                    coroutineScope.launch {
                        playerSheetState.hide()
                        currentSubScreen = null
                    }
                }
                
                PlayerScreen(
                    onNavigateBack = {
                        coroutineScope.launch {
                            playerSheetState.hide()
                            currentSubScreen = null
                        }
                    },
                    onNavigateToQueue = { /* TODO */ },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
