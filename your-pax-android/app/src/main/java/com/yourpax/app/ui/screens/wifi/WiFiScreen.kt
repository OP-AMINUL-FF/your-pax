package com.yourpax.app.ui.screens.wifi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourpax.app.data.api.models.ConflictStatus
import com.yourpax.app.data.api.models.WiFiNetwork
import com.yourpax.app.data.repository.ConfigRepository
import com.yourpax.app.data.repository.EvilApRepository
import com.yourpax.app.data.repository.WiFiRepository
import com.yourpax.app.R
import com.yourpax.app.ui.components.DemoModeBanner
import com.yourpax.app.ui.components.EmptyState
import com.yourpax.app.ui.components.LoadingOverlay
import com.yourpax.app.ui.components.SecurityBadge
import com.yourpax.app.ui.components.SignalDisplay
import com.yourpax.app.ui.components.StatusIndicator
import com.yourpax.app.ui.components.StatusMessageBanner
import com.yourpax.app.ui.components.FadeScaleIn
import com.yourpax.app.ui.components.LottieAnim
import com.yourpax.app.ui.components.CopyButton
import com.yourpax.app.ui.theme.rememberAppColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WiFiScreen(
    onOpenDrawer: () -> Unit = {}, onNavigateToEvilAp: () -> Unit = {}, onNavigateToWifiConnect: () -> Unit = {}
) {
    val repo = remember { WiFiRepository() }
    val evilRepo = remember { EvilApRepository() }
    val scope = rememberCoroutineScope()
    var networks by remember { mutableStateOf<List<WiFiNetwork>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedNetwork by remember { mutableStateOf<WiFiNetwork?>(null) }
    var statusMsg by remember { mutableStateOf("") }
    var conflictStatus by remember { mutableStateOf<ConflictStatus?>(null) }
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
        evilRepo.listInterfaces().getOrNull()?.let { interfaces = it; if (it.isNotEmpty()) selectedInterface = it.first() }
    }

    val appColors = rememberAppColors()

    Column(modifier = Modifier.fillMaxSize()) {
        DemoModeBanner()
        TopAppBar(
            title = { Text("Wi-Fi Attacks", fontWeight = FontWeight.SemiBold) },
            navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, contentDescription = "Menu") } },
            actions = { IconButton(onClick = { scope.launch { scan() } }) { Icon(Icons.Default.Refresh, contentDescription = "Scan") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        FadeScaleIn(delayMillis = 100) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { scope.launch { scan() } }, shape = MaterialTheme.shapes.small, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp), modifier = Modifier.height(40.dp)) { Text("Scan") }
                OutlinedButton(onClick = {
                    scope.launch {
                        pendingMonState = !(ConfigRepository().loadConfig().getOrNull()?.enableMonitorMode ?: false)
                        showMonToggleConfirm = true
                    }
                }, shape = MaterialTheme.shapes.small, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp), modifier = Modifier.height(40.dp)) { Text("Toggle") }
                OutlinedButton(onClick = {
                    scope.launch { conflictStatus = evilRepo.getConflictStatus().getOrNull(); statusMsg = if (conflictStatus?.monitorMode == true) "Monitor mode is active" else "Monitor mode is off" }
                }, shape = MaterialTheme.shapes.small, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp), modifier = Modifier.height(40.dp)) { Text("Check") }
                if (interfaces.isNotEmpty()) {
                    Box {
                        OutlinedButton(onClick = { showInterfaceDropdown = true }, shape = MaterialTheme.shapes.small, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp), modifier = Modifier.height(40.dp)) {
                            Text(selectedInterface, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = showInterfaceDropdown, onDismissRequest = { showInterfaceDropdown = false }) {
                            interfaces.forEach { iface ->
                                DropdownMenuItem(text = { Text(iface) }, onClick = { selectedInterface = iface; showInterfaceDropdown = false })
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                LottieAnim(
                    rawResId = R.raw.wifi_scanning,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("(${networks.size} nets)", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
            }
        }

        FadeScaleIn(delayMillis = 150) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onNavigateToEvilAp, shape = MaterialTheme.shapes.small, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), modifier = Modifier.height(40.dp)) {
                    Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Evil AP")
                }
                OutlinedButton(onClick = onNavigateToWifiConnect, shape = MaterialTheme.shapes.small, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), modifier = Modifier.height(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("WiFi Connect")
                }
            }
        }

        FadeScaleIn(delayMillis = 200) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                val monActive = conflictStatus?.monitorMode == true
                StatusIndicator(isActive = monActive, label = if (monActive) "Monitor ON" else "Monitor OFF")
                if (selectedInterface != "wlan0") Text("iface: $selectedInterface", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
            }
        }

        FadeScaleIn(delayMillis = 250) {
            conflictStatus?.let { cs ->
                val warnings = buildList {
                    if (cs.evilApRunning) add("Evil AP is running")
                    if (cs.monitorMode && !cs.handshakeRunning && !cs.pmkidRunning) add("Monitor mode is active")
                    if (cs.oneshotRunning) add("WPS attack is running")
                }
                if (warnings.isNotEmpty()) {
                    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = MaterialTheme.shapes.extraSmall, color = appColors.conflictBackground) {
                        Text("\u26A0 Conflict: ${warnings.joinToString(", ")}", modifier = Modifier.padding(8.dp, 12.dp), style = MaterialTheme.typography.bodySmall, color = appColors.conflictText)
                    }
                }
            }
        }

        if (statusMsg.isNotEmpty()) FadeScaleIn(delayMillis = 300) { StatusMessageBanner(message = statusMsg, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }

        if (isLoading) LoadingOverlay()
        else if (networks.isEmpty()) EmptyState(title = "No networks found", subtitle = "Click Scan Networks to discover Wi-Fi networks")
        else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("#", Modifier.width(24.dp), style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                        Text("SSID", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                        Text("Ch", Modifier.width(28.dp), style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                        Text("Signal", Modifier.width(60.dp), style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                        Text("Security", Modifier.width(60.dp), style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                        Text("WPS", Modifier.width(32.dp), style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                    }
                }
                items(networks.withIndex().toList()) { (idx, network) ->
                    Surface(
                        onClick = { selectedNetwork = network },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = appColors.surfaceContainerLow,
                        tonalElevation = 0.dp
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${idx + 1}", Modifier.width(20.dp), style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                            Column(Modifier.weight(1f)) {
                                Text(network.ssid.ifEmpty { "<Hidden>" }, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(network.bssid.ifEmpty { "??:??:??:??:??:??" }, style = MaterialTheme.typography.bodySmall, color = appColors.subtleText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    CopyButton(textToCopy = network.bssid.ifEmpty { network.ssid }, size = 12.dp)
                                }
                            }
                            Text(network.channel.ifEmpty { "?" }, Modifier.width(24.dp), style = MaterialTheme.typography.bodySmall)
                            SignalDisplay(network.signal.toIntOrNull() ?: -100)
                            SecurityBadge(if (network.wpa == true) "WPA" else "Open")
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = if (network.wps == true) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp
                            ) {
                                Text(
                                    if (network.wps == true) "WPS" else "-",
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (network.wps == true) MaterialTheme.colorScheme.tertiary else appColors.subtleText
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (selectedNetwork != null) {
        WiFiTargetAttackDialog(network = selectedNetwork!!, onDismiss = { selectedNetwork = null }, repo = repo, scope = scope, onStatus = { statusMsg = it })
    }

    if (showMonToggleConfirm) {
        AlertDialog(
            onDismissRequest = { showMonToggleConfirm = false },
            title = { Text("Toggle Monitor Mode?") },
            text = { Text(if (pendingMonState) "Enable monitor mode? This will put the Wi-Fi interface into monitoring mode for packet capture. Other Wi-Fi operations may be interrupted." else "Disable monitor mode? Return the Wi-Fi interface to managed mode.") },
            confirmButton = { TextButton(onClick = { showMonToggleConfirm = false; scope.launch { ConfigRepository().saveConfig(mapOf("enable_monitor_mode" to pendingMonState)); statusMsg = if (pendingMonState) "Monitor mode on" else "Monitor mode off"; conflictStatus = evilRepo.getConflictStatus().getOrNull() } }) { Text(if (pendingMonState) "Enable" else "Disable") } },
            dismissButton = { TextButton(onClick = { showMonToggleConfirm = false }) { Text("Cancel") } }
        )
    }
}
