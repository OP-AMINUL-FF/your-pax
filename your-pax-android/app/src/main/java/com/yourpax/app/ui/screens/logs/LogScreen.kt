package com.yourpax.app.ui.screens.logs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.yourpax.app.data.repository.LootRepository
import com.yourpax.app.ui.components.DemoModeBanner
import com.yourpax.app.ui.components.EmptyState
import com.yourpax.app.ui.components.LoadingOverlay
import com.yourpax.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LogScreen() {
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Console Logs", style = MaterialTheme.typography.titleLarge)
                Row {
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
                    }) { Icon(imageVector = if (isPolling) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = if (isPolling) "Stop" else "Start", tint = if (isPolling) Error else Success) }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (isLoading) {
                LoadingOverlay()
            } else {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    LazyColumn(modifier = Modifier.fillMaxWidth().padding(12.dp), state = listState) {
                        if (logLines.isEmpty()) {
                            item { EmptyState(title = "No logs yet", subtitle = "Tap play to start polling") }
                        }
                        items(logLines.takeLast(500)) { line ->
                            Text(text = line, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = SubtleText)
                        }
                    }
                }
            }
        }
    }
}
