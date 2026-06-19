package com.yourpax.app.ui.screens.netkb

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.yourpax.app.ui.components.FontSizeControl
import com.yourpax.app.ui.components.LoadingOverlay
import com.yourpax.app.ui.theme.rememberAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetKBScreen(
    onOpenDrawer: () -> Unit = {}
) {
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

    val appColors = rememberAppColors()

    Column(modifier = Modifier.fillMaxSize()) {
        if (showToolbar) {
            DemoModeBanner()
            TopAppBar(
                title = { Text("Net Knowledge Base", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { kbData = repo.getNetKBData().getOrNull(); isLoading = false } }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
                    }) { Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = appColors.subtleText) }
                    IconButton(onClick = { showToolbar = true }) {
                        Icon(Icons.Default.FullscreenExit, contentDescription = "Show Toolbar", tint = appColors.subtleText)
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${data.rows.size} entries", style = MaterialTheme.typography.labelSmall, color = appColors.subtleText)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FontSizeControl(
                            currentSize = fontSize,
                            onDecrease = { fontSize = maxOf(6f, fontSize - 1f) },
                            onIncrease = { fontSize = minOf(24f, fontSize + 1f) },
                            minSize = 6f
                        )
                    }
                }

                val scrollState = rememberScrollState()
                Column(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
                    Row {
                        data.headers.forEachIndexed { _, header ->
                            Box(
                                modifier = Modifier
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                    .background(appColors.infoContainer)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .defaultMinSize(minWidth = 120.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    header,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = fontSize.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    data.rows.forEach { row ->
                        val isBlueRow = row.size > 3 && row[3] == "0"
                        Row {
                            row.forEachIndexed { _, cell ->
                                val isEmpty = cell.isBlank()
                                Box(
                                    modifier = Modifier
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                        .background(
                                            when {
                                                isBlueRow -> appColors.infoContainer.copy(alpha = 0.5f)
                                                cell.contains("success") -> appColors.successContainer
                                                cell.contains("failed") -> appColors.warningContainer
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
                                            isEmpty -> appColors.subtleText
                                            cell.contains("success") -> appColors.success
                                            cell.contains("failed") -> MaterialTheme.colorScheme.error
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
