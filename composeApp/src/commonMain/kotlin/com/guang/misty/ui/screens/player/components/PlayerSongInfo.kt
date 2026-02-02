package com.guang.misty.ui.screens.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guang.misty.model.MistyArtist

/**
 * 播放器歌曲信息组件 - MD3 Expressive 风格
 * 
 * 显示歌曲标题和艺术家名（均可点击）
 * 艺术家用 " / " 分隔，每个可单独点击
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerSongInfo(
    title: String,
    artists: List<MistyArtist>,
    onTitleClick: () -> Unit,
    onArtistClick: (MistyArtist) -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    centered: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
    ) {
        // 歌曲标题（可点击）
        Text(
            text = title,
            style = if (centered) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.titleMedium
            },
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = contentColor,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTitleClick
            )
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 艺术家列表（用 / 分隔，每个可点击）
        if (artists.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (centered) {
                    Arrangement.Center
                } else {
                    Arrangement.Start
                },
                verticalArrangement = Arrangement.Center
            ) {
                artists.forEachIndexed { index, artist ->
                    // 艺术家名（可点击）
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onArtistClick(artist) }
                        )
                    )
                    
                    // 分隔符 " / "
                    if (index < artists.lastIndex) {
                        Text(
                            text = " / ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = 0.5f)
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
        // 歌曲标题（可点击）
        Text(
            text = title,
            style = if (centered) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.titleMedium
            },
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = contentColor,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTitleClick
            )
        )
        
        if (artist.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            
            // 艺术家（可点击）
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onArtistClick
                )
            )
        }
    }
}
