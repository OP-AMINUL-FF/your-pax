package com.yourpax.app.ui.screens.netkb

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourpax.app.data.api.models.NetKBResponse
import com.yourpax.app.data.repository.NetworkRepository
import com.yourpax.app.ui.components.DemoModeBanner
import com.yourpax.app.ui.components.EmptyState
import com.yourpax.app.ui.components.LoadingOverlay
import com.yourpax.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetKBScreen() {
    val repo = remember { NetworkRepository() }
    val scope = rememberCoroutineScope()
    var kbData by remember { mutableStateOf<NetKBResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showToolbar by remember { mutableStateOf(true) }
    var fontSize by remember { mutableFloatStateOf(12f) }

    LaunchedEffect(Unit) {
        kbData = repo.getNetKBData().getOrNull()
        isLoading = false
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(10000)
            kbData = repo.getNetKBData().getOrNull()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (showToolbar) {
            DemoModeBanner()
            TopAppBar(
                title = { Text("Network Knowledge Base", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            kbData = repo.getNetKBData().getOrNull()
                        }
                    }) { Icon(Icons.Default.Refresh, contentDescription = "Refresh") }
                    IconButton(onClick = { showToolbar = false }) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "Hide Toolbar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        } else {
            Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = {
                        scope.launch { kbData = repo.getNetKBData().getOrNull() }
                    }) { Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = SubtleText) }
                    IconButton(onClick = { showToolbar = true }) {
                        Icon(Icons.Default.FullscreenExit, contentDescription = "Show Toolbar", tint = SubtleText)
                    }
                }
            }
        }

        if (isLoading) {
            LoadingOverlay()
        } else if (kbData == null || kbData!!.rows.isEmpty()) {
            EmptyState(title = "Knowledge Base is empty", subtitle = "Discovered hosts will appear here after a scan")
        } else {
            val data = kbData!!
            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                // Controls bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${data.rows.size} entries", style = MaterialTheme.typography.labelSmall, color = SubtleText)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { fontSize = maxOf(6f, fontSize - 1f) },
                            modifier = Modifier.size(28.dp)
                        ) { Icon(Icons.Default.TextDecrease, contentDescription = "Decrease font", modifier = Modifier.size(16.dp), tint = SubtleText) }
                        Text("${fontSize.toInt()}px", style = MaterialTheme.typography.labelSmall, color = SubtleText, modifier = Modifier.align(Alignment.CenterVertically))
                        IconButton(
                            onClick = { fontSize = minOf(24f, fontSize + 1f) },
                            modifier = Modifier.size(28.dp)
                        ) { Icon(Icons.Default.TextIncrease, contentDescription = "Increase font", modifier = Modifier.size(16.dp), tint = SubtleText) }
                    }
                }

                // Data table
                val scrollState = rememberScrollState()
                Column(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
                    // Header row
                    Row {
                        data.headers.forEachIndexed { _, header ->
                            Box(
                                modifier = Modifier
                                    .border(0.5.dp, DividerColor)
                                    .background(Primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .defaultMinSize(minWidth = 120.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    header,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = fontSize.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Primary
                                )
                            }
                        }
                    }

                    // Data rows
                    data.rows.forEach { row ->
                        val isBlueRow = row.size > 3 && row[3] == "0"
                        Row {
                            row.forEachIndexed { _, cell ->
                                val isEmpty = cell.isBlank()
                                Box(
                                    modifier = Modifier
                                        .border(0.5.dp, DividerColor)
                                        .background(
                                            when {
                                                isBlueRow -> Primary.copy(alpha = 0.05f)
                                                cell.contains("success") -> Success.copy(alpha = 0.12f)
                                                cell.contains("failed") -> Error.copy(alpha = 0.12f)
                                                else -> MaterialTheme.colorScheme.surface
                                            }
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .defaultMinSize(minWidth = 120.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        cell.ifEmpty { "-" },
                                        fontSize = fontSize.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = when {
                                            isEmpty -> SubtleText
                                            cell.contains("success") -> Success
                                            cell.contains("failed") -> Error
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
