package com.guang.misty.ui.screens.search

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guang.misty.ui.navigation.LocalBottomBarHeight
import misty.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * 搜索页面 - MD3 Expressive 风格
 * 
 * 特点：
 * - MediumTopAppBar 可折叠标题
 * - 圆润的搜索栏和芯片
 * - 宽松的间距
 * - 表现力丰富的交互
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    
    // TODO: 从 ViewModel 获取
    val searchHistory = remember { mutableStateListOf("周杰伦", "林俊杰", "晴天", "五月天") }
    val availableSources = remember { listOf("网易云", "QQ音乐", "酷狗", "酷我") }
    var selectedSource by remember { mutableStateOf(availableSources.firstOrNull() ?: "") }
    
    // 获取底部栏高度
    val bottomBarHeight = LocalBottomBarHeight.current
    
    // 顶栏滚动折叠行为
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { 
                    Text(
                        text = stringResource(Res.string.nav_search),
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
        contentWindowInsets = WindowInsets(0), // 让内容延伸到系统栏
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 搜索栏
            SearchBarSection(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                isActive = isSearchActive,
                onActiveChange = { isSearchActive = it },
                onSearch = { query ->
                    if (query.isNotBlank()) {
                        onSearch(query)
                        if (!searchHistory.contains(query)) {
                            searchHistory.add(0, query)
                            if (searchHistory.size > 10) {
                                searchHistory.removeLast()
                            }
                        }
                        isSearchActive = false
                    }
                }
            )
            
            // 源选择器
            SourceSelector(
                sources = availableSources,
                selectedSource = selectedSource,
                onSourceSelected = { selectedSource = it },
                modifier = Modifier.padding(top = 8.dp)
            )
            
            // 搜索历史（可滚动区域）
            if (searchHistory.isNotEmpty() && !isSearchActive) {
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = bottomBarHeight + 4.dp)
                ) {
                    item {
                        SearchHistorySection(
                            history = searchHistory,
                            onHistoryClick = { 
                                searchQuery = it
                                onSearch(it)
                            },
                            onClearHistory = { searchHistory.clear() }
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(bottomBarHeight + 4.dp))
            }
        }
    }
}

/**
 * 搜索栏区块 - MD3 Expressive 风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarSection(
    query: String,
    onQueryChange: (String) -> Unit,
    isActive: Boolean,
    onActiveChange: (Boolean) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { onSearch(query) },
                expanded = isActive,
                onExpandedChange = onActiveChange,
                placeholder = { 
                    Text(
                        stringResource(Res.string.search_hint),
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Search, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = query.isNotEmpty(),
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                Icons.Default.Close, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        },
        expanded = isActive,
        onExpandedChange = onActiveChange,
        shape = RoundedCornerShape(28.dp),
        colors = SearchBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            dividerColor = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // 搜索建议内容（展开时显示）
        // TODO: 实现搜索建议
    }
}

/**
 * 源选择器 - MD3 Expressive 风格
 */
@Composable
private fun SourceSelector(
    sources: List<String>,
    selectedSource: String,
    onSourceSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier
    ) {
        items(sources) { source ->
            val isSelected = source == selectedSource
            FilterChip(
                selected = isSelected,
                onClick = { onSourceSelected(source) },
                label = { 
                    Text(
                        source,
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
 * 搜索历史区块 - MD3 Expressive 风格
 */
@Composable
private fun SearchHistorySection(
    history: List<String>,
    onHistoryClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        // 标题行
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
        
        // 历史记录芯片
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
 * 搜索结果页面（待实现）
 */
@Composable
fun SearchResultScreen(
    query: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: 实现搜索结果页面
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "搜索结果: $query",
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
