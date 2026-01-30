package com.guang.misty.ui.screens.player

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.guang.misty.player.PlayerService
import com.guang.misty.player.RepeatMode
import com.guang.misty.ui.theme.LocalWindowSizeClass
import com.guang.misty.ui.theme.WindowWidthSizeClass
import misty.composeapp.generated.resources.Res
import misty.composeapp.generated.resources.player_next
import misty.composeapp.generated.resources.player_no_song
import misty.composeapp.generated.resources.player_pause
import misty.composeapp.generated.resources.player_play
import misty.composeapp.generated.resources.player_previous
import misty.composeapp.generated.resources.player_queue
import misty.composeapp.generated.resources.player_repeat
import misty.composeapp.generated.resources.player_repeat_one
import misty.composeapp.generated.resources.player_shuffle
import misty.composeapp.generated.resources.playing_title
import org.jetbrains.compose.resources.stringResource

/**
 * 全屏播放器界面 - MD3 Expressive 风格
 * 
 * 响应式布局：
 * - Compact: 左右滑动切换封面/歌词
 * - Medium/Expanded: 左侧播放控制 + 右侧歌词
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQueue: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val isExpanded = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    
    // 获取播放状态
    val playerState by PlayerService.state.collectAsState()
    
    // TODO: 从歌词服务获取
    val lyrics = remember {
        listOf(
            LyricLine(0, "暂无歌词"),
            LyricLine(1000, ""),
            LyricLine(2000, "歌词加载中..."),
        )
    }
    val currentLyricIndex = 0
    
    // 背景渐变色
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.12f),
                        surfaceColor
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        if (isExpanded) {
            // 大屏幕布局：左侧控制 + 右侧歌词
            ExpandedPlayerLayout(
                coverUrl = playerState.currentSong?.coverUrl,
                title = playerState.currentSong?.name ?: stringResource(Res.string.player_no_song),
                artist = playerState.currentSong?.artists?.joinToString(", ") { it.name } ?: "",
                isPlaying = playerState.isPlaying,
                position = playerState.position,
                duration = playerState.duration,
                repeatMode = playerState.repeatMode,
                shuffleEnabled = playerState.shuffleEnabled,
                lyrics = lyrics,
                currentLyricIndex = currentLyricIndex,
                onNavigateBack = onNavigateBack,
                onMoreClick = { /* TODO */ },
                onFavoriteClick = { /* TODO */ },
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
                onQueueClick = onNavigateToQueue
            )
        } else {
            // 手机布局：左右滑动切换
            CompactPlayerLayout(
                coverUrl = playerState.currentSong?.coverUrl,
                title = playerState.currentSong?.name ?: stringResource(Res.string.player_no_song),
                artist = playerState.currentSong?.artists?.joinToString(", ") { it.name } ?: "",
                isPlaying = playerState.isPlaying,
                position = playerState.position,
                duration = playerState.duration,
                repeatMode = playerState.repeatMode,
                shuffleEnabled = playerState.shuffleEnabled,
                lyrics = lyrics,
                currentLyricIndex = currentLyricIndex,
                onNavigateBack = onNavigateBack,
                onMoreClick = { /* TODO */ },
                onFavoriteClick = { /* TODO */ },
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
                onQueueClick = onNavigateToQueue
            )
        }
    }
}

/**
 * 歌词行数据
 */
data class LyricLine(
    val timeMs: Long,
    val text: String
)

