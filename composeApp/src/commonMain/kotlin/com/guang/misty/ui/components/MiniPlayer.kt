package com.guang.misty.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guang.misty.ui.theme.LocalWindowSizeClass
import com.guang.misty.ui.theme.WindowWidthSizeClass
import misty.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * 迷你播放条 - MD3 Expressive 风格
 * 
 * 特点：
 * - 大圆角设计
 * - 更明显的进度指示
 * - 精致的动画效果
 * - 响应式布局
 * - 常驻显示，无歌曲时显示空状态
 * - 透明背景容器，只有内容区域有背景
 */
@Composable
fun MiniPlayer(
    currentSong: CurrentSongInfo?,
    isPlaying: Boolean,
    progress: Float,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit = {},
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    
    // 外层容器透明，内层 Surface 有圆角和背景
    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // 进度条 - 更粗更明显（无歌曲时显示为空轨道）
                LinearProgressIndicator(
                    progress = { if (currentSong != null) progress else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                
                AnimatedContent(
                    targetState = currentSong,
                    transitionSpec = {
                        fadeIn() + slideInVertically { it / 4 } togetherWith 
                        fadeOut() + slideOutVertically { -it / 4 }
                    },
                    label = "miniPlayerContent"
                ) { song ->
                    if (song != null) {
                        if (isExpanded) {
                            ExpandedMiniPlayer(
                                currentSong = song,
                                isPlaying = isPlaying,
                                progress = progress,
                                onPlayPauseClick = onPlayPauseClick,
                                onNextClick = onNextClick,
                                onPreviousClick = onPreviousClick,
                                onClick = onClick
                            )
                        } else {
                            CompactMiniPlayer(
                                currentSong = song,
                                isPlaying = isPlaying,
                                onPlayPauseClick = onPlayPauseClick,
                                onNextClick = onNextClick,
                                onClick = onClick
                            )
                        }
                    } else {
                        // 空状态
                        if (isExpanded) {
                            ExpandedEmptyMiniPlayer(onClick = onClick)
                        } else {
                            CompactEmptyMiniPlayer(onClick = onClick)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 紧凑版空状态（手机端）- MD3 Expressive 风格
 */
@Composable
private fun CompactEmptyMiniPlayer(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 封面占位 - Expressive 圆角
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // 空状态提示
        Text(
            text = stringResource(Res.string.player_no_song),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 扩展版空状态（桌面端）- MD3 Expressive 风格
 * 适配深色模式
 */
@Composable
private fun ExpandedEmptyMiniPlayer(
    onClick: () -> Unit
) {
    // 使用主题颜色确保深色模式适配
    val disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val disabledTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 左侧：封面占位和提示
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 封面占位 - Expressive 圆角
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = disabledContentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Text(
                text = stringResource(Res.string.player_no_song),
                style = MaterialTheme.typography.bodyLarge,
                color = disabledContentColor
            )
        }
        
        // 中间：空进度条
        Row(
            modifier = Modifier.weight(2f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "--:--",
                style = MaterialTheme.typography.labelMedium,
                color = disabledContentColor,
                modifier = Modifier.width(40.dp)
            )
            
            Slider(
                value = 0f,
                onValueChange = { },
                enabled = false,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    disabledThumbColor = disabledTrackColor,
                    disabledActiveTrackColor = disabledTrackColor,
                    disabledInactiveTrackColor = disabledTrackColor
                )
            )
            
            Text(
                text = "--:--",
                style = MaterialTheme.typography.labelMedium,
                color = disabledContentColor,
                modifier = Modifier.width(40.dp)
            )
        }
        
        // 右侧：禁用的播放控制按钮
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { }, enabled = false) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = null,
                    tint = disabledContentColor
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 使用 FilledTonalIconButton 替代 FilledIconButton，在深色模式下显示更好
            FilledTonalIconButton(
                onClick = { },
                enabled = false,
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    disabledContentColor = disabledContentColor
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(onClick = { }, enabled = false) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = null,
                    tint = disabledContentColor
                )
            }
        }
    }
}

/**
 * 紧凑版迷你播放条（手机端）- MD3 Expressive 风格
 */
@Composable
private fun CompactMiniPlayer(
    currentSong: CurrentSongInfo,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 封面 - Expressive 圆角
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // 歌曲信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = currentSong.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = currentSong.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // 播放控制按钮 - Expressive 风格
        FilledTonalIconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier.size(44.dp)
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    scaleIn(spring(stiffness = Spring.StiffnessMedium)) togetherWith 
                    scaleOut(spring(stiffness = Spring.StiffnessMedium))
                },
                label = "playPauseAnimation"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) {
                        stringResource(Res.string.player_pause)
                    } else {
                        stringResource(Res.string.player_play)
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        IconButton(onClick = onNextClick) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = stringResource(Res.string.player_next)
            )
        }
    }
}

/**
 * 扩展版迷你播放条（桌面端）- MD3 Expressive 风格
 */
@Composable
private fun ExpandedMiniPlayer(
    currentSong: CurrentSongInfo,
    isPlaying: Boolean,
    progress: Float,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 左侧：封面和歌曲信息
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 封面 - Expressive 圆角
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Column {
                Text(
                    text = currentSong.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentSong.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        // 中间：进度控制
        Row(
            modifier = Modifier.weight(2f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = formatDuration(currentSong.currentPosition),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(40.dp)
            )
            
            Slider(
                value = progress,
                onValueChange = { /* TODO: 拖动进度 */ },
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            )
            
            Text(
                text = formatDuration(currentSong.duration),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(40.dp)
            )
        }
        
        // 右侧：播放控制按钮
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 上一曲
            IconButton(onClick = onPreviousClick) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = stringResource(Res.string.player_previous)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 播放/暂停 - 突出显示
            FilledIconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = {
                        scaleIn(spring(stiffness = Spring.StiffnessMedium)) togetherWith 
                        scaleOut(spring(stiffness = Spring.StiffnessMedium))
                    },
                    label = "playPauseAnimation"
                ) { playing ->
                    Icon(
                        imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playing) {
                            stringResource(Res.string.player_pause)
                        } else {
                            stringResource(Res.string.player_play)
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 下一曲
            IconButton(onClick = onNextClick) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = stringResource(Res.string.player_next)
                )
            }
        }
    }
}

/**
 * 格式化时长
 */
private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/**
 * 当前播放歌曲信息
 */
data class CurrentSongInfo(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String? = null,
    val duration: Long = 0,
    val currentPosition: Long = 0
)
