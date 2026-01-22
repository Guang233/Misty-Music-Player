package com.guang.misty.ui.screens.settings.plugins

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guang.misty.util.BackHandler
import com.guang.misty.util.FilePicker
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
    
    // Snackbar 状态
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 删除确认对话框状态
    var pluginToDelete by remember { mutableStateOf<PluginUiState?>(null) }
    
    // 文件选择器状态
    var showFilePicker by remember { mutableStateOf(false) }
    
    // 判断是否有 dialog 打开
    val hasDialogOpen = state.importDialogVisible || pluginToDelete != null || state.editingPluginId != null
    
    // 处理返回键：优先关闭 dialog，否则返回上一页
    BackHandler(enabled = true) {
        when {
            pluginToDelete != null -> pluginToDelete = null
            state.editingPluginId != null && !state.isSavingCode -> viewModel.closePluginEditor()
            state.importDialogVisible && !state.isImporting -> viewModel.hideImportDialog()
            !hasDialogOpen -> onNavigateBack()
        }
    }
    
    // 显示错误消息
    val errorMessage = state.error?.let { formatPluginError(it) }
    LaunchedEffect(state.error) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }
    
    // 文件选择器
    FilePicker(
        show = showFilePicker,
        fileExtensions = listOf("js"),
        onResult = { result ->
            showFilePicker = false
            if (result != null) {
                // 将文件内容填入导入框并自动导入
                viewModel.updateImportText(result.content)
                viewModel.importPluginFromText(result.content)
            }
        }
    )
    
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
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
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
            // 加载中状态 - 带淡入淡出动画
            AnimatedVisibility(
                visible = state.isLoading,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(200)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                CircularProgressIndicator()
            }
            
            // 空状态 - 带缩放淡入动画
            AnimatedVisibility(
                visible = !state.isLoading && state.plugins.isEmpty(),
                enter = fadeIn(animationSpec = tween(400)) + scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ),
                exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.9f),
                modifier = Modifier.align(Alignment.Center)
            ) {
                EmptyPluginsContent(
                    onImportClick = { viewModel.showImportDialog() }
                )
            }
            
            // 插件列表 - 带淡入动画
            AnimatedVisibility(
                visible = !state.isLoading && state.plugins.isNotEmpty(),
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
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
                    itemsIndexed(
                        items = state.plugins,
                        key = { _, plugin -> plugin.id }
                    ) { index, plugin ->
                        // 每个列表项的交错进入动画
                        AnimatedPluginCard(
                            plugin = plugin,
                            index = index,
                            onClick = { viewModel.openPluginEditor(plugin.id) },
                            onToggleEnabled = { viewModel.togglePluginEnabled(plugin.id) },
                            onDelete = { pluginToDelete = plugin },
                            onLoginClick = if (plugin.meta?.auth != null) {
                                { viewModel.showLoginDialog(plugin.id) }
                            } else null
                        )
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
            onDismiss = { viewModel.hideImportDialog() },
            onSelectFile = {
                viewModel.hideImportDialog()
                showFilePicker = true
            }
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
    
    // 插件编辑对话框
    state.editingPluginId?.let { pluginId ->
        val plugin = state.plugins.find { it.id == pluginId }
        if (plugin != null) {
            EditPluginDialog(
                plugin = plugin,
                code = state.editingPluginCode,
                isLoading = state.isLoadingCode,
                isSaving = state.isSavingCode,
                onCodeChange = { viewModel.updateEditingCode(it) },
                onSave = { viewModel.savePluginCode() },
                onDismiss = { viewModel.closePluginEditor() }
            )
        }
    }

    // 登录对话框 (Android 平台特定)
    if (state.loginDialogVisible && state.loginPluginId != null && state.loginUrl != null) {
        PlatformLoginDialog(
            pluginId = state.loginPluginId!!,
            pluginName = state.loginPluginName ?: state.loginPluginId!!,
            loginUrl = state.loginUrl!!,
            onDismiss = { viewModel.hideLoginDialog() },
            onLoginSuccess = { cookies ->
                viewModel.onLoginSuccess(cookies)
            }
        )
    }
}

/**
 * 平台特定的登录对话框
 * Android: WebView 登录
 * Desktop: 显示提示信息（由 LoginHandler 处理）
 */
@Composable
expect fun PlatformLoginDialog(
    pluginId: String,
    pluginName: String,
    loginUrl: String,
    onDismiss: () -> Unit,
    onLoginSuccess: (List<com.guang.misty.engine.cookie.MistyCookie>) -> Unit
)

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
 * 带动画的插件卡片包装器
 */
@Composable
private fun AnimatedPluginCard(
    plugin: PluginUiState,
    index: Int,
    onClick: () -> Unit,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit,
    onLoginClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // 交错动画延迟
    val animationDelay = (index * 50).coerceAtMost(300)
    
    // 进入动画状态
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(animationDelay.toLong())
        isVisible = true
    }
    
    // 动画值
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        )
    )
    
    val animatedTranslationY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 30f,
        animationSpec = tween(
            durationMillis = 350,
            easing = FastOutSlowInEasing
        )
    )
    
    PluginCard(
        plugin = plugin,
        onClick = onClick,
        onToggleEnabled = onToggleEnabled,
        onDelete = onDelete,
        onLoginClick = onLoginClick,
        modifier = modifier.graphicsLayer {
            alpha = animatedAlpha
            translationY = animatedTranslationY
        }
    )
}

