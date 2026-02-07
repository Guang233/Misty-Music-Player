package com.guang.misty.ui.screens.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.guang.misty.player.RepeatMode
import misty.composeapp.generated.resources.Res
import misty.composeapp.generated.resources.action_more
import misty.composeapp.generated.resources.player_queue
import misty.composeapp.generated.resources.player_repeat
import misty.composeapp.generated.resources.player_repeat_one
import misty.composeapp.generated.resources.player_shuffle
import org.jetbrains.compose.resources.stringResource

/**
 * 播放器底部工具栏组件 - MD3 Expressive 风格
 * 
 * 小屏幕布局：[收藏] [随机] [循环] [定时] [队列] [更多]
 */
@Composable
fun PlayerToolbar(
    isFavorite: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onFavoriteClick: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatModeChange: () -> Unit,
    onTimerClick: () -> Unit,
    onQueueClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 收藏
        IconButton(
            onClick = onFavoriteClick,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "取消收藏" else "收藏",
                modifier = Modifier.size(22.dp),
                tint = if (isFavorite) {
                    MaterialTheme.colorScheme.error
                } else {
                    contentColor.copy(alpha = 0.7f)
                }
            )
        }
        
        // 随机播放
        IconButton(
            onClick = onShuffleToggle,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = stringResource(Res.string.player_shuffle),
                modifier = Modifier.size(22.dp),
                tint = if (shuffleEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    contentColor.copy(alpha = 0.7f)
                }
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
                modifier = Modifier.size(22.dp),
                tint = if (repeatMode != RepeatMode.OFF) {
                    MaterialTheme.colorScheme.primary
                } else {
                    contentColor.copy(alpha = 0.7f)
                }
            )
        }
        
        // 定时器
        IconButton(
            onClick = onTimerClick,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = "定时关闭",
                modifier = Modifier.size(22.dp),
                tint = contentColor.copy(alpha = 0.7f)
            )
        }
        
        // 播放队列
        IconButton(
            onClick = onQueueClick,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = stringResource(Res.string.player_queue),
                modifier = Modifier.size(22.dp),
                tint = contentColor.copy(alpha = 0.7f)
            )
        }
        
        // 更多选项
        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = stringResource(Res.string.action_more),
                modifier = Modifier.size(22.dp),
                tint = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 大屏幕左侧工具栏
 * 
 * 布局：[收藏] [随机] [循环]
 */
@Composable
fun PlayerToolbarLeft(
    isFavorite: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onFavoriteClick: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatModeChange: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 收藏
        IconButton(
            onClick = onFavoriteClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "取消收藏" else "收藏",
                modifier = Modifier.size(20.dp),
                tint = if (isFavorite) {
                    MaterialTheme.colorScheme.error
                } else {
                    contentColor.copy(alpha = 0.7f)
                }
            )
        }
        
        // 随机播放
        IconButton(
            onClick = onShuffleToggle,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = stringResource(Res.string.player_shuffle),
                modifier = Modifier.size(20.dp),
                tint = if (shuffleEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    contentColor.copy(alpha = 0.7f)
                }
            )
        }
        
        // 循环模式
        IconButton(
            onClick = onRepeatModeChange,
            modifier = Modifier.size(40.dp)
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
                modifier = Modifier.size(20.dp),
                tint = if (repeatMode != RepeatMode.OFF) {
                    MaterialTheme.colorScheme.primary
                } else {
                    contentColor.copy(alpha = 0.7f)
                }
            )
        }
    }
}

/**
 * 大屏幕右侧工具栏
 * 
 * 布局：[队列] [定时] [更多]
 */
@Composable
fun PlayerToolbarRight(
    onQueueClick: () -> Unit,
    onTimerClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 播放队列
        IconButton(
            onClick = onQueueClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = stringResource(Res.string.player_queue),
                modifier = Modifier.size(20.dp),
                tint = contentColor.copy(alpha = 0.7f)
            )
        }
        
        // 定时器
        IconButton(
            onClick = onTimerClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = "定时关闭",
                modifier = Modifier.size(20.dp),
                tint = contentColor.copy(alpha = 0.7f)
            )
        }
        
        // 更多选项
        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = stringResource(Res.string.action_more),
                modifier = Modifier.size(20.dp),
                tint = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}
