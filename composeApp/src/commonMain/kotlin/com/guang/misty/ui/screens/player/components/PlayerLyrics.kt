package com.guang.misty.ui.screens.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * 歌词行数据
 */
data class LyricLine(
    val timeMs: Long,
    val text: String,
    val translation: String? = null  // 可选的翻译歌词
)

/**
 * 播放器歌词组件 - MD3 Expressive 风格
 * 
 * 特点：
 * - 自动滚动到当前歌词行
 * - 当前行高亮显示，周围行渐变透明
 * - 支持点击切换沉浸模式
 * - 支持显示翻译歌词
 * - 沉浸模式与普通模式字体大小一致
 */
@Composable
fun PlayerLyrics(
    lyrics: List<LyricLine>,
    currentIndex: Int,
    onToggleImmersive: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
    isImmersive: Boolean = false
) {
    val listState = rememberLazyListState()
    val interactionSource = remember { MutableInteractionSource() }
    
    // 自动滚动到当前歌词
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0 && currentIndex < lyrics.size) {
            listState.animateScrollToItem(
                index = currentIndex,
                scrollOffset = -200  // 偏移量，让当前歌词居中偏上
            )
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,  // 无点击效果
                onClick = onToggleImmersive
            )
    ) {
        if (lyrics.isEmpty()) {
            // 空状态
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lyrics,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = contentColor.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "暂无歌词",
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 顶部间距
                item { 
                    Spacer(modifier = Modifier.height(80.dp)) 
                }
                
                itemsIndexed(lyrics) { index, line ->
                    val isCurrent = index == currentIndex
                    val distance = abs(index - currentIndex)
                    
                    // 根据距离计算透明度
                    val alpha = when {
                        isCurrent -> 1f
                        distance == 1 -> 0.65f
                        distance == 2 -> 0.45f
                        distance == 3 -> 0.3f
                        else -> 0.2f
                    }
                    
                    LyricLineItem(
                        line = line,
                        isCurrent = isCurrent,
                        alpha = alpha,
                        contentColor = contentColor,
                        highlightColor = highlightColor
                    )
                }
                
                // 底部间距
                item { 
                    Spacer(modifier = Modifier.height(120.dp)) 
                }
            }
        }
    }
}

/**
 * 单行歌词显示
 * 
 * 字体大小固定，不受沉浸模式影响
 */
@Composable
private fun LyricLineItem(
    line: LyricLine,
    isCurrent: Boolean,
    alpha: Float,
    contentColor: Color,
    highlightColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 主歌词 - 固定字体大小
        Text(
            text = line.text.ifEmpty { "···" },
            style = if (isCurrent) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.bodyLarge
            },
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent) highlightColor else contentColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        // 翻译歌词（如果有）
        if (line.translation != null && line.translation.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = line.translation,
                style = if (isCurrent) {
                    MaterialTheme.typography.bodyLarge
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                color = if (isCurrent) {
                    highlightColor.copy(alpha = 0.8f)
                } else {
                    contentColor.copy(alpha = 0.7f)
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 沉浸模式歌词视图
 * 
 * 全屏显示，隐藏所有控制元素
 */
@Composable
fun ImmersiveLyricsView(
    lyrics: List<LyricLine>,
    currentIndex: Int,
    onExitImmersive: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    highlightColor: Color = MaterialTheme.colorScheme.primary
) {
    PlayerLyrics(
        lyrics = lyrics,
        currentIndex = currentIndex,
        onToggleImmersive = onExitImmersive,
        modifier = modifier,
        contentColor = contentColor,
        highlightColor = highlightColor,
        isImmersive = true
    )
}
