package com.yourpax.app.ui.screens.more

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourpax.app.data.api.models.ConfigData
import com.yourpax.app.data.repository.ConfigRepository
import com.yourpax.app.ui.components.*
import com.yourpax.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MoreScreen(
    onNavigateToStore: () -> Unit = {},
    onNavigateToConfig: () -> Unit = {},
    onNavigateToManual: () -> Unit = {},
    onNavigateToNetKB: () -> Unit = {},
    onNavigateToEpd: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val configRepo = remember { ConfigRepository() }
    val scope = rememberCoroutineScope()
    var config by remember { mutableStateOf<ConfigData?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        config = configRepo.loadConfig().getOrNull()
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DemoModeBanner()
        TopAppBar(title = { Text("More", fontWeight = FontWeight.SemiBold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface, contentColor = Primary) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Config") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("About") })
        }

        if (isLoading) {
            LoadingOverlay()
        } else when (selectedTab) {
            0 -> ConfigContent(config = config, configRepo = configRepo, scope = scope)
            1 -> AboutContent(onNavigateToStore = onNavigateToStore, onNavigateToConfig = onNavigateToConfig, onNavigateToManual = onNavigateToManual, onNavigateToNetKB = onNavigateToNetKB, onNavigateToEpd = onNavigateToEpd, onNavigateToLogs = onNavigateToLogs, onNavigateToBackup = onNavigateToBackup, onNavigateToSettings = onNavigateToSettings)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ConfigContent(config: ConfigData?, configRepo: ConfigRepository, scope: kotlinx.coroutines.CoroutineScope) {
    if (config == null) { EmptyState(title = "Could not load config"); return }

    var editMode by remember { mutableStateOf(false) }
    var scanInterval by remember { mutableStateOf(config.scanInterval.toString()) }
    var nmapAggr by remember { mutableStateOf(config.nmapAggressivity) }
    var evilSsid by remember { mutableStateOf(config.evilApSsid) }
    var evilChannel by remember { mutableStateOf(config.evilApChannel.toString()) }
    var manualMode by remember { mutableStateOf(config.manualMode) }
    var debugMode by remember { mutableStateOf(config.debugMode) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Settings", fontWeight = FontWeight.SemiBold)
                        if (!editMode) TextButton(onClick = { editMode = true }) { Text("Edit") }
                    }
                    if (editMode) {
                        OutlinedTextField(value = scanInterval, onValueChange = { scanInterval = it }, label = { Text("Scan Interval (s)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = nmapAggr, onValueChange = { nmapAggr = it }, label = { Text("NMAP Aggressivity") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = evilSsid, onValueChange = { evilSsid = it }, label = { Text("Evil AP SSID") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = evilChannel, onValueChange = { evilChannel = it }, label = { Text("Evil AP Channel") }, modifier = Modifier.fillMaxWidth())
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = manualMode, onClick = { manualMode = !manualMode }, label = { Text("Manual Mode") })
                            FilterChip(selected = debugMode, onClick = { debugMode = !debugMode }, label = { Text("Debug") })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                scope.launch {
                                    configRepo.saveConfig(mapOf(
                                        "scan_interval" to (scanInterval.toIntOrNull() ?: 180),
                                        "nmap_scan_aggressivity" to nmapAggr,
                                        "evil_ap_ssid" to evilSsid,
                                        "evil_ap_channel" to (evilChannel.toIntOrNull() ?: 6),
                                        "manual_mode" to manualMode,
                                        "debug_mode" to debugMode
                                    ))
                                    editMode = false
                                }
                            }, shape = RoundedCornerShape(12.dp)) { Text("Save") }
                            OutlinedButton(onClick = { editMode = false }, shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
                        }
                    } else {
                        ConfigRow("Scan Interval", "${config.scanInterval}s"); ConfigRow("NMAP", config.nmapAggressivity); ConfigRow("Evil AP SSID", config.evilApSsid); ConfigRow("Manual Mode", if (config.manualMode) "On" else "Off"); ConfigRow("Debug Mode", if (config.debugMode) "On" else "Off")
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun ConfigRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SubtleText)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AboutContent(onNavigateToStore: () -> Unit, onNavigateToConfig: () -> Unit, onNavigateToManual: () -> Unit, onNavigateToNetKB: () -> Unit = {}, onNavigateToEpd: () -> Unit = {}, onNavigateToLogs: () -> Unit = {}, onNavigateToBackup: () -> Unit = {}, onNavigateToSettings: () -> Unit = {}) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            ModernCard {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("your-pax", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Primary)
                    Text("v1.0.0", style = MaterialTheme.typography.bodyMedium, color = SubtleText)
                    Text("Offensive Security Cyberdeck", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                }
            }
        }
        item {
            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Screens", fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = onNavigateToSettings, label = { Text("Settings") }, leadingIcon = { Icon(Icons.Default.Settings, null, Modifier.size(16.dp)) })
                        AssistChip(onClick = onNavigateToStore, label = { Text("Store Dashboard") }, leadingIcon = { Icon(Icons.Default.Store, null, Modifier.size(16.dp)) })
                        AssistChip(onClick = onNavigateToConfig, label = { Text("Full Config") }, leadingIcon = { Icon(Icons.Default.Code, null, Modifier.size(16.dp)) })
                        AssistChip(onClick = onNavigateToManual, label = { Text("Manual Mode") }, leadingIcon = { Icon(Icons.Default.Terminal, null, Modifier.size(16.dp)) })
                        AssistChip(onClick = onNavigateToNetKB, label = { Text("NetKB") }, leadingIcon = { Icon(Icons.Default.Dns, null, Modifier.size(16.dp)) })
                        AssistChip(onClick = onNavigateToEpd, label = { Text("Live EPD") }, leadingIcon = { Icon(Icons.Default.ScreenshotMonitor, null, Modifier.size(16.dp)) })
                        AssistChip(onClick = onNavigateToLogs, label = { Text("Logs") }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.Article, null, Modifier.size(16.dp)) })
                        AssistChip(onClick = onNavigateToBackup, label = { Text("Backup/Restore") }, leadingIcon = { Icon(Icons.Default.Backup, null, Modifier.size(16.dp)) })
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
