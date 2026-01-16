package com.guang.misty.ui.screens.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import com.guang.misty.ui.navigation.LocalBottomBarHeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import misty.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * 媒体库页面 - MD3 Expressive 风格
 * 
 * 特点：
 * - 更宽松的间距
 * - 列表形式展示歌单
 * - 视觉层次分明
 * - 圆润的形状
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToLocalMusic: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToPlaylist: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: 从 ViewModel 获取
    val playlists = remember {
        mutableStateListOf(
            PlaylistInfo("favorites", "我喜欢", 56, true),
            PlaylistInfo("1", "深夜放松", 23, false),
            PlaylistInfo("2", "运动健身", 15, false),
            PlaylistInfo("3", "学习专注", 8, false),
        )
    }
    
    // 获取底部栏高度
    val bottomBarHeight = LocalBottomBarHeight.current
    
    // 顶栏滚动折叠行为 - 使用 MediumTopAppBar 配合 exitUntilCollapsedScrollBehavior
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { 
                    Text(
                        text = stringResource(Res.string.library_title),
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreatePlaylist,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(Res.string.library_create_playlist)) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(bottom = bottomBarHeight) // FAB 避开底部栏
            )
        },
        contentWindowInsets = WindowInsets(0), // 让内容延伸到系统栏
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = bottomBarHeight + 72.dp), // 底部栏 + FAB 空间
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 快捷入口区域
            item {
                QuickAccessSection(
                    onNavigateToLocalMusic = onNavigateToLocalMusic,
                    onNavigateToDownloads = onNavigateToDownloads
                )
            }
            
            // 分隔标题
            item {
                SectionHeader(
                    title = stringResource(Res.string.library_playlists),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
            
            // 歌单列表
            items(playlists, key = { it.id }) { playlist ->
                PlaylistListItem(
                    playlist = playlist,
                    onClick = { onNavigateToPlaylist(playlist.id) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            // 底部留白
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * 快捷入口区域 - 卡片样式
 */
@Composable
private fun QuickAccessSection(
    onNavigateToLocalMusic: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickAccessCard(
            icon = Icons.Outlined.Folder,
            title = stringResource(Res.string.library_local_music),
            subtitle = "扫描本地音乐文件",
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = onNavigateToLocalMusic
        )
        
        QuickAccessCard(
            icon = Icons.Default.Download,
            title = stringResource(Res.string.library_downloads),
            subtitle = "已下载的音乐",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            onClick = onNavigateToDownloads
        )
    }
}

/**
 * 快捷入口卡片 - MD3 Expressive 风格
 */
@Composable
private fun QuickAccessCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp), // Expressive 大圆角
        color = containerColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = contentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * 分隔标题
 */
@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/**
 * 歌单列表项 - MD3 Expressive 风格
 */
@Composable
private fun PlaylistListItem(
    playlist: PlaylistInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (playlist.isFavorites) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "containerColor"
    )
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp), // Expressive 圆角
        color = containerColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 封面/图标
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (playlist.isFavorites) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (playlist.isFavorites) Icons.Default.Favorite else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (playlist.isFavorites) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            // 信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${playlist.songCount} 首歌曲",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 箭头
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * 歌单信息
 */
data class PlaylistInfo(
    val id: String,
    val name: String,
    val songCount: Int,
    val isFavorites: Boolean = false
)
