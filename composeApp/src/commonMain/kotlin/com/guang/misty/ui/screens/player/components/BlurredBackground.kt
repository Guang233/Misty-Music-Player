package com.guang.misty.ui.screens.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * 模糊背景组件
 * 
 * 使用封面图作为背景，应用高度模糊效果
 * 若无封面图则使用主题色渐变背景
 * 
 * 自动适配浅色/深色模式，确保文字可读性
 */
@Composable
fun BlurredBackground(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    blurRadius: Int = 80
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    
    // 检测当前是否为深色模式（基于 surface 颜色亮度判断）
    val isDarkTheme = surfaceColor.luminance() < 0.5f

    Box(modifier = modifier.fillMaxSize()) {
        if (imageUrl != null) {
            // 有封面图：显示模糊的封面图
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurRadius.dp)
                    .drawWithContent {
                        drawContent()
                        
                        // 根据主题选择叠加层策略
                        if (isDarkTheme) {
                            // 深色模式：使用半透明黑色叠加
                            drawRect(color = Color.Black.copy(alpha = 0.55f))
                        } else {
                            // 浅色模式：先用半透明白色柔化，再轻微暗化确保对比度
                            drawRect(color = Color.White.copy(alpha = 0.65f))
                            drawRect(color = Color.Black.copy(alpha = 0.1f))
                        }
                    }
            )
        } else {
            // 无封面图：使用主题色渐变背景
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = if (isDarkTheme) 0.4f else 0.2f),
                                surfaceColor.copy(alpha = 0.98f)
                            )
                        )
                    )
            )
        }
        
        // 额外的渐变层，增强底部控制区域文字可读性
        // 根据主题动态调整透明度
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = if (isDarkTheme) {
                            // 深色模式：使用 surface 颜色渐变
                            arrayOf(
                                0.0f to Color.Transparent,
                                0.4f to surfaceColor.copy(alpha = 0.2f),
                                0.7f to surfaceColor.copy(alpha = 0.5f),
                                1.0f to surfaceColor.copy(alpha = 0.75f)
                            )
                        } else {
                            // 浅色模式：使用更柔和的渐变
                            arrayOf(
                                0.0f to Color.Transparent,
                                0.5f to surfaceColor.copy(alpha = 0.3f),
                                0.75f to surfaceColor.copy(alpha = 0.6f),
                                1.0f to surfaceColor.copy(alpha = 0.85f)
                            )
                        }
                    )
                )
        )
    }
}
