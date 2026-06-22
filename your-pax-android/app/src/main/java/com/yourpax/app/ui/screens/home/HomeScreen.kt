package com.yourpax.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourpax.app.data.comm.BtEvent
import com.yourpax.app.data.comm.EventBus
import com.yourpax.app.data.repository.BluetoothRepository
import com.yourpax.app.data.repository.ConfigRepository
import com.yourpax.app.data.repository.LootRepository
import com.yourpax.app.data.repository.NetworkRepository
import com.yourpax.app.data.repository.SystemRepository
import com.yourpax.app.data.repository.WiFiRepository
import com.yourpax.app.data.api.models.BluetoothStatus
import com.yourpax.app.data.api.models.StoreDataFull
import com.yourpax.app.data.api.models.WiFiStatusResponse
import com.yourpax.app.R
import com.yourpax.app.ui.components.DemoModeBanner
import com.yourpax.app.ui.components.FontSizeControl
import com.yourpax.app.ui.components.LoadingOverlay
import com.yourpax.app.ui.components.ModernCard
import com.yourpax.app.ui.components.SmallActionBtn
import com.yourpax.app.ui.components.StatusMessageBanner
import com.yourpax.app.ui.components.ToggleButton
import com.yourpax.app.ui.components.LottieAnim
import com.yourpax.app.ui.theme.rememberAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AttackAction { SCAN, BRUTEFORCE, VULN_SCAN, STEAL, STOP_ALL }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit = {}, onNavigateToStore: () -> Unit = {}, onNavigateToConfig: () -> Unit = {},
    onNavigateToManual: () -> Unit = {}, onNavigateToLogs: () -> Unit = {}, onNavigateToSettings: () -> Unit = {},
    onNavigateToNetwork: () -> Unit = {}, onNavigateToLoot: () -> Unit = {}, onNavigateToNetKB: () -> Unit = {},
    onNavigateToEpd: () -> Unit = {}, onNavigateToBackup: () -> Unit = {}, onNavigateToMore: () -> Unit = {},
    onNavigateToBluetooth: () -> Unit = {}
) {
    val lootRepo = remember { LootRepository() }
    val wifiRepo = remember { WiFiRepository() }
    val btRepo = remember { BluetoothRepository() }
    val configRepo = remember { ConfigRepository() }
    val networkRepo = remember { NetworkRepository() }
    val systemRepo = remember { SystemRepository() }
    val scope = rememberCoroutineScope()

    var storeData by remember { mutableStateOf<StoreDataFull?>(null) }
    var wifiStatus by remember { mutableStateOf<WiFiStatusResponse?>(null) }
    var btStatus by remember { mutableStateOf<BluetoothStatus?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("") }
    var logs by remember { mutableStateOf<List<String>>(emptyList()) }
    var showLogs by remember { mutableStateOf(false) }
    var logFontSize by remember { mutableFloatStateOf(12f) }
    val logScrollState = rememberLazyListState()

    var isManualMode by remember { mutableStateOf(false) }
    var orchStatus by remember { mutableStateOf("unknown") }
    var netkbIps by remember { mutableStateOf<List<String>>(emptyList()) }
    var netkbPorts by remember { mutableStateOf<List<String>>(emptyList()) }
    var netkbActions by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedIp by remember { mutableStateOf("") }
    var selectedPort by remember { mutableStateOf("") }
    var selectedAction by remember { mutableStateOf("") }
    var confirmAttack by remember { mutableStateOf<AttackAction?>(null) }
    var runningOps by remember { mutableStateOf<Set<String>>(emptySet()) }
    var connectionMode by remember { mutableStateOf("web_app") }
    var liveEvents by remember { mutableStateOf<List<String>>(emptyList()) }
    var showLiveEvents by remember { mutableStateOf(false) }

    fun triggerOp(op: String, launchBlock: suspend () -> Result<*>) {
        runningOps = runningOps + op
        scope.launch {
            launchBlock()
            delay(10000)
            runningOps = runningOps - op
        }
    }

    fun toggleMode() {
        if (isManualMode) {
            scope.launch {
                networkRepo.startOrchestrator().onSuccess { orchStatus = "running" }
                isManualMode = false
            }
        } else {
            scope.launch {
                networkRepo.stopOrchestrator().onSuccess { orchStatus = "stopped" }
                isManualMode = true
            }
        }
    }

    LaunchedEffect(Unit) {
        val cfg = configRepo.loadConfig().getOrNull()
        isManualMode = cfg?.manualMode ?: false
        orchStatus = if (isManualMode) "stopped" else "running"
        configRepo.getModeConfig().onSuccess { m -> connectionMode = m["connection_mode"] as? String ?: "web_app" }
        val meta = networkRepo.getNetKBMeta().getOrNull()
        if (meta != null) {
            netkbIps = meta.ips
            val allPorts = meta.ports.values.flatten().map { it.toString() }.distinct()
            netkbPorts = allPorts
            netkbActions = meta.actions
            if (netkbIps.isNotEmpty()) selectedIp = netkbIps.first()
            if (netkbPorts.isNotEmpty()) selectedPort = netkbPorts.first()
            if (netkbActions.isNotEmpty()) selectedAction = netkbActions.first()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            storeData = lootRepo.getStoreData().getOrNull()
            wifiStatus = wifiRepo.getWifiStatus().getOrNull()
            btStatus = btRepo.getBluetoothStatus().getOrNull()
            isLoading = false
            delay(5000)
        }
    }

    var showHandshakeAnim by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        EventBus.events.collect { event: BtEvent ->
            val entry = "${event.event}: ${event.data.toString().take(80)}"
            liveEvents = (liveEvents + entry).takeLast(50)
            if (event.event == "wifi_handshake_captured") {
                statusMessage = "Handshake captured!"
                showHandshakeAnim = true
                launch {
                    delay(4000)
                    showHandshakeAnim = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            lootRepo.getLogs().getOrNull()?.let { resp ->
                val lines = resp.logs.lines().filter { it.isNotBlank() }
                if (lines.isNotEmpty()) {
                    logs = lines
                    if (showLogs && logScrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == lines.size - 2) {
                        launch { logScrollState.scrollToItem(lines.size - 1) }
                    }
                }
            }
            delay(3000)
        }
    }

    val appColors = rememberAppColors()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("your-pax", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, contentDescription = "Menu") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )
        DemoModeBanner()

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            btStatus?.let { bt ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = appColors.surfaceContainerLow,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatusChip(Icons.Default.Bluetooth, if (bt.active) "BT: ${bt.ssid.ifEmpty { "your-pax" }}" else "BT Off", bt.active, appColors.success)
                            Text("|", color = appColors.subtleText, style = MaterialTheme.typography.bodySmall)
                            Text("${bt.bridgeIp}:${bt.port}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Text("|", color = appColors.subtleText, style = MaterialTheme.typography.bodySmall)
                            StatusChip(Icons.Default.People, "Clients: ${bt.connectedClients}", bt.connectedClients > 0, appColors.warning)
                            Text("|", color = appColors.subtleText, style = MaterialTheme.typography.bodySmall)
                            StatusChip(Icons.Default.Wifi, if (wifiStatus?.connected == true) wifiStatus?.ssid ?: "WiFi" else "WiFi Off", wifiStatus?.connected == true, appColors.success)
                            Text("|", color = appColors.subtleText, style = MaterialTheme.typography.bodySmall)
                            StatusChip(
                                Icons.Default.Settings,
                                connectionMode.replace("_", " ").replaceFirstChar { it.uppercase() },
                                true,
                                when (connectionMode) {
                                    "web_only" -> appColors.info
                                    "app_only" -> appColors.warning
                                    else -> appColors.success
                                }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text("Good to go, Operator!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("your-pax is ready", style = MaterialTheme.typography.bodyMedium, color = appColors.subtleText)
            }

            if (statusMessage.isNotEmpty()) {
                item { StatusMessageBanner(message = statusMessage) }
            }

            item {
                Text("Quick Stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple(storeData?.netkbCount ?: 0, "Hosts", appColors.info),
                        Triple(storeData?.credsCracked?.count ?: 0, "Credentials", appColors.warning),
                        Triple(storeData?.stolenFiles?.size ?: 0, "Files", appColors.categoryStolen),
                        Triple(storeData?.handshakes?.size ?: 0, "HS", appColors.categoryHandshake)
                    ).forEach { (value, label, color) ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                            color = appColors.surfaceContainerLow,
                            tonalElevation = 0.dp
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
                            ) {
                                Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
                                Spacer(Modifier.height(2.dp))
                                Text(label, style = MaterialTheme.typography.labelSmall, color = appColors.subtleText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            item {
                ModernCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Mode", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                                Surface(shape = MaterialTheme.shapes.extraSmall, color = if (isManualMode) appColors.warningContainer else appColors.successContainer) {
                                    Text(
                                        if (isManualMode) "MANUAL" else "AUTO",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                                        color = if (isManualMode) appColors.warning else appColors.success
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { if (!isManualMode) toggleMode() },
                                    enabled = !isManualMode,
                                    shape = MaterialTheme.shapes.small,
                                    colors = ButtonDefaults.buttonColors(containerColor = appColors.warning),
                                    modifier = Modifier.height(36.dp)
                                ) { Text("Manual", style = MaterialTheme.typography.bodySmall) }
                                OutlinedButton(
                                    onClick = { if (isManualMode) toggleMode() },
                                    enabled = isManualMode,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.height(36.dp)
                                ) { Text("Auto", style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Orchestrator:", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                            val running = orchStatus == "running"
                            Text(
                                if (running) "Running" else "Stopped",
                                style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                                color = if (running) appColors.success else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (runningOps.isNotEmpty()) {
                item {
                    ModernCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            LottieAnim(
                                rawResId = R.raw.radar_blue,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Running: ${runningOps.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            item {
                ModernCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Attacks", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = false, onClick = { confirmAttack = AttackAction.SCAN }, label = { Text("Scan", style = MaterialTheme.typography.bodySmall) }, leadingIcon = { LottieAnim(rawResId = R.raw.attack_scan, modifier = Modifier.size(16.dp)) })
                            FilterChip(selected = false, onClick = { confirmAttack = AttackAction.BRUTEFORCE }, label = { Text("Bruteforce", style = MaterialTheme.typography.bodySmall) }, leadingIcon = { LottieAnim(rawResId = R.raw.attack_bruteforce, modifier = Modifier.size(16.dp)) })
                            FilterChip(selected = false, onClick = { confirmAttack = AttackAction.VULN_SCAN }, label = { Text("Vuln", style = MaterialTheme.typography.bodySmall) }, leadingIcon = { LottieAnim(rawResId = R.raw.attack_vuln, modifier = Modifier.size(16.dp)) })
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = false, onClick = { confirmAttack = AttackAction.STEAL }, label = { Text("Steal", style = MaterialTheme.typography.bodySmall) }, leadingIcon = { LottieAnim(rawResId = R.raw.attack_steal, modifier = Modifier.size(16.dp)) })
                            FilterChip(selected = false, onClick = { confirmAttack = AttackAction.STOP_ALL }, label = { Text("Stop All", style = MaterialTheme.typography.bodySmall) }, leadingIcon = { LottieAnim(rawResId = R.raw.attack_deauth, modifier = Modifier.size(16.dp)) })
                        }
                    }
                }
            }

            if (isManualMode && netkbIps.isNotEmpty()) {
                item {
                    ModernCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Manual Attack", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text("IP", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                    var expandedIp by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(expanded = expandedIp, onExpandedChange = { expandedIp = it }) {
                                        OutlinedTextField(value = selectedIp, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), textStyle = MaterialTheme.typography.bodySmall, singleLine = true)
                                        ExposedDropdownMenu(expanded = expandedIp, onDismissRequest = { expandedIp = false }) {
                                            netkbIps.forEach { ip -> DropdownMenuItem(text = { Text(ip, style = MaterialTheme.typography.bodySmall) }, onClick = { selectedIp = ip; expandedIp = false }) }
                                        }
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("Port", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                    var expandedPort by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(expanded = expandedPort, onExpandedChange = { expandedPort = it }) {
                                        OutlinedTextField(value = selectedPort, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), textStyle = MaterialTheme.typography.bodySmall, singleLine = true)
                                        ExposedDropdownMenu(expanded = expandedPort, onDismissRequest = { expandedPort = false }) {
                                            netkbPorts.forEach { port -> DropdownMenuItem(text = { Text(port, style = MaterialTheme.typography.bodySmall) }, onClick = { selectedPort = port; expandedPort = false }) }
                                        }
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("Action", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                    var expandedAct by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(expanded = expandedAct, onExpandedChange = { expandedAct = it }) {
                                        OutlinedTextField(value = selectedAction, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), textStyle = MaterialTheme.typography.bodySmall, singleLine = true)
                                        ExposedDropdownMenu(expanded = expandedAct, onDismissRequest = { expandedAct = false }) {
                                            netkbActions.forEach { act -> DropdownMenuItem(text = { Text(act, style = MaterialTheme.typography.bodySmall) }, onClick = { selectedAction = act; expandedAct = false }) }
                                        }
                                    }
                                }
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        systemRepo.executeManualAttack(selectedIp, selectedPort, selectedAction)
                                            .onSuccess { statusMessage = "Attack sent to $selectedIp:$selectedPort" }
                                            .onFailure { statusMessage = "Attack failed: ${it.message}" }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small,
                                enabled = selectedIp.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = appColors.warning)
                            ) { Text("Execute") }
                        }
                    }
                }
            }

            item {
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionChip("Network", Icons.Default.Cloud, MaterialTheme.colorScheme.primary, onClick = onNavigateToNetwork)
                    ActionChip("Settings", Icons.Default.Settings, appColors.warning, onClick = onNavigateToSettings)
                    ActionChip("Store", Icons.Default.Store, MaterialTheme.colorScheme.primary, onClick = onNavigateToStore)
                    ActionChip("Config", Icons.Default.Code, appColors.categoryPortal, onClick = onNavigateToConfig)
                    ActionChip("Manual", Icons.Default.Terminal, MaterialTheme.colorScheme.tertiary, onClick = onNavigateToManual)
                    ActionChip("Logs", Icons.AutoMirrored.Filled.Article, MaterialTheme.colorScheme.secondary, onClick = onNavigateToLogs)
                    ActionChip("Loot", Icons.Default.Description, appColors.categoryStolen, onClick = onNavigateToLoot)
                    ActionChip("NetKB", Icons.Default.Dns, appColors.categoryNetkb, onClick = onNavigateToNetKB)
                    ActionChip("Bluetooth", Icons.Default.Bluetooth, MaterialTheme.colorScheme.tertiary, onClick = onNavigateToBluetooth)
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Console Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SmallActionBtn(label = if (showLogs) "Hide" else "Show", icon = Icons.Default.Terminal, onClick = { showLogs = !showLogs })
                        if (logs.isNotEmpty()) SmallActionBtn(label = "Clear", icon = Icons.Default.Delete, onClick = { logs = emptyList() })
                    }
                }
                if (showLogs) {
                    Spacer(Modifier.height(6.dp))
                    FontSizeControl(currentSize = logFontSize, onDecrease = { logFontSize = maxOf(8f, logFontSize - 1f) }, onIncrease = { logFontSize = minOf(24f, logFontSize + 1f) }, modifier = Modifier.align(Alignment.End))
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = appColors.terminalBackground,
                        tonalElevation = 0.dp
                    ) {
                        if (logs.isEmpty()) {
                            Text("No logs available", color = appColors.subtleText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                        } else {
                            LazyColumn(
                                state = logScrollState,
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(logs.size) { i ->
                                    Text(
                                        logs[i], color = when {
                                            logs[i].contains("ERROR", ignoreCase = true) || logs[i].contains("FAIL", ignoreCase = true) -> MaterialTheme.colorScheme.error
                                            logs[i].contains("WARN", ignoreCase = true) -> appColors.warning
                                            else -> appColors.terminalText
                                        },
                                        style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontSize = logFontSize.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Live Events", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SmallActionBtn(label = if (showLiveEvents) "Hide" else "Show", icon = Icons.Default.Terminal, onClick = { showLiveEvents = !showLiveEvents })
                        if (liveEvents.isNotEmpty()) SmallActionBtn(label = "Clear", icon = Icons.Default.Delete, onClick = { liveEvents = emptyList() })
                    }
                }
                if (showLiveEvents) {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = appColors.terminalBackground,
                        tonalElevation = 0.dp
                    ) {
                        if (liveEvents.isEmpty()) {
                            Text("No events yet", color = appColors.subtleText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                items(liveEvents.size) { i ->
                                    Text(
                                        liveEvents[i],
                                        color = if (liveEvents[i].contains("handshake", ignoreCase = true) || liveEvents[i].contains("captured", ignoreCase = true)) appColors.success
                                                else if (liveEvents[i].contains("fail", ignoreCase = true) || liveEvents[i].contains("error", ignoreCase = true)) MaterialTheme.colorScheme.error
                                                else appColors.terminalText,
                                        style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    when (confirmAttack) {
        AttackAction.SCAN -> AlertDialog(
            onDismissRequest = { confirmAttack = null },
            title = { Text("Network Scan?") },
            text = { Text("Start a full network scan to discover hosts and open ports.") },
            confirmButton = { TextButton(onClick = { confirmAttack = null; triggerOp("scan") { networkRepo.triggerScan().also { statusMessage = if (it.isSuccess) "Scan triggered" else "Scan failed" } } }) { Text("Scan") } },
            dismissButton = { TextButton(onClick = { confirmAttack = null }) { Text("Cancel") } }
        )
        AttackAction.BRUTEFORCE -> AlertDialog(
            onDismissRequest = { confirmAttack = null },
            title = { Text("Bruteforce All?") },
            text = { Text("Start bruteforce against all protocols on discovered targets?") },
            confirmButton = { TextButton(onClick = { confirmAttack = null; triggerOp("bruteforce") { networkRepo.triggerBruteforce().also { statusMessage = if (it.isSuccess) "Bruteforce triggered" else "Bruteforce failed" } } }) { Text("Start") } },
            dismissButton = { TextButton(onClick = { confirmAttack = null }) { Text("Cancel") } }
        )
        AttackAction.VULN_SCAN -> AlertDialog(
            onDismissRequest = { confirmAttack = null },
            title = { Text("Vulnerability Scan?") },
            text = { Text("Run nmap --script vuln on all targets? This may take a while.") },
            confirmButton = { TextButton(onClick = { confirmAttack = null; triggerOp("vuln scan") { networkRepo.triggerVulnScan().also { statusMessage = if (it.isSuccess) "Vuln scan triggered" else "Vuln scan failed" } } }) { Text("Scan") } },
            dismissButton = { TextButton(onClick = { confirmAttack = null }) { Text("Cancel") } }
        )
        AttackAction.STEAL -> AlertDialog(
            onDismissRequest = { confirmAttack = null },
            title = { Text("Steal Files?") },
            text = { Text("Attempt to steal files from cracked targets using stored credentials.") },
            confirmButton = { TextButton(onClick = { confirmAttack = null; triggerOp("steal") { networkRepo.triggerSteal().also { statusMessage = if (it.isSuccess) "Steal triggered" else "Steal failed" } } }) { Text("Steal") } },
            dismissButton = { TextButton(onClick = { confirmAttack = null }) { Text("Cancel") } }
        )
        AttackAction.STOP_ALL -> AlertDialog(
            onDismissRequest = { confirmAttack = null },
            title = { Text("Stop All Attacks?") },
            text = { Text("Stop all running attacks including scans, bruteforce, handshake capture, and WPS attacks.") },
            confirmButton = { TextButton(onClick = { confirmAttack = null; scope.launch { networkRepo.stopAll().onSuccess { statusMessage = "All stopped" }.onFailure { statusMessage = "Failed" } } }) { Text("Stop All", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmAttack = null }) { Text("Cancel") } }
        )
        null -> {}
    }

    if (showHandshakeAnim) {
        AlertDialog(
            onDismissRequest = { showHandshakeAnim = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LottieAnim(
                        rawResId = R.raw.handshake_success,
                        modifier = Modifier.size(36.dp)
                    )
                    Text("Handshake Captured!", fontWeight = FontWeight.Bold)
                }
            },
            text = { Text("A new WPA/WPA2 handshake has been successfully captured and saved to the loot repository.") },
            confirmButton = {
                TextButton(onClick = { showHandshakeAnim = false }) { Text("Dismiss") }
            }
        )
    }

    if (isLoading) LoadingOverlay(message = "Loading your-pax...")
}

@Composable
private fun StatusChip(icon: ImageVector, label: String, active: Boolean, activeColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = if (active) activeColor else MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = if (active) activeColor else MaterialTheme.colorScheme.error, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionChip(text: String, icon: ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val chipColors = rememberAppColors()
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = chipColors.surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
    }
}
