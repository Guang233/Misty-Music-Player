package com.guang.misty.ui.screens.player.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 播放器进度条组件 - MD3 Expressive 风格
 * 
 * 特点：
 * - 细线条设计（无 thumb）
 * - 支持点击和拖动调节进度
 * - 按住/悬停时进度条变粗动画
 * - 显示当前时间和总时长
 * - 自定义颜色适配背景
 */
@Composable
fun PlayerProgressBar(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    trackHeight: Dp = 4.dp,
    expandedTrackHeight: Dp = trackHeight * 2,
    showTimeLabels: Boolean = true
) {
    var isDragging by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }
    
    // PC 端悬停检测
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    // 计算是否应该显示放大状态
    val isExpanded = isDragging || isPressed || isHovered
    
    // 进度条高度动画
    val animatedTrackHeight by animateDpAsState(
        targetValue = if (isExpanded) expandedTrackHeight else trackHeight,
        animationSpec = tween(durationMillis = 150),
        label = "trackHeightAnimation"
    )
    
    // 计算目标进度值
    val targetProgress = if (duration > 0) {
        if (isDragging) dragPosition else position.toFloat() / duration
    } else 0f
    
    // 平滑进度动画（拖动时不使用动画，实时响应）
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(
            durationMillis = if (isDragging) 0 else 250
        ),
        label = "progressAnimation"
    )
    
    // 使用动画后的进度值（拖动时使用实时值）
    val progress = if (isDragging) dragPosition else animatedProgress
    
    val displayPosition = if (isDragging) {
        (dragPosition * duration).toLong()
    } else {
        position
    }
    
    // 使用主题色作为进度条颜色
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = contentColor.copy(alpha = 0.3f)
    
    Column(modifier = modifier.fillMaxWidth()) {
        // 自定义细进度条（无 thumb）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(expandedTrackHeight + 16.dp) // 增加点击区域
                .hoverable(interactionSource) // PC 端悬停检测
                .pointerInput(duration) {
                    // 检测按下状态（移动端）
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        
                        // 等待释放
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.all { !it.pressed }) {
                                    break
                                }
                            }
                        } finally {
                            isPressed = false
                        }
                    }
                }
                .pointerInput(duration) {
                    detectTapGestures { offset ->
                        if (duration > 0) {
                            val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                            onSeek((newProgress * duration).toLong())
                        }
                    }
                }
                .pointerInput(duration) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragPosition = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            if (duration > 0) {
                                onSeek((dragPosition * duration).toLong())
                            }
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            dragPosition = (dragPosition + dragAmount / size.width).coerceIn(0f, 1f)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animatedTrackHeight)
            ) {
                val trackHeightPx = animatedTrackHeight.toPx()
                val cornerRadius = CornerRadius(trackHeightPx / 2, trackHeightPx / 2)
                
                // 绘制背景轨道
                drawRoundRect(
                    color = inactiveColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, trackHeightPx),
                    cornerRadius = cornerRadius
                )
                
                // 绘制已播放部分
                val progressWidth = size.width * progress
                if (progressWidth > 0) {
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset(0f, 0f),
                        size = Size(progressWidth, trackHeightPx),
                        cornerRadius = cornerRadius
                    )
                }
            }
        }
        
        // 时间标签
        if (showTimeLabels) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(displayPosition),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
                Text(
                    text = formatDuration(duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * 大屏幕用的全宽进度条（更细）
 */
@Composable
fun PlayerProgressBarExpanded(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    PlayerProgressBar(
        position = position,
        duration = duration,
        onSeek = onSeek,
        modifier = modifier,
        contentColor = contentColor,
        trackHeight = 3.dp,
        expandedTrackHeight = 6.dp,
        showTimeLabels = true
    )
}

/**
 * 格式化时长（毫秒 -> mm:ss）
 */
fun formatDuration(millis: Long): String {
    if (millis <= 0) return "0:00"
    
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
