package com.guang.misty.ui.screens.player.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.guang.misty.model.MistyArtist
import misty.composeapp.generated.resources.Res
import misty.composeapp.generated.resources.player_artists
import org.jetbrains.compose.resources.stringResource

/**
 * 播放器歌曲信息组件 - MD3 Expressive 风格
 * 
 * - 标题：超宽时自动走马灯滚动，使用 emphasized 字重
 * - 艺术家：小屏幕单行走马灯 + BottomSheet，大屏幕 FlowRow 换行
 * 
 * @param isCompact true=小屏幕模式，false=大屏幕模式
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerSongInfo(
    title: String,
    artists: List<MistyArtist>,
    onTitleClick: () -> Unit,
    onArtistClick: (MistyArtist) -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    centered: Boolean = true,
    isCompact: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    var showArtistSheet by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
    ) {
        // 歌曲标题 - emphasized 字重 + 走马灯
        Text(
            text = title,
            style = if (centered) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.titleMedium
            },
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            color = contentColor,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    initialDelayMillis = 2000,
                    repeatDelayMillis = 3000
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onTitleClick
                )
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // 艺术家
        if (artists.isNotEmpty()) {
            if (isCompact) {
                CompactArtistRow(
                    artists = artists,
                    contentColor = contentColor,
                    centered = centered,
                    onArtistClick = onArtistClick,
                    onShowSheet = { showArtistSheet = true }
                )
            } else {
                ExpandedArtistFlowRow(
                    artists = artists,
                    contentColor = contentColor,
                    centered = centered,
                    onArtistClick = onArtistClick
                )
            }
        }
    }
    
    // 艺术家 BottomSheet
    if (showArtistSheet) {
        ArtistBottomSheet(
            artists = artists,
            onArtistClick = { artist ->
                onArtistClick(artist)
                showArtistSheet = false
            },
            onDismiss = { showArtistSheet = false }
        )
    }
}

/**
 * 小屏幕：艺术家单行走马灯
 * 1 个艺术家直接跳转，多个弹 BottomSheet
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactArtistRow(
    artists: List<MistyArtist>,
    contentColor: Color,
    centered: Boolean,
    onArtistClick: (MistyArtist) -> Unit,
    onShowSheet: () -> Unit
) {
    val artistText = remember(artists) {
        artists.joinToString(" / ") { it.name }
    }
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (centered) Alignment.Center else Alignment.CenterStart
    ) {
        Text(
            text = artistText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor.copy(alpha = 0.7f),
            maxLines = 1,
            modifier = Modifier
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    initialDelayMillis = 2000,
                    repeatDelayMillis = 3000
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (artists.size == 1) {
                            onArtistClick(artists.first())
                        } else {
                            onShowSheet()
                        }
                    }
                )
        )
    }
}

/**
 * 大屏幕：FlowRow 换行，每个艺术家独立可点击
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpandedArtistFlowRow(
    artists: List<MistyArtist>,
    contentColor: Color,
    centered: Boolean,
    onArtistClick: (MistyArtist) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (centered) Arrangement.Center else Arrangement.Start,
        verticalArrangement = Arrangement.Center
    ) {
        artists.forEachIndexed { index, artist ->
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onArtistClick(artist) }
                )
            )
            
            if (index < artists.lastIndex) {
                Text(
                    text = " / ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.4f)
                )
            }
        }
    }
}

/**
 * 艺术家 BottomSheet - MD3 Expressive 风格
 * 
 * - 圆角头像 + 名称
 * - Surface 容器实现 tonal containment
 * - 导航箭头暗示可交互
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtistBottomSheet(
    artists: List<MistyArtist>,
    onArtistClick: (MistyArtist) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // 标题 - emphasized 字重
            Text(
                text = stringResource(Res.string.player_artists),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            
            // 艺术家列表
            artists.forEach { artist ->
                Surface(
                    onClick = { onArtistClick(artist) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 头像 - 圆形裁剪
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (artist.coverUrl != null) {
                                AsyncImage(
                                    model = artist.coverUrl,
                                    contentDescription = artist.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        // 名称 - emphasized
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // 导航箭头
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 简化版歌曲信息（使用字符串艺术家，向后兼容）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerSongInfoSimple(
    title: String,
    artist: String,
    onTitleClick: () -> Unit = {},
    onArtistClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    centered: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
    ) {
        // 标题 - emphasized + 走马灯
        Text(
            text = title,
            style = if (centered) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.titleMedium
            },
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            color = contentColor,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    initialDelayMillis = 2000,
                    repeatDelayMillis = 3000
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onTitleClick
                )
        )
        
        if (artist.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor.copy(alpha = 0.7f),
                maxLines = 1,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                modifier = Modifier
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 2000,
                        repeatDelayMillis = 3000
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onArtistClick
                    )
            )
        }
    }
}
