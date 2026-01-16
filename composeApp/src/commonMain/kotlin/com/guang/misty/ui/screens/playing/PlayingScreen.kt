package com.guang.misty.ui.screens.playing

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import com.guang.misty.ui.navigation.LocalBottomBarHeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import misty.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * 播放页面（主页）- MD3 Expressive 风格
 * 
 * 特点：
 * - LargeTopAppBar 大标题
 * - 宽松的间距和大圆角
 * - 当前播放项突出显示
 * - 空状态的表现力设计
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayingScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: 从 ViewModel 获取播放队列
    val queue = remember { mutableStateListOf<PlayingItem>() }
    val currentIndex = remember { mutableIntStateOf(-1) }
    
    // 获取底部栏高度
    val bottomBarHeight = LocalBottomBarHeight.current
    
    // 顶栏滚动折叠行为 - 使用 MediumTopAppBar 配合 exitUntilCollapsedScrollBehavior
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { 
                    Text(
                        text = stringResource(Res.string.playing_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (queue.isNotEmpty()) {
                        FilledTonalIconButton(
                            onClick = { /* TODO: 随机播放 */ }
                        ) {
                            Icon(
                                Icons.Outlined.Shuffle, 
                                contentDescription = stringResource(Res.string.playing_shuffle)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        FilledTonalIconButton(
                            onClick = { queue.clear() }
                        ) {
                            Icon(
                                Icons.Outlined.DeleteSweep, 
                                contentDescription = stringResource(Res.string.playing_clear)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
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
        if (queue.isEmpty()) {
            // 空状态 - Expressive 设计
            EmptyQueueContent(
                onNavigateToSearch = onNavigateToSearch,
                onNavigateToLibrary = onNavigateToLibrary,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(bottom = bottomBarHeight + 4.dp) // 避开底部栏遮挡
            )
        } else {
            // 播放队列列表
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(queue, key = { _, item -> item.id }) { index, item ->
                    QueueItem(
                        item = item,
                        isPlaying = index == currentIndex.intValue,
                        index = index + 1,
                        onClick = { /* TODO: 播放该曲 */ },
                        onMoreClick = { /* TODO: 显示菜单 */ }
                    )
                }
            }
        }
    }
}

/**
 * 空队列提示内容 - MD3 Expressive 风格
 */
@Composable
private fun EmptyQueueContent(
    onNavigateToSearch: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 装饰性背景
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = stringResource(Res.string.playing_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = stringResource(Res.string.playing_empty_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // 操作按钮 - Expressive 风格
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onNavigateToSearch,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.playing_go_search),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            OutlinedButton(
                onClick = onNavigateToLibrary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.playing_browse_library),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/**
 * 队列中的歌曲项 - MD3 Expressive 风格
 */
@Composable
private fun QueueItem(
    item: PlayingItem,
    isPlaying: Boolean,
    index: Int,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isPlaying) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "containerColor"
    )
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 序号或播放指示
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isPlaying) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isPlaying) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = index.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // 歌曲信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isPlaying) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isPlaying) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            // 更多按钮
            FilledTonalIconButton(
                onClick = onMoreClick,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (isPlaying) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    }
                )
            ) {
                Icon(
                    Icons.Default.MoreVert, 
                    contentDescription = stringResource(Res.string.action_more),
                    tint = if (isPlaying) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

/**
 * 播放队列项数据
 */
data class PlayingItem(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String? = null,
    val duration: Long = 0
)
