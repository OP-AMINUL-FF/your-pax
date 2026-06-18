package com.yourpax.app.ui.screens.wifi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourpax.app.data.api.models.ConflictStatus
import com.yourpax.app.data.api.models.WiFiNetwork
import com.yourpax.app.data.repository.EvilApRepository
import com.yourpax.app.data.repository.WiFiRepository
import com.yourpax.app.ui.components.*
import com.yourpax.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WiFiScreen(
    onNavigateToEvilAp: () -> Unit = {},
    onNavigateToWifiConnect: () -> Unit = {}
) {
    val repo = remember { WiFiRepository() }
    val evilRepo = remember { EvilApRepository() }
    val scope = rememberCoroutineScope()
    var networks by remember { mutableStateOf<List<WiFiNetwork>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedNetwork by remember { mutableStateOf<WiFiNetwork?>(null) }
    var statusMsg by remember { mutableStateOf("") }
    var conflictStatus by remember { mutableStateOf<ConflictStatus?>(null) }
    var monMode by remember { mutableStateOf(false) }
    var interfaces by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedInterface by remember { mutableStateOf("wlan0") }
    var showInterfaceDropdown by remember { mutableStateOf(false) }
    var showMonToggleConfirm by remember { mutableStateOf(false) }
    var pendingMonState by remember { mutableStateOf(false) }

    suspend fun scan() {
        isLoading = true
        networks = repo.scanNetworks().getOrDefault(emptyList())
        conflictStatus = evilRepo.getConflictStatus().getOrNull()
        isLoading = false
    }

    LaunchedEffect(Unit) {
        scan()
        evilRepo.listInterfaces().getOrNull()?.let {
            interfaces = it
            if (it.isNotEmpty()) selectedInterface = it.first()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DemoModeBanner()
        TopAppBar(
            title = { Text("Wi-Fi Attacks", fontWeight = FontWeight.SemiBold) },
            actions = {
                IconButton(onClick = { scope.launch { scan() } }) { Icon(Icons.Default.Refresh, contentDescription = "Scan") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { scope.launch { scan() } }, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) { Text("Scan", style = MaterialTheme.typography.bodySmall) }
            OutlinedButton(onClick = {
                scope.launch {
                    val cfg = com.yourpax.app.data.repository.ConfigRepository().loadConfig().getOrNull()
                    pendingMonState = !(cfg?.enableMonitorMode ?: false)
                    showMonToggleConfirm = true
                }
            }, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) { Text("Toggle Mon", style = MaterialTheme.typography.bodySmall) }
            OutlinedButton(onClick = {
                scope.launch {
                    val cs = evilRepo.getConflictStatus().getOrNull()
                    statusMsg = if (cs?.monitorMode == true) "Monitor mode is active" else "Monitor mode is off"
                }
            }, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)) { Text("Check", style = MaterialTheme.typography.bodySmall) }
            if (interfaces.isNotEmpty()) {
                Box {
                    OutlinedButton(onClick = { showInterfaceDropdown = true }, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(selectedInterface, style = MaterialTheme.typography.bodySmall)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = showInterfaceDropdown, onDismissRequest = { showInterfaceDropdown = false }) {
                        interfaces.forEach { iface ->
                            DropdownMenuItem(
                                text = { Text(iface, style = MaterialTheme.typography.bodySmall) },
                                onClick = { selectedInterface = iface; showInterfaceDropdown = false }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text("(${networks.size} nets)", style = MaterialTheme.typography.bodySmall, color = SubtleText)
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onNavigateToEvilAp, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Evil AP / Captive Portal", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onNavigateToWifiConnect, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("WiFi Connect", style = MaterialTheme.typography.bodySmall)
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val monActive = conflictStatus?.monitorMode == true
            StatusIndicator(isActive = monActive, label = if (monActive) "Monitor ON" else "Monitor OFF")
            if (selectedInterface != "wlan0") {
                Text("iface: $selectedInterface", style = MaterialTheme.typography.bodySmall, color = SubtleText)
            }
        }

        conflictStatus?.let { cs ->
            val warnings = buildList {
                if (cs.evilApRunning) add("Evil AP is running")
                if (cs.monitorMode && !cs.handshakeRunning && !cs.pmkidRunning) add("Monitor mode is active")
                if (cs.oneshotRunning) add("WPS attack is running")
            }
            if (warnings.isNotEmpty()) {
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF330000))) {
                    Text("\u26A0 Conflict: ${warnings.joinToString(", ")}", modifier = Modifier.padding(8.dp, 12.dp), style = MaterialTheme.typography.bodySmall, color = Color(0xFFff6666))
                }
            }
        }

        if (statusMsg.isNotEmpty()) {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Info.copy(alpha = 0.1f))) {
                Text(statusMsg, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, color = Info)
            }
        }

        if (isLoading) {
            LoadingOverlay()
        } else if (networks.isEmpty()) {
            EmptyState(title = "No networks found", subtitle = "Click Scan Networks to discover Wi-Fi networks")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("#", Modifier.width(24.dp), style = MaterialTheme.typography.bodySmall, color = SubtleText)
                        Text("SSID", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = SubtleText)
                        Text("Ch", Modifier.width(28.dp), style = MaterialTheme.typography.bodySmall, color = SubtleText)
                        Text("Signal", Modifier.width(60.dp), style = MaterialTheme.typography.bodySmall, color = SubtleText)
                        Text("Security", Modifier.width(60.dp), style = MaterialTheme.typography.bodySmall, color = SubtleText)
                    }
                }
                items(networks.withIndex().toList()) { (idx, network) ->
                    Card(
                        onClick = { selectedNetwork = network },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${idx + 1}", Modifier.width(24.dp), style = MaterialTheme.typography.bodySmall, color = Color(0xFF666666))
                            Column(Modifier.weight(1f)) {
                                Text(network.ssid.ifEmpty { "<Hidden>" }, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                                Text(network.bssid.ifEmpty { "??:??:??:??:??:??" }, style = MaterialTheme.typography.bodySmall, color = Color(0xFF888888), fontSize = MaterialTheme.typography.bodySmall.fontSize)
                            }
                            Text(network.channel.ifEmpty { "?" }, Modifier.width(28.dp), style = MaterialTheme.typography.bodySmall)
                            SignalDisplay(network.signal)
                            SecurityBadge(network.wpa, network.wps)
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (selectedNetwork != null) {
        WiFiTargetAttackDialog(
            network = selectedNetwork!!,
            onDismiss = { selectedNetwork = null },
            repo = repo,
            scope = scope,
            onStatus = { statusMsg = it }
        )
    }

    if (showMonToggleConfirm) {
        AlertDialog(
            onDismissRequest = { showMonToggleConfirm = false },
            title = { Text("Toggle Monitor Mode?") },
            text = { Text(if (pendingMonState) "Enable monitor mode? This will put the Wi-Fi interface into monitoring mode for packet capture. Other Wi-Fi operations may be interrupted." else "Disable monitor mode? Return the Wi-Fi interface to managed mode.") },
            confirmButton = {
                TextButton(onClick = {
                    showMonToggleConfirm = false
                    scope.launch {
                        com.yourpax.app.data.repository.ConfigRepository().saveConfig(mapOf("enable_monitor_mode" to pendingMonState))
                        monMode = pendingMonState
                        statusMsg = if (pendingMonState) "Monitor mode on" else "Monitor mode off"
                        conflictStatus = evilRepo.getConflictStatus().getOrNull()
                    }
                }) { Text(if (pendingMonState) "Enable" else "Disable") }
            },
            dismissButton = { TextButton(onClick = { showMonToggleConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SignalDisplay(signal: String) {
    val sig = signal.toIntOrNull() ?: -100
    val color = when {
        sig > -50 -> Success; sig > -70 -> Warning
        else -> Error
    }
    val bars = when {
        sig > -50 -> "\u2582\u2584\u2586\u2588"
        sig > -60 -> "\u2582\u2584\u2586"
        sig > -70 -> "\u2582\u2584"
        else -> "\u2582"
    }
    Row(Modifier.width(60.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(bars, color = color, style = MaterialTheme.typography.bodySmall)
        Text(signal, style = MaterialTheme.typography.bodySmall, color = SubtleText)
    }
}

@Composable
private fun SecurityBadge(wpa: String?, wps: String?) {
    Row(Modifier.width(60.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (wpa?.contains("WPA2") == true || wpa?.contains("WPA") == true) {
            Box(Modifier.background(Color(0xFF004488), RoundedCornerShape(3.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                Text(if (wpa.contains("WPA2")) "WPA2" else "WPA", color = Color.White, style = MaterialTheme.typography.bodySmall, fontSize = MaterialTheme.typography.bodySmall.fontSize)
            }
        } else if (wpa?.contains("OPEN") == true || wpa.isNullOrBlank()) {
            Box(Modifier.background(Color(0xFF006633), RoundedCornerShape(3.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                Text("OPEN", color = Color.White, style = MaterialTheme.typography.bodySmall, fontSize = MaterialTheme.typography.bodySmall.fontSize)
            }
        } else {
            Box(Modifier.background(Color(0xFF004488), RoundedCornerShape(3.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                Text("WPA2", color = Color.White, style = MaterialTheme.typography.bodySmall, fontSize = MaterialTheme.typography.bodySmall.fontSize)
            }
        }
        if (wps?.contains("Yes") == true || wps?.lowercase() == "yes") {
            Box(Modifier.background(Color(0xFFff6600), RoundedCornerShape(3.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                Text("WPS", color = Color.White, style = MaterialTheme.typography.bodySmall, fontSize = MaterialTheme.typography.bodySmall.fontSize)
            }
        }
    }
}
