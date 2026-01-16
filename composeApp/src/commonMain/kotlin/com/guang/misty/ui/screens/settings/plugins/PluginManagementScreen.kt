package com.guang.misty.ui.screens.settings.plugins

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guang.misty.util.BackHandler
import misty.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * 格式化插件错误消息（多语言支持）
 */
@Composable
private fun formatPluginError(error: PluginError): String {
    return when (error) {
        is PluginError.FileNotFound -> stringResource(Res.string.plugin_file_not_found)
        is PluginError.LoadFailed -> stringResource(Res.string.plugin_load_failed, error.message)
        is PluginError.SaveFailed -> stringResource(Res.string.plugin_save_failed)
        is PluginError.ImportFailed -> stringResource(Res.string.plugin_import_failed, error.message)
        is PluginError.DeleteFailed -> stringResource(Res.string.plugin_delete_failed, error.message)
        is PluginError.IdNotFound -> stringResource(Res.string.plugin_id_not_found)
        is PluginError.LoadListFailed -> stringResource(Res.string.plugin_load_list_failed, error.message)
    }
}

/**
 * 插件管理页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginManagementScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PluginManagerViewModel = viewModel { PluginManagerViewModel() }
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    // 删除确认对话框状态
    var pluginToDelete by remember { mutableStateOf<PluginUiState?>(null) }
    
    // 判断是否有 dialog 打开
    val hasDialogOpen = state.importDialogVisible || pluginToDelete != null
    
    // 处理返回键：优先关闭 dialog，否则返回上一页
    BackHandler(enabled = true) {
        when {
            pluginToDelete != null -> pluginToDelete = null
            state.importDialogVisible && !state.isImporting -> viewModel.hideImportDialog()
            !hasDialogOpen -> onNavigateBack()
        }
    }
    
    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { 
                    Text(
                        text = stringResource(Res.string.plugin_management_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back)
                        )
                    }
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
                onClick = { viewModel.showImportDialog() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(Res.string.plugin_import)) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        },
        contentWindowInsets = WindowInsets(0),
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                // 加载中
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (state.plugins.isEmpty()) {
                // 空状态
                EmptyPluginsContent(
                    onImportClick = { viewModel.showImportDialog() },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                // 插件列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 100.dp // 为 FAB 留出空间
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = state.plugins,
                        key = { it.id }
                    ) { plugin ->
                        PluginCard(
                            plugin = plugin,
                            onToggleEnabled = { viewModel.togglePluginEnabled(plugin.id) },
                            onDelete = { pluginToDelete = plugin }
                        )
                    }
                }
            }
            
            // 错误提示
            AnimatedVisibility(
                visible = state.error != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                state.error?.let { error ->
                    Snackbar(
                        action = {
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text(stringResource(Res.string.action_close))
                            }
                        }
                    ) {
                        Text(formatPluginError(error))
                    }
                }
            }
        }
    }
    
    // 导入插件对话框
    if (state.importDialogVisible) {
        ImportPluginDialog(
            importText = state.importText,
            isImporting = state.isImporting,
            onTextChange = { viewModel.updateImportText(it) },
            onConfirm = { viewModel.importPluginFromText(state.importText) },
            onDismiss = { viewModel.hideImportDialog() }
        )
    }
    
    // 删除确认对话框
    pluginToDelete?.let { plugin ->
        DeletePluginDialog(
            plugin = plugin,
            onConfirm = {
                viewModel.deletePlugin(plugin.id)
                pluginToDelete = null
            },
            onDismiss = { pluginToDelete = null }
        )
    }
}

/**
 * 空状态内容
 */
@Composable
private fun EmptyPluginsContent(
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Extension,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        
        Text(
            text = stringResource(Res.string.plugin_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = stringResource(Res.string.plugin_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        FilledTonalButton(
            onClick = onImportClick,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(Res.string.plugin_import))
        }
    }
}

/**
 * 插件卡片
 */
@Composable
private fun PluginCard(
    plugin: PluginUiState,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 插件图标
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (plugin.enabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (plugin.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Extension,
                                contentDescription = null,
                                tint = if (plugin.enabled) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                
                // 插件信息
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plugin.meta?.name ?: plugin.id,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (plugin.meta != null) {
                        Text(
                            text = buildString {
                                append("v${plugin.meta.version ?: "1.0.0"}")
                                plugin.meta.author?.let { append(" · $it") }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (plugin.error != null) {
                        Text(
                            text = formatPluginError(plugin.error),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                // 启用开关
                Switch(
                    checked = plugin.enabled,
                    onCheckedChange = { onToggleEnabled() }
                )
            }
            
            // 插件描述和功能
            if (plugin.meta != null && plugin.enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 描述
                plugin.meta.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // 功能标签
                if (plugin.meta.capabilities.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        plugin.meta.capabilities.take(4).forEach { capability ->
                            CapabilityChip(capability = capability.name)
                        }
                        if (plugin.meta.capabilities.size > 4) {
                            Text(
                                text = "+${plugin.meta.capabilities.size - 4}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(Res.string.action_delete))
                    }
                }
            }
        }
    }
}

/**
 * 功能标签
 */
@Composable
private fun CapabilityChip(
    capability: String,
    modifier: Modifier = Modifier
) {
    val displayName = when (capability) {
        "SEARCH" -> stringResource(Res.string.plugin_capability_search)
        "PLAYLIST" -> stringResource(Res.string.plugin_capability_playlist)
        "ALBUM" -> stringResource(Res.string.plugin_capability_album)
        "LYRICS" -> stringResource(Res.string.plugin_capability_lyrics)
        "AUDIO_RESOURCES" -> stringResource(Res.string.plugin_capability_audio)
        else -> capability
    }
    
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        modifier = modifier
    ) {
        Text(
            text = displayName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * 导入插件对话框
 */
@Composable
private fun ImportPluginDialog(
    importText: String,
    isImporting: Boolean,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isImporting) onDismiss() },
        shape = RoundedCornerShape(28.dp),
        icon = {
            Icon(
                imageVector = Icons.Outlined.Code,
                contentDescription = null
            )
        },
        title = {
            Text(
                text = stringResource(Res.string.plugin_import_title),
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.plugin_import_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = importText,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    enabled = !isImporting,
                    placeholder = { 
                        Text(
                            stringResource(Res.string.plugin_import_placeholder),
                            style = MaterialTheme.typography.bodySmall
                        ) 
                    },
                    shape = RoundedCornerShape(16.dp),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(Res.string.plugin_import_warning),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = importText.isNotBlank() && !isImporting
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(Res.string.plugin_import))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isImporting
            ) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}

/**
 * 删除插件确认对话框
 */
@Composable
private fun DeletePluginDialog(
    plugin: PluginUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val pluginName = plugin.meta?.name ?: plugin.id
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = stringResource(Res.string.plugin_delete_title),
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = stringResource(Res.string.plugin_delete_confirm, pluginName),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(Res.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}
