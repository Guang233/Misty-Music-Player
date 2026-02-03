package com.guang.misty.ui.screens.player.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guang.misty.model.LyricDisplayMode

/**
 * 歌词显示模式切换组件（精简版）
 * 
 * 三种模式：
 * - 原：仅原文
 * - 译：原文 + 译文
 * - 罗：原文 + 罗马音
 * 
 * 放置于歌词区域右下角，点击循环切换
 */
@Composable
fun LyricModeToggle(
    currentMode: LyricDisplayMode,
    hasTranslation: Boolean,
    hasRomanization: Boolean,
    onModeChange: (LyricDisplayMode) -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    // 构建可用模式列表
    val availableModes = buildList {
        add(LyricDisplayMode.ORIGINAL)
        if (hasTranslation) add(LyricDisplayMode.DUAL)
        if (hasRomanization) add(LyricDisplayMode.ROMANIZATION)
    }
    
    // 如果只有原文模式，不显示切换按钮
    if (availableModes.size <= 1) return
    
    // 当前模式的显示文字
    val modeText = when (currentMode) {
        LyricDisplayMode.ORIGINAL -> "原"
        LyricDisplayMode.DUAL -> "译"
        LyricDisplayMode.ROMANIZATION -> "罗"
        LyricDisplayMode.TRANSLATION -> "译" // 兼容旧模式，映射到双语
    }
    
    // 当前模式的完整说明
    val modeDescription = when (currentMode) {
        LyricDisplayMode.ORIGINAL -> "原文"
        LyricDisplayMode.DUAL -> "原文+译文"
        LyricDisplayMode.ROMANIZATION -> "原文+罗马音"
        LyricDisplayMode.TRANSLATION -> "原文+译文"
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // 循环切换到下一个模式
                val currentIndex = availableModes.indexOf(currentMode).coerceAtLeast(0)
                val nextIndex = (currentIndex + 1) % availableModes.size
                onModeChange(availableModes[nextIndex])
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 模式指示器（小点，带动画）
            availableModes.forEachIndexed { index, mode ->
                val isActive = mode == currentMode
                val dotColor by animateColorAsState(
                    targetValue = if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        contentColor.copy(alpha = 0.3f)
                    },
                    animationSpec = tween(200),
                    label = "dotColor"
                )
                
                // 点的大小动画
                val dotSize by animateDpAsState(
                    targetValue = if (isActive) 6.dp else 4.dp,
                    animationSpec = tween(200),
                    label = "dotSize"
                )
                
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(RoundedCornerShape(50))
                        .background(dotColor)
                )
            }
            
            // 当前模式文字
            Text(
                text = modeText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

/**
 * 歌词模式切换按钮（带完整标签）
 * 
 * 显示当前模式的完整描述，适合有足够空间时使用
 */
@Composable
fun LyricModeToggleExpanded(
    currentMode: LyricDisplayMode,
    hasTranslation: Boolean,
    hasRomanization: Boolean,
    onModeChange: (LyricDisplayMode) -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    // 构建可用模式列表
    val availableModes = buildList {
        add(LyricDisplayMode.ORIGINAL)
        if (hasTranslation) add(LyricDisplayMode.DUAL)
        if (hasRomanization) add(LyricDisplayMode.ROMANIZATION)
    }
    
    // 如果只有原文模式，不显示切换按钮
    if (availableModes.size <= 1) return
    
    // 当前模式的完整说明
    val modeText = when (currentMode) {
        LyricDisplayMode.ORIGINAL -> "原文"
        LyricDisplayMode.DUAL -> "原+译"
        LyricDisplayMode.ROMANIZATION -> "原+罗"
        LyricDisplayMode.TRANSLATION -> "原+译"
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                val currentIndex = availableModes.indexOf(currentMode).coerceAtLeast(0)
                val nextIndex = (currentIndex + 1) % availableModes.size
                onModeChange(availableModes[nextIndex])
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = modeText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}
