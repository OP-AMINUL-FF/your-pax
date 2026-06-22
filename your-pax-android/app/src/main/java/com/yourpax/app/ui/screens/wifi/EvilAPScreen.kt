package com.yourpax.app.ui.screens.wifi

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourpax.app.R
import com.yourpax.app.data.api.models.ConflictStatus
import com.yourpax.app.data.api.models.EvilApStatusResponse
import com.yourpax.app.data.api.models.EvilClientInfo
import com.yourpax.app.data.api.models.EvilCredEntry
import com.yourpax.app.data.api.models.LootMonitorData
import com.yourpax.app.data.api.models.ScanTargetResult
import com.yourpax.app.data.repository.EvilApRepository
import com.yourpax.app.data.repository.LootRepository
import com.yourpax.app.data.repository.WiFiRepository
import com.yourpax.app.ui.components.DemoModeBanner
import com.yourpax.app.ui.components.DotState
import com.yourpax.app.ui.components.InfoCard
import com.yourpax.app.ui.components.LoadingOverlay
import com.yourpax.app.ui.components.ModernCard
import com.yourpax.app.ui.components.StatusDot
import com.yourpax.app.ui.components.StatusMessageBanner
import com.yourpax.app.ui.components.ToggleButton
import com.yourpax.app.ui.components.WarningCard
import com.yourpax.app.ui.components.LottieAnim
import com.yourpax.app.ui.components.CopyButton
import com.yourpax.app.ui.theme.rememberAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvilAPScreen(
    onOpenDrawer: () -> Unit = {}
) {
    val repo = remember { EvilApRepository() }
    val lootRepo = remember { LootRepository() }
    val wifiRepo = remember { WiFiRepository() }
    val scope = rememberCoroutineScope()
    val appColors = rememberAppColors()

    var ssid by remember { mutableStateOf("FreeWiFi") }
    var channel by remember { mutableStateOf("6") }
    var apPassword by remember { mutableStateOf("") }
    var security by remember { mutableStateOf("open") }
    var selectedIface by remember { mutableStateOf("wlan0") }
    var availableInterfaces by remember { mutableStateOf(listOf("wlan0", "wlan1", "wlan0mon")) }
    var selectedPortal by remember { mutableStateOf("generic.html") }
    var availablePortals by remember { mutableStateOf(listOf("generic.html")) }
    var status by remember { mutableStateOf<EvilApStatusResponse?>(null) }
    var clients by remember { mutableStateOf<List<EvilClientInfo>>(emptyList()) }
    var monitorData by remember { mutableStateOf<LootMonitorData?>(null) }
    var creds by remember { mutableStateOf<List<EvilCredEntry>>(emptyList()) }
    var conflict by remember { mutableStateOf<ConflictStatus?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var statusMsg by remember { mutableStateOf("") }

    var wpaEnabled by remember { mutableStateOf(false) }
    var wpaStatus by remember { mutableStateOf("idle") }
    var karmaEnabled by remember { mutableStateOf(false) }
    var cloneEnabled by remember { mutableStateOf(false) }
    var wpaIface by remember { mutableStateOf("wlan0") }
    var karmaIface by remember { mutableStateOf("wlan0") }
    var deauthIface by remember { mutableStateOf("wlan1") }
    var cloneIface by remember { mutableStateOf("wlan0") }
    var targetBssid by remember { mutableStateOf("") }
    var targetSsid by remember { mutableStateOf("") }
    var targetScanResults by remember { mutableStateOf<List<ScanTargetResult>>(emptyList()) }
    var scanningTargets by remember { mutableStateOf(false) }
    var monitorTab by remember { mutableStateOf(0) }
    var showStartApConfirm by remember { mutableStateOf(false) }
    var showDeauthConfirm by remember { mutableStateOf<EvilClientInfo?>(null) }
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    suspend fun refresh() {
        status = repo.getStatus().getOrNull()
        clients = repo.getClients().getOrNull()?.clients ?: emptyList()
        monitorData = repo.getMonitorData().getOrNull()
        creds = lootRepo.getStoreData().getOrNull()?.credsEvil ?: emptyList()
        conflict = repo.getConflictStatus().getOrNull()
        wpaStatus = (repo.wpaValidateStatus().getOrNull()?.get("status") as? String) ?: "idle"
        val ifaces = repo.listInterfaces().getOrNull()
        if (ifaces != null) availableInterfaces = ifaces
        val portals = repo.listPortals().getOrNull()?.portals ?: emptyList()
        availablePortals = portals.map { it.name }.ifEmpty { listOf("generic.html") }
        isLoading = false
    }

    LaunchedEffect(Unit) {
        while (true) { refresh(); delay(3000) }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(modifier = Modifier.fillMaxSize()) {
        DemoModeBanner()
        TopAppBar(
            title = { Text("Evil AP Control Panel", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            },
            actions = {
                IconButton(onClick = { scope.launch { isLoading = true; refresh() } }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        if (isLoading) {
            LoadingOverlay()
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Access Point Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    LottieAnim(
                        rawResId = R.raw.wifi_scanning,
                        modifier = Modifier.size(36.dp)
                    )
                }

                conflict?.let { c ->
                    val conflicts = buildList {
                        if (c.handshakeRunning) add("Handshake capture")
                        if (c.pmkidRunning) add("PMKID capture")
                        if (c.oneshotRunning) add("WPS attack")
                        if (c.monitorMode) add("Monitor mode")
                    }
                    if (conflicts.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = appColors.conflictBackground,
                            tonalElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = appColors.conflictText, modifier = Modifier.size(16.dp))
                                Text("Conflict: ${conflicts.joinToString(", ")}. Stop them first.", style = MaterialTheme.typography.bodySmall, color = appColors.conflictText)
                            }
                        }
                    }
                }

                if (statusMsg.isNotEmpty()) {
                    StatusMessageBanner(message = statusMsg, onDismiss = { statusMsg = "" })
                }

                ModernCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                                StatusDot(state = if (status?.running == true) DotState.Active else DotState.Error)
                                Text(
                                    if (status?.running == true) "Running" else "Stopped",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (status?.running == true) appColors.success else MaterialTheme.colorScheme.error
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { showStartApConfirm = true },
                                    shape = MaterialTheme.shapes.small,
                                    colors = ButtonDefaults.buttonColors(containerColor = appColors.success),
                                    enabled = status?.running != true,
                                    modifier = Modifier.height(44.dp)
                                ) { Text("Start") }
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            repo.stopAp()
                                            if (cloneEnabled) repo.stopClone()
                                            statusMsg = "Evil AP stopped"
                                            refresh()
                                        }
                                    },
                                    shape = MaterialTheme.shapes.small,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    enabled = status?.running == true,
                                    modifier = Modifier.height(44.dp)
                                ) { Text("Stop") }
                            }
                        }
                    }
                }

                ModernCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Basic Setup", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("AP Interface", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                DropdownSelector(availableInterfaces, selectedIface, onSelected = { selectedIface = it })
                                Text("Channel (1-14)", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                OutlinedTextField(value = channel, onValueChange = { channel = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = MaterialTheme.typography.bodySmall)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("SSID Name", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                OutlinedTextField(value = ssid, onValueChange = { ssid = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = MaterialTheme.typography.bodySmall)
                                Text("Security", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                DropdownSelector(listOf("open", "wpa"), if (security == "open") "OPEN" else "WPA-PSK", { v -> security = if (v == "OPEN") "open" else "wpa" }, valueToLabel = { if (it == "open") "OPEN" else "WPA-PSK" })
                                if (security == "wpa") {
                                    Text("Password", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                    OutlinedTextField(value = apPassword, onValueChange = { apPassword = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                ModernCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Portal Template", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        DropdownSelector(availablePortals, selectedPortal, onSelected = { selectedPortal = it })
                        InfoCard("Portal files are managed from Storage page — upload your own .html there.")
                    }
                }

                ModernCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("WPA Validate", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ToggleButton(if (wpaEnabled) "YES" else "NO", wpaEnabled, { wpaEnabled = !wpaEnabled }, activeColor = appColors.success)
                            Text("Verify password by connecting to real AP", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusDot(state = when (wpaStatus) { "running" -> DotState.Active; "success" -> DotState.Active; "failed" -> DotState.Error; else -> DotState.Inactive })
                            Text(
                                when (wpaStatus) { "running" -> "Verifying..."; "success" -> "Password Validated"; "failed" -> "Validation Failed"; else -> "Idle" },
                                style = MaterialTheme.typography.bodySmall, color = appColors.subtleText
                            )
                        }
                        if (wpaEnabled) {
                            Text("Verify Interface", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                            DropdownSelector(availableInterfaces, wpaIface, onSelected = { wpaIface = it })
                            InfoCard("Same as AP IF — AP stops during verification. Fail → auto-restart.")
                        }
                    }
                }

                ModernCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Karma Attack", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ToggleButton(if (karmaEnabled) "YES" else "NO", karmaEnabled, { karmaEnabled = !karmaEnabled }, activeColor = appColors.success)
                            Text("Respond to all probe requests", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                        }
                        if (karmaEnabled) {
                            Text("Karma Interface", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                            DropdownSelector(availableInterfaces, karmaIface, onSelected = { karmaIface = it })
                        }
                    }
                }

                ModernCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Clone AP (Evil Twin)", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ToggleButton(if (cloneEnabled) "YES" else "NO", cloneEnabled, { cloneEnabled = !cloneEnabled }, activeColor = appColors.success)
                            Text("Dual-IF: deauth original + clone SSID/BSSID", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                        }
                        if (cloneEnabled) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Deauth Interface", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                    DropdownSelector(availableInterfaces, deauthIface, onSelected = { deauthIface = it })
                                    Text("Clone Interface", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                    DropdownSelector(availableInterfaces, cloneIface, onSelected = { cloneIface = it })
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Target BSSID", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                    OutlinedTextField(value = targetBssid, onValueChange = { targetBssid = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = MaterialTheme.typography.bodySmall, placeholder = { Text("AA:BB:CC:DD:EE:FF", style = MaterialTheme.typography.bodySmall) })
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Target SSID (auto-fill)", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                    OutlinedTextField(value = targetSsid, onValueChange = { targetSsid = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = MaterialTheme.typography.bodySmall, placeholder = { Text("Same as SSID if blank", style = MaterialTheme.typography.bodySmall) })
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        scanningTargets = true
                                        targetScanResults = repo.scanTargets(deauthIface).getOrNull() ?: emptyList()
                                        scanningTargets = false
                                    }
                                },
                                shape = MaterialTheme.shapes.small,
                                enabled = !scanningTargets,
                                modifier = Modifier.height(44.dp)
                            ) {
                                if (scanningTargets) { CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp) }
                                else { Text("Scan for Targets", style = MaterialTheme.typography.bodySmall) }
                            }
                            if (targetScanResults.isNotEmpty()) {
                                Text("Select Target", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                val targetLabels = targetScanResults.map { "${it.ssif()} (${it.bssid}) ch${it.channel}" }
                                val selectedTargetIdx = targetScanResults.indexOfFirst { it.bssid == targetBssid }.let { if (it < 0) 0 else it }
                                DropdownSelector(targetLabels, targetLabels.getOrElse(selectedTargetIdx) { "-- Select --" }, { idx ->
                                    val i = targetLabels.indexOf(idx)
                                    if (i >= 0) {
                                        targetBssid = targetScanResults[i].bssid
                                        targetSsid = targetScanResults[i].ssid
                                        if (targetScanResults[i].channel.isNotEmpty()) channel = targetScanResults[i].channel
                                    }
                                })
                            }
                            WarningCard("Requires 2 Wi-Fi interfaces (built-in + USB dongle)")
                        }
                    }
                }

                if (status?.running == true) {
                    ModernCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Live Status", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column { Text("Clients", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText); Text("${clients.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                                Column { Text("Passwords", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText); Text("${creds.size}", fontWeight = FontWeight.Bold, color = appColors.warning) }
                                Column { Text("Mode", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText); Text(status?.mode ?: "Basic", fontWeight = FontWeight.Bold, color = appColors.info) }
                            }
                        }
                    }
                }

                ModernCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Connected Clients", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        if (clients.isEmpty()) {
                            Text("No clients connected", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("IP", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = appColors.subtleText)
                                    Text("MAC", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = appColors.subtleText)
                                    Text("Hostname", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = appColors.subtleText)
                                    Text("", modifier = Modifier.width(60.dp))
                                }
                                clients.forEach { client ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(client.ip, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(client.mac, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(client.hostname.ifEmpty { "-" }, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        TextButton(onClick = { showDeauthConfirm = client }, contentPadding = PaddingValues(4.dp), modifier = Modifier.width(60.dp)) {
                                            Text("Deauth", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, maxLines = 1)
                                        }
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }

                ModernCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Captured Passwords", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                            OutlinedButton(onClick = { scope.launch { refresh() } }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), modifier = Modifier.height(36.dp)) { Text("Refresh", style = MaterialTheme.typography.bodySmall) }
                        }
                        if (creds.isEmpty()) {
                            Text("No credentials captured yet", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                        } else {
                            creds.forEach { cred ->
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(cred.time, style = MaterialTheme.typography.bodySmall, color = appColors.subtleText, modifier = Modifier.weight(1f))
                                    Text(cred.password, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                                    CopyButton(textToCopy = cred.password, size = 18.dp)
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }

                ModernCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Live Monitor", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        val tabTitles = listOf("DNS", "HTTP", "Devices")
                        TabRow(
                            selectedTabIndex = monitorTab,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ) {
                            tabTitles.forEachIndexed { idx, title ->
                                Tab(
                                    selected = monitorTab == idx,
                                    onClick = { monitorTab = idx },
                                    text = { Text(title, style = MaterialTheme.typography.bodySmall) }
                                )
                            }
                        }
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = appColors.terminalBackground,
                            tonalElevation = 0.dp
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                when (monitorTab) {
                                    0 -> {
                                        val dns = monitorData?.dns ?: emptyList()
                                        if (dns.isEmpty()) Text("No DNS queries yet", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                        else dns.take(20).forEach { Text("${it.domain} from ${it.client}", style = MaterialTheme.typography.bodySmall, color = appColors.terminalText) }
                                    }
                                    1 -> {
                                        val http = monitorData?.http ?: emptyList()
                                        if (http.isEmpty()) Text("No HTTP requests yet", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                        else http.take(20).forEach { Text("${it.url} (${it.os})", style = MaterialTheme.typography.bodySmall, color = appColors.terminalText) }
                                    }
                                    2 -> {
                                        val devices = monitorData?.devices ?: emptyList()
                                        if (devices.isEmpty()) Text("No devices detected yet", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                        else devices.take(20).forEach { Text("${it.hostname} (${it.mac}) - ${it.ip}", style = MaterialTheme.typography.bodySmall, color = appColors.terminalText) }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showStartApConfirm) {
        AlertDialog(
            onDismissRequest = { showStartApConfirm = false },
            title = { Text("Start Evil AP?") },
            text = { Text("Starting Evil AP will take over the Wi-Fi interface ($selectedIface). Other Wi-Fi operations (handshake capture, PMKID, WPS) will stop. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    showStartApConfirm = false
                    scope.launch {
                        val params = mutableMapOf<String, Any>(
                            "ssid" to ssid, "channel" to (channel.toIntOrNull() ?: 6),
                            "interface" to selectedIface, "portal" to selectedPortal
                        )
                        if (security == "wpa" && apPassword.isNotEmpty()) params["password"] = apPassword
                        params["wpa_validate"] = wpaEnabled; params["wpa_interface"] = wpaIface
                        params["karma"] = karmaEnabled; params["karma_interface"] = karmaIface
                        params["clone"] = cloneEnabled; params["deauth_interface"] = deauthIface
                        params["clone_interface"] = cloneIface; params["target_bssid"] = targetBssid
                        params["target_ssid"] = targetSsid; params["target_channel"] = (channel.toIntOrNull() ?: 6)
                        val r = repo.startAp(params)
                        statusMsg = if (r.isSuccess) "Evil AP started on $selectedIface" else (r.exceptionOrNull()?.message ?: "Failed")
                        refresh()
                    }
                }) { Text("Start AP") }
            },
            dismissButton = { TextButton(onClick = { showStartApConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showDeauthConfirm != null) {
        val client = showDeauthConfirm!!
        AlertDialog(
            onDismissRequest = { showDeauthConfirm = null },
            title = { Text("Deauth Client?") },
            text = { Text("Send deauth packets to ${client.mac} (${client.ip})? They will temporarily lose connection to the Evil AP.") },
            confirmButton = { TextButton(onClick = { showDeauthConfirm = null; scope.launch { wifiRepo.deauth(client.mac, "ff:ff:ff:ff:ff:ff", 5, channel.toIntOrNull() ?: 6); statusMsg = "Deauth sent to ${client.mac}" } }) { Text("Deauth", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeauthConfirm = null }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSelector(
    items: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    valueToLabel: ((String) -> String)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = valueToLabel?.invoke(selected) ?: selected
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(value = label, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), textStyle = MaterialTheme.typography.bodySmall, singleLine = true)
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(valueToLabel?.invoke(item) ?: item) },
                    onClick = { onSelected(item); expanded = false }
                )
            }
        }
    }
}
private fun ScanTargetResult.ssif(): String = ssid.ifEmpty { "[Hidden]" }
