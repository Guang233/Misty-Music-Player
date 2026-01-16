package com.guang.misty.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.guang.misty.ui.theme.LocalWindowSizeClass
import com.guang.misty.ui.theme.WindowWidthSizeClass
import org.jetbrains.compose.resources.stringResource

/**
 * 底部栏高度的 CompositionLocal，供内容区域使用以添加底部 padding
 */
val LocalBottomBarHeight = compositionLocalOf { 0.dp }

/**
 * Misty 主导航框架
 * 
 * - Compact: 底部导航栏 (NavigationBar)
 * - Expanded: 侧边导航 (NavigationRail)
 * - 支持 edge-to-edge 布局（内容延伸到状态栏和手势条）
 * - MiniPlayer 圆角外部透明（使用 Box 叠加布局实现）
 * - 内容可以滚动到底部栏上方
 */
@Composable
fun MistyNavigation(
    currentDestination: MainDestination,
    onNavigate: (MainDestination) -> Unit,
    miniPlayer: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val useNavRail = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val density = LocalDensity.current
    
    if (useNavRail) {
        // 桌面端：侧边导航
        Row(modifier = Modifier.fillMaxSize()) {
            MistyNavigationRail(
                currentDestination = currentDestination,
                onNavigate = onNavigate
            )
            
            // 测量 MiniPlayer 高度
            var bottomBarHeightPx by remember { mutableIntStateOf(0) }
            val bottomBarHeight = with(density) { bottomBarHeightPx.toDp() }
            
            // 使用 Box 叠加布局，让 MiniPlayer 浮在内容上方
            Box(modifier = Modifier.weight(1f)) {
                // 主内容区域 - 提供底部栏高度给内容使用
                CompositionLocalProvider(LocalBottomBarHeight provides bottomBarHeight) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        content()
                    }
                }
                
                // 底部播放控制栏 - 浮在内容上方，圆角外部透明
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { bottomBarHeightPx = it.height }
                ) {
                    miniPlayer()
                }
            }
        }
    } else {
        // 手机端：使用 Box 叠加布局实现 MiniPlayer 圆角透明效果
        // 测量底部栏高度（MiniPlayer + NavigationBar + 手势条）
        var bottomBarHeightPx by remember { mutableIntStateOf(0) }
        val bottomBarHeight = with(density) { bottomBarHeightPx.toDp() }
        
        Box(modifier = Modifier.fillMaxSize()) {
            // 主内容区域 - 不添加任何 padding，让各个 Screen 自己处理
            // 提供底部栏高度给内容使用，内容可以据此添加底部 padding
            CompositionLocalProvider(LocalBottomBarHeight provides bottomBarHeight) {
                Box(modifier = Modifier.fillMaxSize()) {
                    content()
                }
            }
            
            // 底部栏 - 浮在内容上方
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { bottomBarHeightPx = it.height }
            ) {
                // 迷你播放条 - 圆角外部透明，可以看到下面的内容
                miniPlayer()
                // 底部导航栏 - 自动处理手势条内边距
                MistyNavigationBar(
                    currentDestination = currentDestination,
                    onNavigate = onNavigate
                )
            }
        }
    }
}

/**
 * 底部导航栏（手机端）
 */
@Composable
private fun MistyNavigationBar(
    currentDestination: MainDestination,
    onNavigate: (MainDestination) -> Unit
) {
    NavigationBar {
        MainDestination.entries.forEach { destination ->
            val isSelected = currentDestination == destination
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = null
                    )
                },
                label = {
                    Text(destination.label())
                }
            )
        }
    }
}

/**
 * 侧边导航（桌面端）
 */
@Composable
private fun MistyNavigationRail(
    currentDestination: MainDestination,
    onNavigate: (MainDestination) -> Unit
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        
        MainDestination.entries.forEach { destination ->
            val isSelected = currentDestination == destination
            NavigationRailItem(
                selected = isSelected,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = null
                    )
                },
                label = {
                    Text(destination.label())
                }
            )
        }
    }
}
