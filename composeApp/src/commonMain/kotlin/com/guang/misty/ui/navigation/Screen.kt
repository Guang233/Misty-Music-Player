package com.guang.misty.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import misty.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * 主导航目的地
 */
enum class MainDestination(
    val route: String,
    val labelRes: StringResource,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Playing(
        route = "playing",
        labelRes = Res.string.nav_playing,
        selectedIcon = Icons.Filled.MusicNote,
        unselectedIcon = Icons.Outlined.MusicNote
    ),
    Search(
        route = "search",
        labelRes = Res.string.nav_search,
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    ),
    Library(
        route = "library",
        labelRes = Res.string.nav_library,
        selectedIcon = Icons.Filled.FolderOpen,
        unselectedIcon = Icons.Outlined.FolderOpen
    ),
    Settings(
        route = "settings",
        labelRes = Res.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    );
    
    @Composable
    fun label(): String = stringResource(labelRes)
}

/**
 * 子页面路由
 */
sealed class SubScreen(val route: String) {
    // 搜索结果
    data object SearchResult : SubScreen("search/result/{query}") {
        fun createRoute(query: String) = "search/result/$query"
    }
    
    // 播放详情（全屏播放器）
    data object PlayerDetail : SubScreen("player/detail")
    
    // 歌单详情
    data object PlaylistDetail : SubScreen("library/playlist/{id}") {
        fun createRoute(id: String) = "library/playlist/$id"
    }
    
    // 本地音乐
    data object LocalMusic : SubScreen("library/local")
    
    // 已下载
    data object Downloads : SubScreen("library/downloads")
    
    // 插件管理
    data object PluginManagement : SubScreen("settings/plugins")
    
    // 关于
    data object About : SubScreen("settings/about")
}
