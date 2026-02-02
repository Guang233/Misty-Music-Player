package com.guang.misty.ui.screens.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.guang.misty.model.MistyArtist
import com.guang.misty.player.PlayerService
import com.guang.misty.player.RepeatMode
import com.guang.misty.ui.screens.player.components.BlurredBackground
import com.guang.misty.ui.screens.player.components.LyricLine
import com.guang.misty.ui.screens.player.components.PlayerControls
import com.guang.misty.ui.screens.player.components.PlayerControlsExpanded
import com.guang.misty.ui.screens.player.components.PlayerCover
import com.guang.misty.ui.screens.player.components.PlayerLyrics
import com.guang.misty.ui.screens.player.components.PlayerProgressBar
import com.guang.misty.ui.screens.player.components.PlayerProgressBarExpanded
import com.guang.misty.ui.screens.player.components.PlayerSongInfo
import com.guang.misty.ui.screens.player.components.PlayerSongInfoSimple
import com.guang.misty.ui.screens.player.components.PlayerToolbar
import com.guang.misty.ui.screens.player.components.PlayerToolbarLeft
import com.guang.misty.ui.screens.player.components.PlayerToolbarRight
import com.guang.misty.ui.screens.player.components.PlayerTopBar
import com.guang.misty.ui.screens.player.components.TopBarMode
import com.guang.misty.ui.theme.LocalWindowSizeClass
import com.guang.misty.ui.theme.WindowWidthSizeClass
import misty.composeapp.generated.resources.Res
import misty.composeapp.generated.resources.player_no_song
import org.jetbrains.compose.resources.stringResource

/**
 * 全屏播放器界面 - MD3 Expressive 风格
 * 
 * 响应式布局：
 * - Compact（小屏幕）: 左右滑动切换封面/歌词，支持沉浸模式
 * - Medium/Expanded（大屏幕）: 左侧封面+信息，右侧歌词，底部控制
 * 
 * 特点：
 * - 模糊封面背景
 * - 沉浸模式（隐藏控制，仅显示歌词）
 * - 流畅的过渡动画
 */
@Composable
fun PlayerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQueue: () -> Unit = {},
    onNavigateToSongDetail: () -> Unit = {},
    onNavigateToArtist: (MistyArtist) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    
    // 获取播放状态
    val playerState by PlayerService.state.collectAsState()
    
    // 沉浸模式状态
    var isImmersive by remember { mutableStateOf(false) }
    
    // TODO: 从歌词服务获取
    val lyrics = remember {
        listOf(
            LyricLine(0, "暂无歌词"),
            LyricLine(1000, ""),
            LyricLine(2000, "歌词加载中..."),
        )
    }
    val currentLyricIndex = 0
    
    // 收藏状态（TODO: 从收藏服务获取）
    var isFavorite by remember { mutableStateOf(false) }
    
    // 从歌曲提取信息
    val coverUrl = playerState.currentSong?.coverUrl
    val title = playerState.currentSong?.name ?: stringResource(Res.string.player_no_song)
    val artists = playerState.currentSong?.artists ?: emptyList()
    val artistText = artists.joinToString(" / ") { it.name }
    
    // 内容颜色（在模糊背景上需要更高对比度）
    val contentColor = MaterialTheme.colorScheme.onSurface
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 模糊背景
        BlurredBackground(
            imageUrl = coverUrl,
            modifier = Modifier.fillMaxSize()
        )
        
        // 主内容
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            if (isCompact) {
                CompactPlayerLayout(
                    coverUrl = coverUrl,
                    title = title,
                    artists = artists,
                    isPlaying = playerState.isPlaying,
                    position = playerState.position,
                    duration = playerState.duration,
                    repeatMode = playerState.repeatMode,
                    shuffleEnabled = playerState.shuffleEnabled,
                    isFavorite = isFavorite,
                    lyrics = lyrics,
                    currentLyricIndex = currentLyricIndex,
                    isImmersive = isImmersive,
                    onNavigateBack = onNavigateBack,
                    onMoreClick = { /* TODO */ },
                    onFavoriteClick = { isFavorite = !isFavorite },
                    onTitleClick = onNavigateToSongDetail,
                    onArtistClick = onNavigateToArtist,
                    onSeek = { PlayerService.seekTo(it) },
                    onPlayPause = { PlayerService.playPause() },
                    onPrevious = { PlayerService.previous() },
                    onNext = { PlayerService.next() },
                    onRepeatModeChange = {
                        val newMode = when (playerState.repeatMode) {
                            RepeatMode.OFF -> RepeatMode.ALL
                            RepeatMode.ALL -> RepeatMode.ONE
                            RepeatMode.ONE -> RepeatMode.OFF
                        }
                        PlayerService.setRepeatMode(newMode)
                    },
                    onShuffleToggle = { PlayerService.setShuffleEnabled(!playerState.shuffleEnabled) },
                    onTimerClick = { /* TODO */ },
                    onQueueClick = onNavigateToQueue,
                    onToggleImmersive = { isImmersive = !isImmersive },
                    contentColor = contentColor
                )
            } else {
                ExpandedPlayerLayout(
                    coverUrl = coverUrl,
                    title = title,
                    artists = artists,
                    isPlaying = playerState.isPlaying,
                    position = playerState.position,
                    duration = playerState.duration,
                    repeatMode = playerState.repeatMode,
                    shuffleEnabled = playerState.shuffleEnabled,
                    isFavorite = isFavorite,
                    lyrics = lyrics,
                    currentLyricIndex = currentLyricIndex,
                    isImmersive = isImmersive,
                    onNavigateBack = onNavigateBack,
                    onMoreClick = { /* TODO */ },
                    onFavoriteClick = { isFavorite = !isFavorite },
                    onTitleClick = onNavigateToSongDetail,
                    onArtistClick = onNavigateToArtist,
                    onSeek = { PlayerService.seekTo(it) },
                    onPlayPause = { PlayerService.playPause() },
                    onPrevious = { PlayerService.previous() },
                    onNext = { PlayerService.next() },
                    onRepeatModeChange = {
                        val newMode = when (playerState.repeatMode) {
                            RepeatMode.OFF -> RepeatMode.ALL
                            RepeatMode.ALL -> RepeatMode.ONE
                            RepeatMode.ONE -> RepeatMode.OFF
                        }
                        PlayerService.setRepeatMode(newMode)
                    },
                    onShuffleToggle = { PlayerService.setShuffleEnabled(!playerState.shuffleEnabled) },
                    onTimerClick = { /* TODO */ },
                    onQueueClick = onNavigateToQueue,
                    onToggleImmersive = { isImmersive = !isImmersive },
                    contentColor = contentColor
                )
            }
        }
    }
}

