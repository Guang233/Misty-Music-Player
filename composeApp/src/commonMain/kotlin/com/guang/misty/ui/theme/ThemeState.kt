package com.guang.misty.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * 主题模式
 */
enum class ThemeMode {
    SYSTEM,  // 跟随系统
    LIGHT,   // 浅色
    DARK     // 深色
}

/**
 * 预设主题色
 */
enum class ThemeColor(val color: Color, val nameKey: String) {
    MIST_BLUE(Color(0xFF5B7C99), "theme_color_mist_blue"),       // 雾霭蓝（默认）
    OCEAN_TEAL(Color(0xFF006A6A), "theme_color_ocean_teal"),    // 海洋青
    FOREST_GREEN(Color(0xFF386A20), "theme_color_forest_green"), // 森林绿
    SUNSET_ORANGE(Color(0xFFB5651D), "theme_color_sunset_orange"),// 日落橙
    LAVENDER(Color(0xFF7B5EA7), "theme_color_lavender"),         // 薰衣草紫
    ROSE(Color(0xFFB4637A), "theme_color_rose"),                 // 玫瑰红
    SLATE(Color(0xFF505F79), "theme_color_slate"),               // 石板灰
}

/**
 * 主题状态管理
 */
class ThemeState {
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set
    
    var seedColor by mutableStateOf(ThemeColor.MIST_BLUE.color)
        private set
    
    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
    }
    
    fun updateSeedColor(color: Color) {
        seedColor = color
    }
    
    fun updateSeedColor(themeColor: ThemeColor) {
        seedColor = themeColor.color
    }
}

/**
 * CompositionLocal for theme state
 */
val LocalThemeState = compositionLocalOf { ThemeState() }
