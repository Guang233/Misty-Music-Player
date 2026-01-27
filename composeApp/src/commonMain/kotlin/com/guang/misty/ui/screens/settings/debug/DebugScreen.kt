package com.guang.misty.ui.screens.settings.debug

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.guang.misty.util.LogStore

@Composable
fun DebugScreen() {
    val logs by LogStore.logs.collectAsState()

    LazyColumn {
        items(logs) { entry ->
            Text(entry.format())
        }
    }
}