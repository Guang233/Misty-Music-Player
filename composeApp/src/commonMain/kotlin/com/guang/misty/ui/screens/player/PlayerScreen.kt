package com.guang.misty.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.guang.misty.model.LyricDisplayMode
import com.guang.misty.model.LyricLine
import com.guang.misty.model.MistyArtist
import com.guang.misty.player.PlayerService
import com.guang.misty.player.RepeatMode
import com.guang.misty.service.LyricService
import com.guang.misty.ui.screens.player.components.BlurredBackground
import com.guang.misty.ui.screens.player.components.PlayerControls
import com.guang.misty.ui.screens.player.components.PlayerControlsExpanded
import com.guang.misty.ui.screens.player.components.PlayerCover
import com.guang.misty.ui.screens.player.components.PlayerLyrics
import com.guang.misty.ui.screens.player.components.PlayerProgressBar
import com.guang.misty.ui.screens.player.components.PlayerProgressBarExpanded
import com.guang.misty.ui.screens.player.components.PlayerSongInfo
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
    
    // 过渡动画状态（用于防止歌词跳动）
    var isTransitioning by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // 带过渡状态的沉浸模式切换
    val onToggleImmersiveWithTransition: () -> Unit = {
        isTransitioning = true
        isImmersive = !isImmersive
        // 动画完成后恢复（300ms 的动画 + 100ms 缓冲）
        coroutineScope.launch {
            delay(400)
            isTransitioning = false
        }
    }
    
    // 从歌词服务获取状态
    val lyrics by LyricService.lyrics.collectAsState()
    val currentLyricIndex by LyricService.currentLineIndex.collectAsState()
    val displayMode by LyricService.displayMode.collectAsState()
    val hasTranslation by LyricService.hasTranslation.collectAsState()
    val hasRomanization by LyricService.hasRomanization.collectAsState()
    
    // 当歌曲变化时加载歌词
    LaunchedEffect(playerState.currentSong?.globalId) {
        val song = playerState.currentSong
        if (song != null) {
            // 从歌曲的 source 获取插件 ID
            LyricService.loadLyrics(song, song.source)
        } else {
            LyricService.clear()
        }
    }
    
    // 更新播放位置到歌词服务
    LaunchedEffect(playerState.position) {
        LyricService.updatePosition(playerState.position)
    }
    
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
                    bufferedPosition = playerState.bufferedPosition,
                    repeatMode = playerState.repeatMode,
                    shuffleEnabled = playerState.shuffleEnabled,
                    isFavorite = isFavorite,
                    lyrics = lyrics,
                    currentLyricIndex = currentLyricIndex,
                    displayMode = displayMode,
                    hasTranslation = hasTranslation,
                    hasRomanization = hasRomanization,
                    isImmersive = isImmersive,
                    isTransitioning = isTransitioning,
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
                    onToggleImmersive = onToggleImmersiveWithTransition,
                    onLyricModeChange = { LyricService.setDisplayMode(it) },
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
                    bufferedPosition = playerState.bufferedPosition,
                    repeatMode = playerState.repeatMode,
                    shuffleEnabled = playerState.shuffleEnabled,
                    isFavorite = isFavorite,
                    lyrics = lyrics,
                    currentLyricIndex = currentLyricIndex,
                    displayMode = displayMode,
                    hasTranslation = hasTranslation,
                    hasRomanization = hasRomanization,
                    isImmersive = isImmersive,
                    isTransitioning = isTransitioning,
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
                    onToggleImmersive = onToggleImmersiveWithTransition,
                    onLyricModeChange = { LyricService.setDisplayMode(it) },
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
 * - 沉浸模式：仅在歌词页生效，无缝隐藏顶栏和底部控制区
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
    bufferedPosition: Long,
    repeatMode: RepeatMode,
    shuffleEnabled: Boolean,
    isFavorite: Boolean,
    lyrics: List<LyricLine>,
    currentLyricIndex: Int,
    displayMode: LyricDisplayMode,
    hasTranslation: Boolean,
    hasRomanization: Boolean,
    isImmersive: Boolean,
    isTransitioning: Boolean,
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
    onLyricModeChange: (LyricDisplayMode) -> Unit,
    contentColor: Color
) {
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val isOnLyricsPage = pagerState.currentPage == 1
    val artistText = artists.joinToString(" / ") { it.name }
    
    // 沉浸模式仅在歌词页生效
    val shouldHideControls = isImmersive && isOnLyricsPage
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部栏 - 沉浸模式+歌词页时隐藏（带高度动画）
        AnimatedVisibility(
            visible = !shouldHideControls,
            enter = fadeIn(tween(300)) + expandVertically(tween(300)),
            exit = fadeOut(tween(300)) + shrinkVertically(tween(300))
        ) {
            PlayerTopBar(
                mode = if (isOnLyricsPage) TopBarMode.SONG_INFO else TopBarMode.STANDARD,
                songTitle = title,
                artistName = artistText,
                onNavigateBack = onNavigateBack,
                onMoreClick = onMoreClick,
                modifier = Modifier.padding(horizontal = 12.dp),
                contentColor = contentColor
                // 小屏幕不显示沉浸模式按钮，用户通过长按歌词切换沉浸模式
            )
        }
        
        // 中间内容区：始终显示，不参与沉浸模式切换
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> {
                    // 主页面：封面 + 歌曲信息
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        PlayerCover(
                            coverUrl = coverUrl,
                            isPlaying = isPlaying,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // 歌曲信息放在封面下方
                        PlayerSongInfo(
                            title = title,
                            artists = artists,
                            onTitleClick = onTitleClick,
                            onArtistClick = onArtistClick,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            contentColor = contentColor,
                            centered = true,
                            isCompact = true
                        )
                    }
                }
                1 -> {
                    // 歌词页面（小屏幕：长按切换沉浸模式）
                    PlayerLyrics(
                        lyrics = lyrics,
                        currentIndex = currentLyricIndex,
                        currentPosition = position,
                        displayMode = displayMode,
                        onSeekTo = onSeek,
                        onToggleImmersive = onToggleImmersive,
                        modifier = Modifier.fillMaxSize(),
                        contentColor = contentColor,
                        isImmersive = isImmersive,
                        isTransitioning = isTransitioning,
                        onLongPress = onToggleImmersive, // 长按切换沉浸模式
                        hasTranslation = hasTranslation,
                        hasRomanization = hasRomanization,
                        onLyricModeChange = onLyricModeChange
                    )
                }
            }
        }
        
        // 页面指示器 - 沉浸模式+歌词页时隐藏（带高度动画）
        AnimatedVisibility(
            visible = !shouldHideControls,
            enter = fadeIn(tween(300)) + expandVertically(tween(300)),
            exit = fadeOut(tween(300)) + shrinkVertically(tween(300))
        ) {
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
        }
        
        // 底部控制区 - 沉浸模式+歌词页时隐藏（带高度动画）
        AnimatedVisibility(
            visible = !shouldHideControls,
            enter = fadeIn(tween(300)) + expandVertically(tween(300)),
            exit = fadeOut(tween(300)) + shrinkVertically(tween(300))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 进度条
                PlayerProgressBar(
                    position = position,
                    duration = duration,
                    onSeek = onSeek,
                    bufferedPosition = bufferedPosition,
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
    bufferedPosition: Long,
    repeatMode: RepeatMode,
    shuffleEnabled: Boolean,
    isFavorite: Boolean,
    lyrics: List<LyricLine>,
    currentLyricIndex: Int,
    displayMode: LyricDisplayMode,
    hasTranslation: Boolean,
    hasRomanization: Boolean,
    isImmersive: Boolean,
    isTransitioning: Boolean,
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
    onLyricModeChange: (LyricDisplayMode) -> Unit,
    contentColor: Color
) {
    val artistText = artists.joinToString(" / ") { it.name }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶栏 - 沉浸模式时隐藏（带高度动画）
        AnimatedVisibility(
            visible = !isImmersive,
            enter = fadeIn(tween(300)) + expandVertically(tween(300)),
            exit = fadeOut(tween(300)) + shrinkVertically(tween(300))
        ) {
            PlayerTopBar(
                mode = TopBarMode.STANDARD,
                songTitle = title,
                artistName = artistText,
                onNavigateBack = onNavigateBack,
                onMoreClick = onMoreClick,
                modifier = Modifier.padding(horizontal = 16.dp),
                contentColor = contentColor,
                isImmersive = isImmersive,
                onToggleImmersive = onToggleImmersive,
                showImmersiveButton = true // 大屏幕始终显示沉浸模式按钮
            )
        }
        
        // 内容区：左侧封面+信息，右侧歌词
        // 使用 animateContentSize 让沉浸模式切换时尺寸变化更平滑
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                )
        ) {
            // 左侧：封面 + 歌曲信息（点击空白区域切换沉浸模式）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleImmersive
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
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
                        modifier = Modifier.widthIn(max = 400.dp),
                        contentColor = contentColor,
                        centered = true,
                        isCompact = false
                    )
                }
            }
            
            // 分隔线
            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 24.dp),
                color = contentColor.copy(alpha = 0.1f)
            )
            
            // 右侧：歌词（使用 animateContentSize 让沉浸模式切换时尺寸变化更平滑）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .animateContentSize(
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    )
            ) {
                PlayerLyrics(
                    lyrics = lyrics,
                    currentIndex = currentLyricIndex,
                    currentPosition = position,
                    displayMode = displayMode,
                    onSeekTo = onSeek,
                    onToggleImmersive = onToggleImmersive,
                    modifier = Modifier.fillMaxSize(),
                    contentColor = contentColor,
                    isImmersive = isImmersive,
                    isTransitioning = isTransitioning,
                    hasTranslation = hasTranslation,
                    hasRomanization = hasRomanization,
                    onLyricModeChange = onLyricModeChange
                )
            }
        }
        
        // 底部控制区（沉浸模式下隐藏，带高度动画，点击空白区域退出播放器）
        AnimatedVisibility(
            visible = !isImmersive,
            enter = expandVertically(tween(300)) + fadeIn(tween(300)),
            exit = shrinkVertically(tween(300)) + fadeOut(tween(300))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onNavigateBack
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 进度条（全宽细线条）
                PlayerProgressBarExpanded(
                    position = position,
                    duration = duration,
                    onSeek = onSeek,
                    modifier = Modifier.fillMaxWidth(),
                    bufferedPosition = bufferedPosition,
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
        
    }
}