/**
 * 紧凑型布局（手机）- 左右滑动切换封面/歌词
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactPlayerLayout(
    coverUrl: String?,
    title: String,
    artist: String,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    repeatMode: RepeatMode,
    shuffleEnabled: Boolean,
    lyrics: List<LyricLine>,
    currentLyricIndex: Int,
    onNavigateBack: () -> Unit,
    onMoreClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRepeatModeChange: () -> Unit,
    onShuffleToggle: () -> Unit,
    onQueueClick: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部栏
        TopBar(
            onNavigateBack = onNavigateBack,
            onMoreClick = onMoreClick,
            modifier = Modifier.padding(horizontal = 16.dp)
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
                    // 封面页
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AlbumCover(
                            coverUrl = coverUrl,
                            isPlaying = isPlaying,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                        )
                    }
                }
                1 -> {
                    // 歌词页
                    LyricsView(
                        lyrics = lyrics,
                        currentIndex = currentLyricIndex,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        
        // 页面指示器
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(2) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outlineVariant
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
            // 歌曲信息
            SongInfo(
                title = title,
                artist = artist,
                isFavorite = false,
                onFavoriteClick = onFavoriteClick
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 进度条
            ProgressBar(
                position = position,
                duration = duration,
                onSeek = onSeek
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 播放控制
            PlaybackControls(
                isPlaying = isPlaying,
                repeatMode = repeatMode,
                shuffleEnabled = shuffleEnabled,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onRepeatModeChange = onRepeatModeChange,
                onShuffleToggle = onShuffleToggle
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 底部附加控制
            BottomControls(onQueueClick = onQueueClick)
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * 扩展型布局（平板/桌面）- 左侧控制 + 右侧歌词
 */