/**
 * 紧凑型布局（手机/平板竖屏）
 * 
 * - 主界面：顶栏 → 封面 → 歌曲信息 → 进度条 → 播放控制 → 工具栏
 * - 歌词界面：右滑显示，顶栏显示歌曲信息
 * - 沉浸模式：隐藏所有控制，仅显示歌词
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactPlayerLayout(
    coverUrl: String?,
    title: String,
    artists: List<MistyArtist>,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    repeatMode: RepeatMode,
    shuffleEnabled: Boolean,
    isFavorite: Boolean,
    lyrics: List<LyricLine>,
    currentLyricIndex: Int,
    isImmersive: Boolean,
    onNavigateBack: () -> Unit,
    onMoreClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onTitleClick: () -> Unit,
    onArtistClick: (MistyArtist) -> Unit,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRepeatModeChange: () -> Unit,
    onShuffleToggle: () -> Unit,
    onTimerClick: () -> Unit,
    onQueueClick: () -> Unit,
    onToggleImmersive: () -> Unit,
    contentColor: Color
) {
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val isOnLyricsPage = pagerState.currentPage == 1
    val artistText = artists.joinToString(" / ") { it.name }
    
    // 使用 AnimatedContent 实现沉浸模式的平滑过渡
    AnimatedContent(
        targetState = isImmersive,
        transitionSpec = {
            // 淡入淡出 + 轻微滑动效果
            (fadeIn(tween(400)) + slideInVertically(tween(400)) { -it / 10 }) togetherWith
            (fadeOut(tween(300)) + slideOutVertically(tween(300)) { it / 10 })
        },
        modifier = Modifier.fillMaxSize(),
        label = "immersiveModeTransition"
    ) { immersive ->
        if (immersive) {
            // 沉浸模式：全屏歌词
            PlayerLyrics(
                lyrics = lyrics,
                currentIndex = currentLyricIndex,
                onToggleImmersive = onToggleImmersive,
                modifier = Modifier.fillMaxSize(),
                contentColor = contentColor,
                isImmersive = true
            )
        } else {
            // 常规布局
            Column(modifier = Modifier.fillMaxSize()) {
                // 顶部栏
                PlayerTopBar(
                    mode = if (isOnLyricsPage) TopBarMode.SONG_INFO else TopBarMode.STANDARD,
                    songTitle = title,
                    artistName = artistText,
                    onNavigateBack = onNavigateBack,
                    onMoreClick = onMoreClick,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    contentColor = contentColor
                )
                
                // 中间内容区：可滑动
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    when (page) {
                        0 -> {
                            // 主页面：封面
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                PlayerCover(
                                    coverUrl = coverUrl,
                                    isPlaying = isPlaying,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp)
                                )
                            }
                        }
                        1 -> {
                            // 歌词页面
                            PlayerLyrics(
                                lyrics = lyrics,
                                currentIndex = currentLyricIndex,
                                onToggleImmersive = onToggleImmersive,
                                modifier = Modifier.fillMaxSize(),
                                contentColor = contentColor,
                                isImmersive = false
                            )
                        }
                    }
                }
                
                // 页面指示器
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(2) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(if (pagerState.currentPage == index) 6.dp else 4.dp)
                                .width(if (pagerState.currentPage == index) 20.dp else 4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        contentColor.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
                
                // 底部控制区
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 歌曲信息（仅在主页面显示）
                    AnimatedVisibility(
                        visible = !isOnLyricsPage,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        PlayerSongInfo(
                            title = title,
                            artists = artists,
                            onTitleClick = onTitleClick,
                            onArtistClick = onArtistClick,
                            contentColor = contentColor,
                            centered = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 进度条
                    PlayerProgressBar(
                        position = position,
                        duration = duration,
                        onSeek = onSeek,
                        contentColor = contentColor
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 播放控制
                    PlayerControls(
                        isPlaying = isPlaying,
                        onPlayPause = onPlayPause,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        contentColor = contentColor
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 工具栏（含收藏按钮）
                    PlayerToolbar(
                        isFavorite = isFavorite,
                        shuffleEnabled = shuffleEnabled,
                        repeatMode = repeatMode,
                        onFavoriteClick = onFavoriteClick,
                        onShuffleToggle = onShuffleToggle,
                        onRepeatModeChange = onRepeatModeChange,
                        onTimerClick = onTimerClick,
                        onQueueClick = onQueueClick,
                        onMoreClick = onMoreClick,
                        contentColor = contentColor
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

/**
 * 扩展型布局（桌面/平板横屏）
 * 
 * 结构：
 * - 顶栏
 * - 内容区：左侧（封面+信息）| 右侧（歌词）
 * - 底部控制区：进度条 + 三段式播放控制
 */
