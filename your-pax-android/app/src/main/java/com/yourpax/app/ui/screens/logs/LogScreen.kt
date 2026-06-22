package com.yourpax.app.ui.screens.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.yourpax.app.R
import com.yourpax.app.ui.components.CopyButton
import com.yourpax.app.ui.components.LottieAnim
import com.yourpax.app.data.repository.LootRepository
import com.yourpax.app.ui.components.DemoModeBanner
import com.yourpax.app.ui.components.EmptyState
import com.yourpax.app.ui.components.LoadingOverlay
import com.yourpax.app.ui.theme.rememberAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LogScreen(onOpenDrawer: () -> Unit = {}) {
    val appColors = rememberAppColors()
    val repo = remember { LootRepository() }
    val scope = rememberCoroutineScope()
    var logLines by remember { mutableStateOf<List<String>>(emptyList()) }
    var isPolling by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    suspend fun fetchLogs() {
        val r = repo.getLogs().getOrNull()
        if (r != null) {
            val lines = r.logs.split("\n").filter { it.isNotBlank() }
            logLines = lines
        }
        isLoading = false
    }

    LaunchedEffect(Unit) { fetchLogs() }

    Column(modifier = Modifier.fillMaxSize()) {
        DemoModeBanner()
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Open drawer")
                    }
                    Text("Console Logs", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(8.dp))
                    LottieAnim(
                        rawResId = R.raw.terminal_typing,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (logLines.isNotEmpty()) {
                        CopyButton(textToCopy = logLines.joinToString("\n"))
                        Spacer(Modifier.width(8.dp))
                    }
                    IconButton(onClick = { scope.launch { isLoading = true; fetchLogs() } }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = {
                        if (isPolling) {
                            isPolling = false
                        } else {
                            isPolling = true
                            scope.launch {
                                while (isPolling) {
                                    fetchLogs()
                                    if (logLines.isNotEmpty()) {
                                        listState.animateScrollToItem(logLines.size - 1)
                                    }
                                    delay(5000)
                                }
                            }
                        }
                    }) { Icon(imageVector = if (isPolling) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = if (isPolling) "Stop" else "Start", tint = if (isPolling) MaterialTheme.colorScheme.error else appColors.success) }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (isLoading) {
                LoadingOverlay()
            } else {
                Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = appColors.surfaceContainerLow, tonalElevation = 0.dp) {
                    LazyColumn(modifier = Modifier.fillMaxWidth().padding(12.dp), state = listState) {
                        if (logLines.isEmpty()) {
                            item { EmptyState(title = "No logs yet", subtitle = "Tap play to start polling") }
                        }
                        items(logLines.takeLast(500)) { line ->
                            Text(text = line, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = appColors.subtleText)
                        }
                    }
                }
            }
        }
    }
}