@Composable
private fun ExpandedPlayerLayout(
    coverUrl: String?,
    title: String,
    artist: String,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    repeatMode: RepeatMode,
    shuffleEnabled: Boolean,
    lyrics: List<LyricLine>,
    currentLyricIndex: Int,
    onNavigateBack: () -> Unit,
    onMoreClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRepeatModeChange: () -> Unit,
    onShuffleToggle: () -> Unit,
    onQueueClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // 左侧：播放控制区
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部栏
            TopBar(
                onNavigateBack = onNavigateBack,
                onMoreClick = onMoreClick
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 专辑封面
            AlbumCover(
                coverUrl = coverUrl,
                isPlaying = isPlaying,
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .aspectRatio(1f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 歌曲信息
            SongInfoCentered(
                title = title,
                artist = artist,
                isFavorite = false,
                onFavoriteClick = onFavoriteClick
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 进度条
            ProgressBar(
                position = position,
                duration = duration,
                onSeek = onSeek,
                modifier = Modifier.widthIn(max = 400.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 播放控制
            PlaybackControlsExpanded(
                isPlaying = isPlaying,
                repeatMode = repeatMode,
                shuffleEnabled = shuffleEnabled,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onRepeatModeChange = onRepeatModeChange,
                onShuffleToggle = onShuffleToggle
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 底部附加控制
            BottomControls(onQueueClick = onQueueClick)
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 分隔线
        HorizontalDivider(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        
        // 右侧：歌词区
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 歌词标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lyrics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "歌词",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // 歌词内容
                LyricsView(
                    lyrics = lyrics,
                    currentIndex = currentLyricIndex,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 顶部栏
 */
@Composable
private fun TopBar(
    onNavigateBack: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Text(
            text = stringResource(Res.string.playing_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null
            )
        }
    }
}

/**
 * 专辑封面 - MD3 Expressive 风格
 */
@Composable
private fun AlbumCover(
    coverUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "coverScale"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            shadowElevation = if (isPlaying) 16.dp else 4.dp,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // 默认占位符
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 歌词视图
 */
@Composable
private fun LyricsView(
    lyrics: List<LyricLine>,
    currentIndex: Int,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // 自动滚动到当前歌词
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0 && currentIndex < lyrics.size) {
            listState.animateScrollToItem(
                index = currentIndex,
                scrollOffset = -200 // 偏移量，让当前歌词居中偏上
            )
        }
    }
    
    if (lyrics.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lyrics,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "暂无歌词",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 顶部间距
            item { Spacer(modifier = Modifier.height(80.dp)) }
            
            itemsIndexed(lyrics) { index, line ->
                val isCurrent = index == currentIndex
                val alpha = when {
                    isCurrent -> 1f
                    kotlin.math.abs(index - currentIndex) == 1 -> 0.6f
                    kotlin.math.abs(index - currentIndex) == 2 -> 0.4f
                    else -> 0.25f
                }
                
                Text(
                    text = line.text.ifEmpty { "..." },
                    style = if (isCurrent) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.bodyLarge
                    },
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(alpha)
                )
            }
            
            // 底部间距
            item { Spacer(modifier = Modifier.height(120.dp)) }
        }
    }
}

/**
 * 歌曲信息（紧凑型）
 */
@Composable
private fun SongInfo(
    title: String,
    artist: String,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (artist.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 收藏按钮
        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 歌曲信息（居中）
 */
@Composable
private fun SongInfoCentered(
    title: String,
    artist: String,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (artist.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 进度条
 */
@Composable
private fun ProgressBar(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }
    
    val progress = if (duration > 0) {
        if (isDragging) dragPosition else position.toFloat() / duration
    } else 0f
    
    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = progress,
            onValueChange = { value ->
                isDragging = true
                dragPosition = value
            },
            onValueChangeFinished = {
                onSeek((dragPosition * duration).toLong())
                isDragging = false
            },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(if (isDragging) (dragPosition * duration).toLong() else position),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 播放控制按钮（紧凑型）
 */
@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    repeatMode: RepeatMode,
    shuffleEnabled: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRepeatModeChange: () -> Unit,
    onShuffleToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 随机播放
        IconButton(
            onClick = onShuffleToggle,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = stringResource(Res.string.player_shuffle),
                tint = if (shuffleEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        
        // 上一曲
        FilledTonalIconButton(
            onClick = onPrevious,
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = stringResource(Res.string.player_previous),
                modifier = Modifier.size(26.dp)
            )
        }
        
        // 播放/暂停 - 大按钮
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) {
                    stringResource(Res.string.player_pause)
                } else {
                    stringResource(Res.string.player_play)
                },
                modifier = Modifier.size(36.dp)
            )
        }
        
        // 下一曲
        FilledTonalIconButton(
            onClick = onNext,
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = stringResource(Res.string.player_next),
                modifier = Modifier.size(26.dp)
            )
        }
        
        // 循环模式
        IconButton(
            onClick = onRepeatModeChange,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = when (repeatMode) {
                    RepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                },
                contentDescription = when (repeatMode) {
                    RepeatMode.ONE -> stringResource(Res.string.player_repeat_one)
                    else -> stringResource(Res.string.player_repeat)
                },
                tint = if (repeatMode != RepeatMode.OFF) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * 播放控制按钮（扩展型）
 */
@Composable
private fun PlaybackControlsExpanded(
    isPlaying: Boolean,
    repeatMode: RepeatMode,
    shuffleEnabled: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRepeatModeChange: () -> Unit,
    onShuffleToggle: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 随机播放
        IconButton(
            onClick = onShuffleToggle,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = stringResource(Res.string.player_shuffle),
                modifier = Modifier.size(24.dp),
                tint = if (shuffleEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 上一曲
        FilledTonalIconButton(
            onClick = onPrevious,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = stringResource(Res.string.player_previous),
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // 播放/暂停 - 大按钮
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) {
                    stringResource(Res.string.player_pause)
                } else {
                    stringResource(Res.string.player_play)
                },
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // 下一曲
        FilledTonalIconButton(
            onClick = onNext,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = stringResource(Res.string.player_next),
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 循环模式
        IconButton(
            onClick = onRepeatModeChange,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = when (repeatMode) {
                    RepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                },
                contentDescription = when (repeatMode) {
                    RepeatMode.ONE -> stringResource(Res.string.player_repeat_one)
                    else -> stringResource(Res.string.player_repeat)
                },
                modifier = Modifier.size(24.dp),
                tint = if (repeatMode != RepeatMode.OFF) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * 底部附加控制
 */
@Composable
private fun BottomControls(
    onQueueClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        FilledTonalIconButton(
            onClick = onQueueClick,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = stringResource(Res.string.player_queue),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * 格式化时长
 */
private fun formatDuration(millis: Long): String {
    if (millis <= 0) return "0:00"
    
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
