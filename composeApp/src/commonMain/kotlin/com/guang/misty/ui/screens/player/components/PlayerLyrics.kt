package com.guang.misty.ui.screens.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guang.misty.model.LyricDisplayMode
import com.guang.misty.model.LyricLine
import com.guang.misty.model.LyricWord
import misty.composeapp.generated.resources.Res
import misty.composeapp.generated.resources.player_no_lyrics
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 播放器歌词组件 - MD3 Expressive 风格
 * 
 * 特点：
 * - 支持逐行和逐字高亮
 * - 自动滚动使当前歌词居中显示
 * - 手动滚动后暂停自动滚动，3秒后自动恢复
 * - 点击歌词行跳转到对应时间（带水波纹效果）
 * - 平滑的透明度和缩放过渡动画
 * - 支持显示原文/译文/罗马音
 */
@Composable
fun PlayerLyrics(
    lyrics: List<LyricLine>,
    currentIndex: Int,
    currentPosition: Long,
    displayMode: LyricDisplayMode,
    onSeekTo: (Long) -> Unit,
    onToggleImmersive: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
    isImmersive: Boolean = false,
    isTransitioning: Boolean = false, // 是否正在进行过渡动画（沉浸模式切换时）
    onLongPress: (() -> Unit)? = null, // 长按回调，用于小屏幕设备切换沉浸模式
    // 歌词模式切换相关
    hasTranslation: Boolean = false,
    hasRomanization: Boolean = false,
    onLyricModeChange: ((LyricDisplayMode) -> Unit)? = null
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // 视口高度（用于计算居中偏移）
    var viewportHeight by remember { mutableIntStateOf(0) }
    
    // 手动滚动检测
    var isUserScrolling by remember { mutableStateOf(false) }
    var scrollResumeJob by remember { mutableStateOf<Job?>(null) }
    
    // 标记是否正在进行自动滚动
    var isAutoScrolling by remember { mutableStateOf(false) }
    
    // 用户点击跳转时的目标索引（-1 表示无）
    var pendingSeekIndex by remember { mutableIntStateOf(-1) }
    
    // 滚动到指定索引（使用较慢的动画速度）
    suspend fun scrollToIndex(targetLyricIndex: Int) {
        if (targetLyricIndex >= 0 && targetLyricIndex < lyrics.size && viewportHeight > 0) {
            val centerOffset = -(viewportHeight / 2 - 40)
            val targetIndex = targetLyricIndex + 1 // +1 因为有顶部 Spacer
            
            isAutoScrolling = true
            try {
                val currentFirstVisible = listState.firstVisibleItemIndex
                val distance = abs(targetIndex - currentFirstVisible)
                
                if (distance > 8) {
                    // 远距离滚动：先快速定位到目标附近
                    val intermediateIndex = if (targetIndex > currentFirstVisible) {
                        targetIndex - 3
                    } else {
                        targetIndex + 3
                    }.coerceIn(0, lyrics.size)
                    
                    listState.scrollToItem(index = intermediateIndex, scrollOffset = 0)
                    delay(50)
                }
                
                // 使用 animateScrollBy 配合较慢的 tween 实现平滑滚动
                val layoutInfo = listState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                val targetItem = visibleItems.find { it.index == targetIndex }
                
                if (targetItem != null) {
                    // 目标在可见范围内，计算精确滚动距离
                    val viewportCenter = viewportHeight / 2
                    val itemCenter = targetItem.offset + targetItem.size / 2
                    val scrollDistance = itemCenter - viewportCenter
                    
                    listState.animateScrollBy(
                        value = scrollDistance.toFloat(),
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
                } else {
                    // 目标不在可见范围，使用 animateScrollToItem
                    listState.animateScrollToItem(index = targetIndex, scrollOffset = centerOffset)
                }
            } finally {
                isAutoScrolling = false
            }
        }
    }
    
    // 监听滚动状态变化，区分用户滚动和自动滚动
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { isScrolling ->
                if (isScrolling && !isAutoScrolling) {
                    isUserScrolling = true
                    scrollResumeJob?.cancel()
                } else if (!isScrolling && isUserScrolling) {
                    scrollResumeJob?.cancel()
                    scrollResumeJob = coroutineScope.launch {
                        delay(3000)
                        isUserScrolling = false
                    }
                }
            }
    }
    
    // 当 isUserScrolling 变为 false 时，自动滚动回当前歌词位置
    LaunchedEffect(isUserScrolling) {
        if (!isUserScrolling && !isTransitioning && pendingSeekIndex < 0 && 
            currentIndex >= 0 && viewportHeight > 0) {
            delay(50)
            scrollToIndex(currentIndex)
        }
    }
    
    // 自动滚动到当前歌词 - 每次 currentIndex 变化都触发
    LaunchedEffect(currentIndex) {
        if (currentIndex < 0 || currentIndex >= lyrics.size || viewportHeight <= 0) return@LaunchedEffect
        
        // 如果有待处理的点击跳转
        if (pendingSeekIndex >= 0) {
            if (currentIndex == pendingSeekIndex) {
                // currentIndex 已更新到用户点击的位置，现在滚动
                pendingSeekIndex = -1
                isUserScrolling = false
                scrollResumeJob?.cancel()
                scrollToIndex(currentIndex)
            }
            // 如果 currentIndex 还没更新到目标位置，不做任何事，继续等待
            return@LaunchedEffect
        }
        
        // 正常的自动滚动
        if (!isUserScrolling && !isTransitioning) {
            scrollToIndex(currentIndex)
        }
    }
    
    // 过渡动画结束后，自动滚动到当前歌词
    LaunchedEffect(isTransitioning) {
        if (!isTransitioning && !isUserScrolling && pendingSeekIndex < 0 &&
            currentIndex >= 0 && viewportHeight > 0) {
            delay(100)
            scrollToIndex(currentIndex)
        }
    }
    
    // 切换歌词显示模式后，重新居中当前歌词（因为歌词高度可能变化）
    LaunchedEffect(displayMode) {
        if (!isUserScrolling && currentIndex >= 0 && viewportHeight > 0) {
            delay(300) // 等待高度动画完成
            scrollToIndex(currentIndex)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                viewportHeight = size.height
            }
    ) {
        if (lyrics.isEmpty()) {
            // 空状态
            EmptyLyricsView(
                contentColor = contentColor,
                onToggleImmersive = onToggleImmersive
            )
        } else {
            // 歌词列表
            LyricsListView(
                lyrics = lyrics,
                currentIndex = currentIndex,
                currentPosition = currentPosition,
                displayMode = displayMode,
                listState = listState,
                viewportHeight = viewportHeight,
                contentColor = contentColor,
                highlightColor = highlightColor,
                onLineClick = { index ->
                    val line = lyrics.getOrNull(index)
                    if (line != null) {
                        // 记录用户点击的目标索引
                        pendingSeekIndex = index
                        
                        // 取消之前的恢复任务
                        scrollResumeJob?.cancel()
                        
                        // 跳转播放位置，等待 currentIndex 更新后再滚动
                        onSeekTo(line.startTimeMs)
                    }
                },
                onLineLongPress = onLongPress,
                onBackgroundClick = onToggleImmersive
            )
            
            // 歌词模式切换按钮（右下角，沉浸模式时隐藏）
            AnimatedVisibility(
                visible = !isImmersive && onLyricModeChange != null && (hasTranslation || hasRomanization),
                enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.8f),
                exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                LyricModeToggle(
                    currentMode = displayMode,
                    hasTranslation = hasTranslation,
                    hasRomanization = hasRomanization,
                    onModeChange = onLyricModeChange ?: {},
                    contentColor = contentColor
                )
            }
        }
    }
}

