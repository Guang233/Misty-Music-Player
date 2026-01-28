package com.guang.misty.ui.screens.settings.debug

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guang.misty.ui.util.onRightClick
import com.guang.misty.util.LogEntry
import com.guang.misty.util.LogLevel
import com.guang.misty.util.LogStore
import com.guang.misty.util.copyToClipboard
import kotlinx.coroutines.launch
import misty.composeapp.generated.resources.Res
import misty.composeapp.generated.resources.action_back
import misty.composeapp.generated.resources.action_cancel
import misty.composeapp.generated.resources.action_confirm
import misty.composeapp.generated.resources.debug_clear
import misty.composeapp.generated.resources.debug_clear_confirm
import misty.composeapp.generated.resources.debug_clear_title
import misty.composeapp.generated.resources.debug_cleared_message
import misty.composeapp.generated.resources.debug_copied_message
import misty.composeapp.generated.resources.debug_copy
import misty.composeapp.generated.resources.debug_empty_hint
import misty.composeapp.generated.resources.debug_empty_title
import misty.composeapp.generated.resources.debug_invert_selection
import misty.composeapp.generated.resources.debug_scroll_to_bottom
import misty.composeapp.generated.resources.debug_select_all
import misty.composeapp.generated.resources.debug_selected_count
import misty.composeapp.generated.resources.debug_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DebugScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit
) {
    val logs by LogStore.logs.collectAsState()
    val scope = rememberCoroutineScope()
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    
    // 多选状态
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIndices by remember { mutableStateOf(setOf<Int>()) }
    
    // 清空确认对话框
    var showClearDialog by remember { mutableStateOf(false) }
    
    // 多选模式下的更多菜单
    var showMoreMenu by remember { mutableStateOf(false) }
    
    // 是否显示滚动到底部按钮
    val showScrollToBottom by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            totalItems > 0 && lastVisibleItem < totalItems - 3
        }
    }
    
    // 字符串资源
    val copiedMessage = stringResource(Res.string.debug_copied_message)
    val clearedMessage = stringResource(Res.string.debug_cleared_message)
    
    // 退出多选模式
    fun exitSelectionMode() {
        isSelectionMode = false
        selectedIndices = emptySet()
    }
    
    // 复制选中的日志
    fun copySelectedLogs() {
        val selectedLogs = selectedIndices.sorted().mapNotNull { index ->
            logs.getOrNull(index)?.format()
        }.joinToString("\n")
        
        if (selectedLogs.isNotEmpty()) {
            val size = selectedIndices.size
            copyToClipboard(selectedLogs)
            scope.launch {
                snackbarHostState.showSnackbar(copiedMessage.replace("%1\$d", "$size"))
            }
        }
        exitSelectionMode()
    }
    
    // 全选
    fun selectAll() {
        selectedIndices = logs.indices.toSet()
    }
    
    // 反选
    fun invertSelection() {
        val allIndices = logs.indices.toSet()
        selectedIndices = allIndices - selectedIndices
        if (selectedIndices.isEmpty()) {
            isSelectionMode = false
        }
    }
    
    // 触发多选模式
    fun enterSelectionMode(index: Int) {
        if (!isSelectionMode) {
            isSelectionMode = true
            selectedIndices = setOf(index)
        }
    }

    // 清空确认对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = stringResource(Res.string.debug_clear_title),
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.debug_clear_confirm),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        LogStore.clear()
                        scope.launch {
                            snackbarHostState.showSnackbar(clearedMessage)
                        }
                    }
                ) {
                    Text(
                        text = stringResource(Res.string.action_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(text = stringResource(Res.string.action_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(28.dp)
        )
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text(
                            text = stringResource(Res.string.debug_selected_count).replace("%1\$d", "${selectedIndices.size}"),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.debug_title),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectionMode) {
                            exitSelectionMode()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = if (isSelectionMode) Icons.Default.Clear else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back)
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        // 复制按钮
                        IconButton(
                            onClick = { copySelectedLogs() },
                            enabled = selectedIndices.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = stringResource(Res.string.debug_copy)
                            )
                        }
                        
                        // 更多菜单
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = null
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.debug_select_all)) },
                                    onClick = {
                                        selectAll()
                                        showMoreMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.SelectAll, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.debug_invert_selection)) },
                                    onClick = {
                                        invertSelection()
                                        showMoreMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.FlipToBack, contentDescription = null)
                                    }
                                )
                            }
                        }
                    } else {
                        // 清空日志按钮
                        IconButton(
                            onClick = { showClearDialog = true },
                            enabled = logs.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = stringResource(Res.string.debug_clear)
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // 滚动到底部按钮
            AnimatedVisibility(
                visible = showScrollToBottom && !isSelectionMode,
                enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)) + 
                        scaleIn(spring(stiffness = Spring.StiffnessMedium)) +
                        slideInVertically(spring(stiffness = Spring.StiffnessMedium)) { it },
                exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) + 
                       scaleOut(spring(stiffness = Spring.StiffnessMedium)) +
                       slideOutVertically(spring(stiffness = Spring.StiffnessMedium)) { it }
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        scope.launch {
                            listState.animateScrollToItem(logs.size - 1)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.KeyboardDoubleArrowDown,
                            contentDescription = null
                        )
                    },
                    text = { Text(stringResource(Res.string.debug_scroll_to_bottom)) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    ) { padding ->
        if (logs.isEmpty()) {
            // 空状态 - MD3 Expressive 风格
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BugReport,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                    Text(
                        text = stringResource(Res.string.debug_empty_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(Res.string.debug_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    itemsIndexed(logs, key = { index, entry -> "${index}_${entry.timestampMillis}" }) { index, entry ->
                        val isSelected = selectedIndices.contains(index)
                        
                        LogEntryItem(
                            entry = entry,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onContextMenu = { enterSelectionMode(index) },
                            onClick = {
                                if (isSelectionMode) {
                                    selectedIndices = if (isSelected) {
                                        selectedIndices - index
                                    } else {
                                        selectedIndices + index
                                    }
                                    if (selectedIndices.isEmpty()) {
                                        isSelectionMode = false
                                    }
                                }
                            },
                            onCheckedChange = { checked ->
                                selectedIndices = if (checked) {
                                    selectedIndices + index
                                } else {
                                    selectedIndices - index
                                }
                                if (selectedIndices.isEmpty()) {
                                    isSelectionMode = false
                                }
                            }
                        )
                    }
                    
                    // 底部间距
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LogEntryItem(
    entry: LogEntry,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onContextMenu: () -> Unit,
    onClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.surface
    }
    
    val levelColor = when (entry.level) {
        LogLevel.DEBUG -> MaterialTheme.colorScheme.outline
        LogLevel.INFO -> MaterialTheme.colorScheme.primary
        LogLevel.WARN -> MaterialTheme.colorScheme.tertiary
        LogLevel.ERROR -> MaterialTheme.colorScheme.error
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            // 移动端长按、桌面端右键
            .combinedClickable(
                onClick = onClick,
                onLongClick = onContextMenu
            )
            .onRightClick(onContextMenu)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 多选模式下显示复选框
        AnimatedVisibility(visible = isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            // 时间和级别行
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 级别标签 - MD3 风格
                Text(
                    text = entry.level.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = levelColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(levelColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
                
                // 标签
                Text(
                    text = entry.tag,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 时间
                Text(
                    text = com.guang.misty.util.formatTimestamp(entry.timestampMillis),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // 消息内容
            Text(
                text = entry.message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp
            )
            
            // 异常堆栈
            entry.throwable?.let { throwable ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = throwable.stackTraceToString(),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 15.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                        .padding(8.dp)
                )
            }
        }
    }
}