/**
 * 插件卡片
 */
@Composable
private fun PluginCard(
    plugin: PluginUiState,
    onClick: () -> Unit,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit,
    onLoginClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // 图标颜色动画
    val iconContainerColor by animateColorAsState(
        targetValue = if (plugin.enabled) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(300)
    )
    
    val iconTint by animateColorAsState(
        targetValue = if (plugin.enabled) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(300)
    )
    
    Surface(
        onClick = onClick,
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
                // 插件图标 - 带颜色动画
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconContainerColor,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // 使用 Crossfade 在加载和图标之间切换
                        Crossfade(
                            targetState = plugin.isLoading,
                            animationSpec = tween(200)
                        ) { isLoading ->
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Extension,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
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
            
            // 插件描述和功能 - 带展开/折叠动画
            AnimatedVisibility(
                visible = plugin.meta != null && plugin.enabled,
                enter = expandVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(200, delayMillis = 100)),
                exit = shrinkVertically(
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(150))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 描述
                    plugin.meta?.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // 功能标签 - 带交错动画
                    if (plugin.meta?.capabilities?.isNotEmpty() == true) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            plugin.meta.capabilities.take(4).forEachIndexed { capIndex, capability ->
                                AnimatedCapabilityChip(
                                    capability = capability.name,
                                    delayMillis = capIndex * 50
                                )
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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 登录按钮（如果插件配置了 auth）
                        if (plugin.meta?.auth != null && onLoginClick != null) {
                            FilledTonalButton(
                                onClick = onLoginClick,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(Res.string.action_login))
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        // 删除按钮
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
}

/**
 * 带动画的功能标签
 */
@Composable
private fun AnimatedCapabilityChip(
    capability: String,
    delayMillis: Int = 0,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.toLong())
        isVisible = true
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    CapabilityChip(
        capability = capability,
        modifier = modifier.scale(scale)
    )
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
    onDismiss: () -> Unit,
    onSelectFile: () -> Unit
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
                // 从文件选择按钮
                OutlinedButton(
                    onClick = onSelectFile,
                    enabled = !isImporting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.plugin_import_from_file))
                }
                
                // 分隔线
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(Res.string.plugin_import_or),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }
                
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
                        .height(160.dp),
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

/**
 * 插件编辑对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPluginDialog(
    plugin: PluginUiState,
    code: String,
    isLoading: Boolean,
    isSaving: Boolean,
    onCodeChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val pluginName = plugin.meta?.name ?: plugin.id
    
    // 全屏对话框
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f),
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pluginName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (plugin.meta != null) {
                        Text(
                            text = "v${plugin.meta.version ?: "1.0.0"} · ${plugin.fileName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 300.dp, max = 500.dp)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    OutlinedTextField(
                        value = code,
                        onValueChange = onCodeChange,
                        modifier = Modifier.fillMaxSize(),
                        enabled = !isSaving,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = code.isNotBlank() && !isLoading && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(Res.string.action_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}