/**
 * 空歌词状态
 */
@Composable
private fun EmptyLyricsView(
    contentColor: Color,
    onToggleImmersive: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggleImmersive
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lyrics,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = contentColor.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.player_no_lyrics),
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor.copy(alpha = 0.5f)
        )
    }
}

/**
 * 歌词列表视图
 */
@Composable
private fun LyricsListView(
    lyrics: List<LyricLine>,
    currentIndex: Int,
    currentPosition: Long,
    displayMode: LyricDisplayMode,
    listState: LazyListState,
    viewportHeight: Int,
    contentColor: Color,
    highlightColor: Color,
    onLineClick: (Int) -> Unit,
    onLineLongPress: (() -> Unit)?,
    onBackgroundClick: () -> Unit
) {
    val density = LocalDensity.current
    
    // 计算顶部/底部间距：让第一句/最后一句最多滚动到屏幕中央（减去一点偏移让它不要完全到中央）
    val verticalPaddingPx = if (viewportHeight > 0) {
        // 视口高度的一半减去一些偏移，这样第一句/最后一句最多到中央附近
        (viewportHeight / 2 - 60).coerceAtLeast(80)
    } else {
        200
    }
    val verticalPaddingDp = with(density) { verticalPaddingPx.toDp() }
    
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onBackgroundClick() }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部间距（让第一句歌词可以居中）
        item(key = "top_spacer") {
            Spacer(modifier = Modifier.height(verticalPaddingDp))
        }
        
        itemsIndexed(lyrics, key = { index, _ -> "lyric_$index" }) { index, line ->
            val isCurrent = index == currentIndex
            val distance = abs(index - currentIndex.coerceAtLeast(0))
            
            // 根据距离计算目标透明度
            val targetAlpha = when {
                isCurrent -> 1f
                distance == 1 -> 0.7f
                distance == 2 -> 0.5f
                distance == 3 -> 0.35f
                else -> 0.25f
            }
            
            LyricLineItem(
                line = line,
                isCurrent = isCurrent,
                currentPosition = currentPosition,
                displayMode = displayMode,
                targetAlpha = targetAlpha,
                contentColor = contentColor,
                highlightColor = highlightColor,
                onClick = { onLineClick(index) },
                onLongPress = onLineLongPress
            )
        }
        
        // 底部间距（让最后一句歌词可以居中）
        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(verticalPaddingDp))
        }
    }
}

