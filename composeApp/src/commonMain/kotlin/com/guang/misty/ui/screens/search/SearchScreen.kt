package com.guang.misty.ui.screens.search

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.guang.misty.model.MistySong
import com.guang.misty.service.LoadedPlugin
import com.guang.misty.ui.navigation.LocalBottomBarHeight
import com.guang.misty.util.BackHandler
import misty.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * 搜索页面 - MD3 Expressive 风格
 * 双模式设计：结果浏览模式 + 搜索输入模式
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToResult: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel { SearchViewModel() }
) {
    val state by viewModel.state.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearchMode by remember { mutableStateOf(false) }
    
    // 获取底部栏高度
    val bottomBarHeight = LocalBottomBarHeight.current
    
    // 顶栏滚动折叠行为
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    
    // 判断是否有搜索结果
    val hasResults = state.searchResults.isNotEmpty()
    val showResults = hasResults || state.isSearching || state.searchError != null
    
    // 是否有可用插件
    val hasPlugins = state.availableSources.isNotEmpty()
    
    // 当前搜索的关键词
    val currentKeyword = state.searchQuery.ifBlank { null }
    
    Box(modifier = modifier.fillMaxSize()) {
        // 主内容：结果浏览模式
    Scaffold(
        topBar = {
                // 紧凑的顶栏，包含搜索入口
                TopAppBar(
                title = { 
                        // 可点击的搜索入口
                        CompactSearchBar(
                            currentKeyword = currentKeyword,
                            hasPlugins = hasPlugins,
                            onClick = { 
                                if (hasPlugins) {
                                    searchQuery = currentKeyword ?: ""
                                    isSearchMode = true
                                }
                            }
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
            contentWindowInsets = WindowInsets(0),
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
                // 源选择器（可折叠）
                AnimatedVisibility(
                    visible = hasPlugins,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SourceSelector(
                        sources = state.availableSources,
                        selectedSourceId = state.selectedSourceId,
                        onSourceSelected = { viewModel.selectSource(it) },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                // 无插件提示
                if (!hasPlugins && !state.isLoadingSources) {
                    NoPluginHint(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
                
                // 内容区域
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = bottomBarHeight + 16.dp)
                ) {
                    if (showResults && hasPlugins) {
                        // 搜索结果
                        itemsIndexed(
                            items = state.searchResults,
                            key = { index, song -> "${song.globalId}_$index" }
                        ) { _, song ->
                            SongListItem(
                                song = song,
                                onClick = { viewModel.playSong(song) },
                                onMenuClick = { /* TODO: 显示菜单 */ }
                            )
                        }
                        
                        // 加载中
                        if (state.isSearching) {
                            item(key = "loading") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                        
                        // 错误
                        if (state.searchError != null && state.searchResults.isEmpty()) {
                            item(key = "error") {
                                SearchErrorState(
                                    error = state.searchError!!,
                                    modifier = Modifier.fillMaxWidth().padding(32.dp)
                                )
                            }
                        }
                        
                        // 空结果
                        if (state.searchResults.isEmpty() && !state.isSearching && state.searchError == null) {
                            item(key = "empty") {
                                SearchEmptyState(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp)
                                )
                    }
                }
                        
                        // 没有更多
                        if (!state.hasMoreResults && state.searchResults.isNotEmpty()) {
                            item(key = "no_more") {
                                Text(
                                    text = stringResource(Res.string.search_no_more),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .wrapContentWidth(Alignment.CenterHorizontally)
                                )
                            }
                        }
                        
                        // 加载更多触发器
                        if (state.searchResults.isNotEmpty() && !state.isSearching && state.hasMoreResults) {
                            item(key = "load_more_trigger") {
                                LaunchedEffect(Unit) {
                                    viewModel.loadMore()
                                }
                            }
                        }
                    } else if (!showResults && hasPlugins) {
                        // 初始状态：提示用户开始搜索
                        item(key = "initial_hint") {
                            InitialSearchHint(
                                onStartSearch = { isSearchMode = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp)
                            )
                        }
                        
                        // 搜索历史
                        if (state.searchHistory.isNotEmpty()) {
                            item(key = "history") {
                        SearchHistorySection(
                                    history = state.searchHistory,
                            onHistoryClick = { 
                                        viewModel.search(it)
                                    },
                                    onClearHistory = { viewModel.clearHistory() }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // 全屏搜索模式覆盖层
        AnimatedVisibility(
            visible = isSearchMode,
            enter = fadeIn(animationSpec = tween(200)) + expandVertically(
                animationSpec = tween(250),
                expandFrom = Alignment.Top
            ),
            exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(
                animationSpec = tween(200),
                shrinkTowards = Alignment.Top
            )
        ) {
            SearchOverlay(
                initialQuery = searchQuery,
                onQueryChange = { query ->
                    searchQuery = query
                    viewModel.fetchSuggestions(query)
                },
                onSearch = { query ->
                    if (query.isNotBlank()) {
                        viewModel.search(query)
                        viewModel.clearSuggestions()
                        isSearchMode = false
                    }
                },
                onDismiss = { 
                    viewModel.clearSuggestions()
                    isSearchMode = false 
                },
                searchHistory = state.searchHistory,
                onHistoryClick = { keyword ->
                    viewModel.search(keyword)
                    viewModel.clearSuggestions()
                    isSearchMode = false
                },
                onHistoryDelete = { keyword ->
                    viewModel.deleteHistoryItem(keyword)
                },
                suggestions = state.suggestions,
                isLoadingSuggestions = state.isLoadingSuggestions,
                onSuggestionClick = { suggestion ->
                    viewModel.search(suggestion)
                    viewModel.clearSuggestions()
                    isSearchMode = false
                }
                        )
                    }
                }
}

/**
 * 紧凑的搜索栏入口
 */
@Composable
private fun CompactSearchBar(
    currentKeyword: String?,
    hasPlugins: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 8.dp) // 右侧留出间距
            .height(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
            alpha = if (hasPlugins) 1f else 0.6f
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (hasPlugins) 1f else 0.5f
                ),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = currentKeyword ?: if (hasPlugins) {
                    stringResource(Res.string.search_hint)
                } else {
                    stringResource(Res.string.search_no_plugin_hint)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = if (currentKeyword != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (hasPlugins) 0.7f else 0.5f
                    )
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 全屏搜索覆盖层
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchOverlay(
    initialQuery: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    searchHistory: List<String>,
    onHistoryClick: (String) -> Unit,
    onHistoryDelete: (String) -> Unit,
    suggestions: List<String>,
    isLoadingSuggestions: Boolean,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    
    // 使用 TextFieldValue 来控制光标位置
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialQuery,
                selection = TextRange(initialQuery.length) // 光标在末尾
            )
        )
    }
    
    // 同步外部状态变化
    LaunchedEffect(initialQuery) {
        if (textFieldValue.text != initialQuery) {
            textFieldValue = TextFieldValue(
                text = initialQuery,
                selection = TextRange(initialQuery.length)
            )
        }
    }
    
    // 自动聚焦输入框
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    // 返回键处理
    BackHandler(enabled = true) {
        onDismiss()
    }
    
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 搜索输入栏
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back)
                        )
                    }
                },
                title = {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            onQueryChange(newValue.text)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                onSearch(textFieldValue.text)
                            }
                        ),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (textFieldValue.text.isEmpty()) {
                    Text(
                                        text = stringResource(Res.string.search_hint),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                },
                actions = {
                    AnimatedVisibility(
                        visible = textFieldValue.text.isNotEmpty(),
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        IconButton(onClick = { 
                            textFieldValue = TextFieldValue("", TextRange.Zero)
                            onQueryChange("")
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(Res.string.action_clear)
                            )
                        }
                    }
                    IconButton(
                        onClick = { onSearch(textFieldValue.text) },
                        enabled = textFieldValue.text.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(Res.string.action_search),
                            tint = if (textFieldValue.text.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            )
                        }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
            
            HorizontalDivider()
            
            // 联想词或搜索历史
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // 联想词加载指示器
                if (isLoadingSuggestions && textFieldValue.text.isNotBlank()) {
                    item(key = "suggestions_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
                
                // 联想词列表（仅当有输入且有联想词时显示）
                if (suggestions.isNotEmpty() && textFieldValue.text.isNotBlank()) {
                    item(key = "suggestions_header") {
                        Text(
                            text = stringResource(Res.string.search_suggestions),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    
                    items(
                        items = suggestions,
                        key = { "suggestion_$it" }
                    ) { suggestion ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = suggestion,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickable { onSuggestionClick(suggestion) }
                        )
                    }
                }
                
                // 搜索历史（当没有输入或没有联想词时显示）
                if (searchHistory.isNotEmpty() && (textFieldValue.text.isBlank() || suggestions.isEmpty())) {
                    item(key = "history_header") {
                        Text(
                            text = stringResource(Res.string.search_history),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    
                    items(
                        items = searchHistory,
                        key = { "history_$it" }
                    ) { keyword ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = keyword,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = { onHistoryDelete(keyword) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(Res.string.action_delete),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            modifier = Modifier.clickable { onHistoryClick(keyword) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 初始搜索提示
 */
@Composable
private fun InitialSearchHint(
    onStartSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = stringResource(Res.string.search_initial_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(Res.string.search_initial_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FilledTonalButton(
            onClick = onStartSearch,
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(Res.string.search_start))
        }
    }
}

/**
 * 没有可用插件的提示
 */
@Composable
private fun NoPluginHint(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            Icon(
                imageVector = Icons.Outlined.Extension,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(Res.string.search_no_plugin_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 源选择器 - MD3 Expressive 风格
 */
@Composable
private fun SourceSelector(
    sources: List<LoadedPlugin>,
    selectedSourceId: String?,
    onSourceSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier
    ) {
        items(sources, key = { it.id }) { source ->
            val isSelected = source.id == selectedSourceId
            FilterChip(
                selected = isSelected,
                onClick = { onSourceSelected(source.id) },
                label = { 
                    Text(
                        source.meta.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                    enabled = true,
                    selected = isSelected
                )
            )
        }
    }
}

/**
 * 搜索历史区块
 */
@Composable
private fun SearchHistorySection(
    history: List<String>,
    onHistoryClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(Res.string.search_history),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = onClearHistory,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    stringResource(Res.string.search_clear_history),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            history.forEach { keyword ->
                SuggestionChip(
                    onClick = { onHistoryClick(keyword) },
                    label = { 
                        Text(
                            keyword,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        enabled = true
                    )
                )
            }
        }
    }
}

/**
 * 歌曲列表项
 */
@Composable
private fun SongListItem(
    song: MistySong,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                val url = song.album?.coverUrl ?: song.coverUrl ?: ""
                if (url.isNotEmpty()) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = song.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artists.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(Res.string.action_more),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 搜索错误状态
 */
@Composable
private fun SearchErrorState(
    error: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = stringResource(Res.string.search_error),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 搜索空结果状态
 */
@Composable
private fun SearchEmptyState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = stringResource(Res.string.search_empty),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(Res.string.search_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
