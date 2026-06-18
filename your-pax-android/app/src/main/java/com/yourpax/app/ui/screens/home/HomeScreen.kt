package com.yourpax.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourpax.app.data.api.models.*
import com.yourpax.app.data.repository.*
import com.yourpax.app.ui.components.*
import com.yourpax.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onNavigateToStore: () -> Unit = {},
    onNavigateToConfig: () -> Unit = {},
    onNavigateToManual: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToNetwork: () -> Unit = {},
    onNavigateToMore: () -> Unit = {}
) {
    val lootRepo = remember { LootRepository() }
    val wifiRepo = remember { WiFiRepository() }
    val btRepo = remember { BluetoothRepository() }

    var storeData by remember { mutableStateOf<StoreDataFull?>(null) }
    var wifiStatus by remember { mutableStateOf<WiFiStatusResponse?>(null) }
    var btStatus by remember { mutableStateOf<BluetoothStatus?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("") }
    var logs by remember { mutableStateOf<List<String>>(emptyList()) }
    var showLogs by remember { mutableStateOf(false) }
    var logFontSize by remember { mutableFloatStateOf(12f) }
    val logScrollState = remember { LazyListState() }

    LaunchedEffect(Unit) {
        while (true) {
            storeData = lootRepo.getStoreData().getOrNull()
            wifiStatus = wifiRepo.getWifiStatus().getOrNull()
            btStatus = btRepo.getBluetoothStatus().getOrNull()
            isLoading = false
            delay(5000)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            lootRepo.getLogs().getOrNull()?.let { resp ->
                val lines = resp.logs.lines().filter { it.isNotBlank() }
                if (lines.isNotEmpty()) {
                    logs = lines
                    if (showLogs && logScrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == logs.size - 2) {
                        launch { logScrollState.scrollToItem(logs.size - 1) }
                    }
                }
            }
            delay(3000)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DemoModeBanner()

        btStatus?.let { bt ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
            ) {
                Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = if (bt.active) Success else Error, modifier = Modifier.size(14.dp))
                        Text(if (bt.active) "BT NAP: ${bt.ssid.ifEmpty { "your-pax" }}" else "BT: Inactive", style = MaterialTheme.typography.bodySmall, color = if (bt.active) Success else Error)
                    }
                    Text("|", color = SubtleText)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${bt.bridgeIp}:${bt.port}", style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                    Text("|", color = SubtleText)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.People, contentDescription = null, tint = Warning, modifier = Modifier.size(14.dp))
                        Text("Clients: ${bt.connectedClients}", style = MaterialTheme.typography.bodySmall, color = Warning)
                    }
                    Text("|", color = SubtleText)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = if (wifiStatus?.connected == true) Success else Error, modifier = Modifier.size(14.dp))
                        Text(if (wifiStatus?.connected == true) "WiFi: ${wifiStatus?.ssid}" else "WiFi: Not Connected", style = MaterialTheme.typography.bodySmall, color = if (wifiStatus?.connected == true) Success else Error)
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("Good to go", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Primary)
            }

            if (statusMessage.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Info.copy(alpha = 0.1f))) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Info, modifier = Modifier.size(14.dp))
                            Text(statusMessage, style = MaterialTheme.typography.bodySmall, color = Info)
                        }
                    }
                }
            }

            item {
                Text("Navigation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NavChip("Network", Icons.Default.Cloud, Primary, onClick = onNavigateToNetwork)
                    NavChip("Settings", Icons.Default.Settings, Warning, onClick = onNavigateToSettings)
                    NavChip("Store", Icons.Default.Store, Primary, onClick = onNavigateToStore)
                    NavChip("Config", Icons.Default.Code, Color(0xFF8833CC), onClick = onNavigateToConfig)
                    NavChip("Manual", Icons.Default.Terminal, Info, onClick = onNavigateToManual)
                    NavChip("Logs", Icons.AutoMirrored.Filled.Article, Secondary, onClick = onNavigateToLogs)
                    NavChip("More", Icons.Default.MoreHoriz, SubtleText, onClick = onNavigateToMore)
                }
            }

            item {
                Text("Live Stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Pair(storeData?.netkbCount ?: 0, "Hosts"),
                        Pair(storeData?.credsCracked?.count ?: 0, "Credentials"),
                        Pair(storeData?.stolenFiles?.size ?: 0, "Files"),
                        Pair(storeData?.handshakes?.size ?: 0, "HS")
                    ).forEach { (value, label) ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                                Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Primary)
                                Text(label, style = MaterialTheme.typography.bodySmall, color = SubtleText)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Console Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SmallActionBtn(if (showLogs) "Hide" else "Show", Icons.Default.Terminal) { showLogs = !showLogs }
                        if (logs.isNotEmpty()) {
                            SmallActionBtn("Clear", Icons.Default.Delete) { logs = emptyList() }
                        }
                    }
                }
                if (showLogs) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { logFontSize = maxOf(8f, logFontSize - 1f) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.TextDecrease, contentDescription = "Decrease", modifier = Modifier.size(14.dp), tint = SubtleText)
                        }
                        Text("${logFontSize.toInt()}px", style = MaterialTheme.typography.labelSmall, color = SubtleText)
                        IconButton(onClick = { logFontSize = minOf(24f, logFontSize + 1f) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.TextIncrease, contentDescription = "Increase", modifier = Modifier.size(14.dp), tint = SubtleText)
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0D1117))
                    ) {
                        if (logs.isEmpty()) {
                            Text(
                                "No logs available",
                                color = SubtleText,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        } else {
                            LazyColumn(
                                state = logScrollState,
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(logs.size) { i ->
                                    Text(
                                        logs[i],
                                        color = if (logs[i].contains("ERROR", ignoreCase = true) || logs[i].contains("FAIL", ignoreCase = true)) Error
                                                else if (logs[i].contains("WARN", ignoreCase = true)) Warning
                                                else Color(0xFF00FF00),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = logFontSize.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (isLoading) { LoadingOverlay(message = "Loading your-pax...") }
}

@Composable
private fun SmallActionBtn(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), shape = RoundedCornerShape(6.dp), modifier = Modifier.height(36.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.85f)
    }
}

@Composable
private fun NavChip(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
    }
}
