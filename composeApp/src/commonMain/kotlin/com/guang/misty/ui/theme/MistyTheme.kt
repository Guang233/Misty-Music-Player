package com.guang.misty.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle

/**
 * Misty 主题
 * 
 * 基于 MaterialKolor 实现动态 Material Design 3 主题
 */
@Composable
fun MistyTheme(
    themeState: ThemeState = remember { ThemeState() },
    content: @Composable () -> Unit
) {
    val isDark = when (themeState.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    
    CompositionLocalProvider(LocalThemeState provides themeState) {
        DynamicMaterialTheme(
            seedColor = themeState.seedColor,
            isDark = isDark,
            animate = true,
            style = PaletteStyle.TonalSpot,  // MD3 默认风格
            typography = MistyTypography,
            content = content
        )
    }
}

/**
 * 自定义字体排版
 * 
 * 使用 MD3 标准字体比例，可自定义字体族
 */
val MistyTypography: Typography
    @Composable
    get() = MaterialTheme.typography.copy(
        // 可以在这里自定义字体
        // 目前使用系统默认字体
    )

/**
 * 窗口尺寸类别
 */
enum class WindowWidthSizeClass {
    Compact,   // < 600dp，手机
    Medium,    // 600-840dp，平板竖屏
    Expanded   // > 840dp，桌面/平板横屏
}

enum class WindowHeightSizeClass {
    Compact,   // < 480dp
    Medium,    // 480-900dp
    Expanded   // > 900dp
}

data class WindowSizeClass(
    val widthSizeClass: WindowWidthSizeClass,
    val heightSizeClass: WindowHeightSizeClass
)

/**
 * CompositionLocal for window size class
 */
val LocalWindowSizeClass = compositionLocalOf { 
    WindowSizeClass(WindowWidthSizeClass.Compact, WindowHeightSizeClass.Medium) 
}