/**
 * 单行歌词显示
 * 
 * 包含平滑的透明度和缩放过渡动画
 * 统一字号，仅通过颜色、字重、透明度和缩放区分当前歌词
 * 支持单击跳转播放、长按切换沉浸模式
 */
@Composable
private fun LyricLineItem(
    line: LyricLine,
    isCurrent: Boolean,
    currentPosition: Long,
    displayMode: LyricDisplayMode,
    targetAlpha: Float,
    contentColor: Color,
    highlightColor: Color,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null
) {
    // 平滑的透明度过渡
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    
    // 当前行的缩放效果（微微放大，不会导致换行）
    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1.05f else 1f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "scale"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = animatedAlpha
            }
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onLongPress != null) {
                    // 小屏幕：使用 pointerInput 支持长按（无水波纹）
                    Modifier.pointerInput(onLongPress) {
                        detectTapGestures(
                            onTap = { onClick() },
                            onLongPress = { onLongPress() }
                        )
                    }
                } else {
                    // 大屏幕：使用 clickable 带水波纹效果
                    Modifier.clickable(onClick = onClick)
                }
            )
            .padding(vertical = 10.dp, horizontal = 8.dp), // 适中的间距
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 主歌词（始终显示原文，TRANSLATION 模式例外）
        val mainText = when (displayMode) {
            LyricDisplayMode.ORIGINAL -> line.text
            LyricDisplayMode.TRANSLATION -> line.translation ?: line.text
            LyricDisplayMode.ROMANIZATION -> line.text // 原文+罗马音模式：主文本为原文
            LyricDisplayMode.DUAL -> line.text
        }
        
        if (line.isWordByWord && isCurrent) {
            // 逐字高亮模式（有逐字歌词时优先显示，不受显示模式影响）
            WordByWordText(
                words = line.words!!,
                currentPosition = currentPosition,
                highlightColor = highlightColor,
                normalColor = contentColor
            )
        } else {
            // 普通文本显示 - 使用 titleMedium 字号，通过颜色和字重区分
            Text(
                text = mainText.ifEmpty { "···" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) highlightColor else contentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // 副歌词（双语模式显示译文，罗马音模式显示罗马音）
        val subText = when (displayMode) {
            LyricDisplayMode.DUAL -> line.translation
            LyricDisplayMode.ROMANIZATION -> line.romanization
            else -> null
        }
        
        // 副歌词出现/消失动画（配合 animateContentSize 实现平滑高度过渡）
        AnimatedVisibility(
            visible = subText != null && subText.isNotEmpty(),
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150))
        ) {
            Column {
                Spacer(modifier = Modifier.height(2.dp))
                // 副歌词使用 bodyMedium 字号
                Text(
                    text = subText ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                    color = if (isCurrent) {
                        highlightColor.copy(alpha = 0.8f)
                    } else {
                        contentColor.copy(alpha = 0.7f)
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 逐字高亮文本 - 帧动画驱动
 * 
 * 整行文本渲染，两层叠加 + DstOut 渐变遮罩：
 * - 底层：淡主题色（未播放部分）
 * - 上层：全亮主题色（已播放部分）
 * - 利用每个字的时长，用 withFrameMillis 帧循环驱动 60fps 平滑动画
 */
@Composable
private fun WordByWordText(
    words: List<LyricWord>,
    currentPosition: Long,
    highlightColor: Color,
    normalColor: Color
) {
    val density = LocalDensity.current
    val fullText = remember(words) { words.joinToString("") { it.text } }
    val totalChars = remember(words) { words.sumOf { it.text.length } }
    
    // 从 currentPosition 检测当前播放的字
    val activeWordIndex = words.indexOfLast { currentPosition >= it.startTimeMs }
    
    // 已完成的字符占比（基准进度）
    val completedChars = if (activeWordIndex > 0) {
        words.take(activeWordIndex).sumOf { it.text.length }
    } else 0
    val baseProgress = if (totalChars > 0) completedChars.toFloat() / totalChars else 0f
    val wordCharFraction = if (activeWordIndex >= 0 && totalChars > 0) {
        words[activeWordIndex].text.length.toFloat() / totalChars
    } else 0f
    
    // 帧动画驱动的字内进度 (0.0 ~ 1.0)
    // 使用 remember(activeWordIndex) 保证字切换时立即重置，避免闪烁
    val initialWordProgress = if (activeWordIndex >= 0) {
        val word = words[activeWordIndex]
        val duration = word.endTimeMs - word.startTimeMs
        if (duration > 0 && currentPosition < word.endTimeMs) {
            ((currentPosition - word.startTimeMs).toFloat() / duration).coerceIn(0f, 1f)
        } else 1f
    } else 0f
    
    var wordAnimProgress by remember(activeWordIndex) { mutableFloatStateOf(initialWordProgress) }
    
    LaunchedEffect(activeWordIndex) {
        if (activeWordIndex < 0 || totalChars == 0) return@LaunchedEffect
        
        val word = words[activeWordIndex]
        val wordDuration = word.endTimeMs - word.startTimeMs
        
        if (wordDuration <= 0 || currentPosition >= word.endTimeMs) {
            wordAnimProgress = 1f
            return@LaunchedEffect
        }
        
        val remainingMs = ((1f - initialWordProgress) * wordDuration).toLong()
        if (remainingMs <= 0) {
            wordAnimProgress = 1f
            return@LaunchedEffect
        }
        
        // 60fps 帧循环：自主驱动字内平滑动画
        val startFrame = withFrameMillis { it }
        while (wordAnimProgress < 1f) {
            withFrameMillis { frameTime ->
                val elapsed = frameTime - startFrame
                val additional = (elapsed.toFloat() / remainingMs) * (1f - initialWordProgress)
                wordAnimProgress = (initialWordProgress + additional).coerceIn(0f, 1f)
            }
        }
    }
    
    // 最终行进度
    val lineProgress = (baseProgress + wordCharFraction * wordAnimProgress).coerceIn(0f, 1f)
    
    // 渐变边缘宽度（固定像素，约 1/3 个字宽）
    val gradientEdgePx = with(density) { 4.dp.toPx() }
    
    // 淡主题色（未播放的字）
    val dimColor = highlightColor.copy(alpha = 0.35f)
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // 底层：淡主题色（未播放部分）
        Text(
            text = fullText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = dimColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        // 上层：高亮色，用 DstOut 渐变遮罩
        Text(
            text = fullText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = highlightColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    
                    if (lineProgress < 1f) {
                        val clipEnd = size.width * lineProgress
                        val gradStart = ((clipEnd - gradientEdgePx) / size.width).coerceIn(0f, 1f)
                        val gradEnd = ((clipEnd + gradientEdgePx * 0.5f) / size.width).coerceIn(gradStart + 0.001f, 1f)
                        
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    gradStart to Color.Transparent,
                                    gradEnd to Color.Black,
                                    1f to Color.Black
                                )
                            ),
                            blendMode = BlendMode.DstOut
                        )
                    }
                }
        )
    }
}

/**
 * 沉浸模式歌词视图
 */
@Composable
fun ImmersiveLyricsView(
    lyrics: List<LyricLine>,
    currentIndex: Int,
    currentPosition: Long,
    displayMode: LyricDisplayMode,
    onSeekTo: (Long) -> Unit,
    onExitImmersive: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    highlightColor: Color = MaterialTheme.colorScheme.primary
) {
    PlayerLyrics(
        lyrics = lyrics,
        currentIndex = currentIndex,
        currentPosition = currentPosition,
        displayMode = displayMode,
        onSeekTo = onSeekTo,
        onToggleImmersive = onExitImmersive,
        modifier = modifier,
        contentColor = contentColor,
        highlightColor = highlightColor,
        isImmersive = true
    )
}