@Composable
private fun ExpandedPlayerLayout(
    coverUrl: String?,
    title: String,
    artists: List<MistyArtist>,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    repeatMode: RepeatMode,
    shuffleEnabled: Boolean,
    isFavorite: Boolean,
    lyrics: List<LyricLine>,
    currentLyricIndex: Int,
    isImmersive: Boolean,
    onNavigateBack: () -> Unit,
    onMoreClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onTitleClick: () -> Unit,
    onArtistClick: (MistyArtist) -> Unit,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRepeatModeChange: () -> Unit,
    onShuffleToggle: () -> Unit,
    onTimerClick: () -> Unit,
    onQueueClick: () -> Unit,
    onToggleImmersive: () -> Unit,
    contentColor: Color
) {
    val interactionSource = remember { MutableInteractionSource() }
    val artistText = artists.joinToString(" / ") { it.name }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶栏
        PlayerTopBar(
            mode = TopBarMode.STANDARD,
            songTitle = title,
            artistName = artistText,
            onNavigateBack = onNavigateBack,
            onMoreClick = onMoreClick,
            modifier = Modifier.padding(horizontal = 16.dp),
            contentColor = contentColor
        )
        
        // 内容区：左侧封面+信息，右侧歌词
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // 左侧：封面 + 歌曲信息
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 封面
                PlayerCover(
                    coverUrl = coverUrl,
                    isPlaying = isPlaying,
                    modifier = Modifier.widthIn(max = 300.dp)
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // 歌曲信息（可点击）
                PlayerSongInfo(
                    title = title,
                    artists = artists,
                    onTitleClick = onTitleClick,
                    onArtistClick = onArtistClick,
                    modifier = Modifier.widthIn(max = 300.dp),
                    contentColor = contentColor,
                    centered = true
                )
            }
            
            // 分隔线
            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 24.dp),
                color = contentColor.copy(alpha = 0.1f)
            )
            
            // 右侧：歌词
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                PlayerLyrics(
                    lyrics = lyrics,
                    currentIndex = currentLyricIndex,
                    onToggleImmersive = onToggleImmersive,
                    modifier = Modifier.fillMaxSize(),
                    contentColor = contentColor,
                    isImmersive = isImmersive
                )
            }
        }
        
        // 底部控制区（沉浸模式下隐藏）
        AnimatedVisibility(
            visible = !isImmersive,
            enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
            exit = slideOutVertically(tween(300)) { it } + fadeOut(tween(300))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 进度条（全宽细线条）
                PlayerProgressBarExpanded(
                    position = position,
                    duration = duration,
                    onSeek = onSeek,
                    modifier = Modifier.fillMaxWidth(),
                    contentColor = contentColor
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 三段式播放控制
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧工具栏：收藏、随机、循环
                    PlayerToolbarLeft(
                        isFavorite = isFavorite,
                        shuffleEnabled = shuffleEnabled,
                        repeatMode = repeatMode,
                        onFavoriteClick = onFavoriteClick,
                        onShuffleToggle = onShuffleToggle,
                        onRepeatModeChange = onRepeatModeChange,
                        contentColor = contentColor
                    )
                    
                    // 中间播放控制：上一曲、播放、下一曲
                    PlayerControlsExpanded(
                        isPlaying = isPlaying,
                        onPlayPause = onPlayPause,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        contentColor = contentColor
                    )
                    
                    // 右侧工具栏：队列、定时、更多
                    PlayerToolbarRight(
                        onQueueClick = onQueueClick,
                        onTimerClick = onTimerClick,
                        onMoreClick = onMoreClick,
                        contentColor = contentColor
                    )
                }
            }
        }
        
        // 沉浸模式提示（点击任意位置退出）
        if (isImmersive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onToggleImmersive
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // 淡入淡出的提示
            }
        }
    }
}
