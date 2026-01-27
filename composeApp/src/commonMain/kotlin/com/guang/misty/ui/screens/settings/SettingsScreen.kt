package com.guang.misty.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guang.misty.ui.navigation.LocalBottomBarHeight
import com.guang.misty.ui.theme.LocalThemeState
import com.guang.misty.ui.theme.ThemeColor
import com.guang.misty.ui.theme.ThemeMode
import misty.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * 设置页面 - MD3 Expressive 风格
 * 
 * 特点：
 * - LargeTopAppBar 大标题
 * - 分组卡片设计
 * - 宽松间距和大圆角
 * - 清晰的视觉层次
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToPlugins: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeState = LocalThemeState.current
    var showThemeDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var wifiOnlyDownload by remember { mutableStateOf(true) }
    
    // 获取底部栏高度
    val bottomBarHeight = LocalBottomBarHeight.current
    
    // 顶栏滚动折叠行为 - 使用 MediumTopAppBar 配合 exitUntilCollapsedScrollBehavior
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { 
                    Text(
                        text = stringResource(Res.string.settings_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        contentWindowInsets = WindowInsets(0), // 让内容延伸到系统栏
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = bottomBarHeight + 4.dp // 底部留出底部栏的空间
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 插件设置组
            item {
                SettingsGroup(title = stringResource(Res.string.settings_plugins)) {
                    SettingsItem(
                        icon = Icons.Outlined.Extension,
                        title = stringResource(Res.string.settings_plugin_management),
                        subtitle = stringResource(Res.string.settings_plugin_management_desc),
                        onClick = onNavigateToPlugins
                    )
                }
            }
            
            // 播放设置组
            item {
                SettingsGroup(title = stringResource(Res.string.settings_playback)) {
                    SettingsItem(
                        icon = Icons.Outlined.HighQuality,
                        title = stringResource(Res.string.settings_audio_quality),
                        subtitle = "高品质",
                        onClick = { /* TODO */ }
                    )
                }
            }
            
            // 下载设置组
            item {
                SettingsGroup(title = stringResource(Res.string.settings_download)) {
                    Column {
                        SettingsItem(
                            icon = Icons.Outlined.Folder,
                            title = stringResource(Res.string.settings_download_path),
                            subtitle = "/Music/Misty",
                            onClick = { /* TODO */ }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        SettingsItem(
                            icon = Icons.Outlined.MusicNote,
                            title = stringResource(Res.string.settings_download_quality),
                            subtitle = "无损",
                            onClick = { /* TODO */ }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        SettingsSwitchItem(
                            icon = Icons.Outlined.Wifi,
                            title = stringResource(Res.string.settings_wifi_only),
                            subtitle = "仅在 WiFi 环境下下载",
                            checked = wifiOnlyDownload,
                            onCheckedChange = { wifiOnlyDownload = it }
                        )
                    }
                }
            }
            
            // 外观设置组
            item {
                SettingsGroup(title = stringResource(Res.string.settings_appearance)) {
                    Column {
                        SettingsItem(
                            icon = Icons.Outlined.Palette,
                            title = stringResource(Res.string.settings_theme),
                            subtitle = when (themeState.themeMode) {
                                ThemeMode.SYSTEM -> stringResource(Res.string.theme_system)
                                ThemeMode.LIGHT -> stringResource(Res.string.theme_light)
                                ThemeMode.DARK -> stringResource(Res.string.theme_dark)
                            },
                            onClick = { showThemeDialog = true }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        SettingsItem(
                            icon = Icons.Outlined.ColorLens,
                            title = stringResource(Res.string.settings_theme_color),
                            subtitle = "自定义主题色",
                            showColorPreview = true,
                            previewColor = themeState.seedColor,
                            onClick = { showColorDialog = true }
                        )
                    }
                }
            }
            
            // 关于设置组
            item {
                SettingsGroup(title = stringResource(Res.string.settings_about)) {
                    Column {
                        SettingsItem(
                            icon = Icons.Outlined.Info,
                            title = stringResource(Res.string.settings_about_app),
                            subtitle = "版本 1.0.0",
                            onClick = onNavigateToAbout
                        )

                        SettingsItem(
                            icon = Icons.Outlined.BugReport,
                            title = stringResource(Res.string.debug_title),
                            onClick = onNavigateToDebug
                        )
                    }
                }
            }
            
        }
    }
    
    // 主题选择对话框
    if (showThemeDialog) {
        ThemeModeDialog(
            currentMode = themeState.themeMode,
            onModeSelected = { 
                themeState.updateThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
    
    // 主题色选择对话框
    if (showColorDialog) {
        ThemeColorDialog(
            currentColor = themeState.seedColor,
            onColorSelected = { 
                themeState.updateSeedColor(it)
                showColorDialog = false
            },
            onDismiss = { showColorDialog = false }
        )
    }
}

/**
 * 设置组卡片 - MD3 Expressive 风格
 */
@Composable
private fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
        )
        
        Surface(
            shape = RoundedCornerShape(24.dp), // Expressive 大圆角
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

/**
 * 设置项 - MD3 Expressive 风格
 */
@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    showColorPreview: Boolean = false,
    previewColor: androidx.compose.ui.graphics.Color? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 图标
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            
            // 文本
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 颜色预览或箭头
            if (showColorPreview && previewColor != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = previewColor,
                    modifier = Modifier.size(28.dp)
                ) {}
            } else {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * 带开关的设置项 - MD3 Expressive 风格
 */
@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 图标
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            
            // 文本
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 开关
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

/**
 * 主题模式选择对话框 - MD3 Expressive 风格
 */
@Composable
private fun ThemeModeDialog(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp), // Expressive 大圆角
        title = { 
            Text(
                stringResource(Res.string.settings_theme),
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ThemeMode.entries.forEach { mode ->
                    Surface(
                        onClick = { onModeSelected(mode) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (mode == currentMode) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = mode == currentMode,
                                onClick = { onModeSelected(mode) }
                            )
                            Text(
                                text = when (mode) {
                                    ThemeMode.SYSTEM -> stringResource(Res.string.theme_system)
                                    ThemeMode.LIGHT -> stringResource(Res.string.theme_light)
                                    ThemeMode.DARK -> stringResource(Res.string.theme_dark)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (mode == currentMode) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}

/**
 * 主题色选择对话框 - MD3 Expressive 风格
 */
@Composable
private fun ThemeColorDialog(
    currentColor: androidx.compose.ui.graphics.Color,
    onColorSelected: (ThemeColor) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp), // Expressive 大圆角
        title = { 
            Text(
                stringResource(Res.string.settings_theme_color),
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ThemeColor.entries.forEach { themeColor ->
                    val isSelected = themeColor.color == currentColor
                    Surface(
                        onClick = { onColorSelected(themeColor) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onColorSelected(themeColor) }
                            )
                            Surface(
                                color = themeColor.color,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(28.dp)
                            ) {}
                            Text(
                                text = themeColor.name.replace("_", " "),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}
