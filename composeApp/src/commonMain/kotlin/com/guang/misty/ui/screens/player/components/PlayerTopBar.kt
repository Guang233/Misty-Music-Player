package com.guang.misty.ui.screens.player.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import misty.composeapp.generated.resources.Res
import misty.composeapp.generated.resources.playing_title
import org.jetbrains.compose.resources.stringResource

/**
 * 顶栏显示模式
 */
enum class TopBarMode {
    /** 标准模式：显示 "正在播放" 标题 */
    STANDARD,
    /** 歌曲信息模式：显示歌曲名和艺术家（用于歌词页面） */
    SONG_INFO
}

/**
 * 播放器顶栏组件
 * 
 * 支持两种显示模式：
 * - STANDARD: 显示 "正在播放" 标题
 * - SONG_INFO: 显示歌曲名和艺术家名（歌词页使用）
 */
@Composable
fun PlayerTopBar(
    mode: TopBarMode,
    songTitle: String = "",
    artistName: String = "",
    onNavigateBack: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 返回按钮
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "返回",
                modifier = Modifier.size(28.dp),
                tint = contentColor
            )
        }
        
        // 中间标题区域
        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            modifier = Modifier.weight(1f),
            label = "topBarContent"
        ) { currentMode ->
            when (currentMode) {
                TopBarMode.STANDARD -> {
                    Text(
                        text = stringResource(Res.string.playing_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = contentColor,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TopBarMode.SONG_INFO -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = songTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (artistName.isNotEmpty()) {
                            Text(
                                text = artistName,
                                style = MaterialTheme.typography.bodySmall,
                                color = contentColor.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        
        // 更多按钮
        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "更多选项",
                tint = contentColor
            )
        }
    }
}
