package com.yourpax.app.ui.screens.wifi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourpax.app.data.api.models.*
import com.yourpax.app.data.repository.EvilApRepository
import com.yourpax.app.data.repository.LootRepository
import com.yourpax.app.data.repository.WiFiRepository
import com.yourpax.app.ui.components.*
import com.yourpax.app.ui.theme.*
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EvilAPScreen() {
    val repo = remember { EvilApRepository() }
    val lootRepo = remember { LootRepository() }
    val wifiRepo = remember { WiFiRepository() }
    val scope = rememberCoroutineScope()
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
    var monitorTab by remember { mutableStateOf("dns") }
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
        while (true) {
            refresh()
            delay(3000)
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(modifier = Modifier.fillMaxSize()) {
        DemoModeBanner()
        TopAppBar(
            title = { Text("Evil AP Control Panel", fontWeight = FontWeight.SemiBold) },
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
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            conflict?.let { c ->
                val conflicts = buildList {
                    if (c.handshakeRunning) add("Handshake capture")
                    if (c.pmkidRunning) add("PMKID capture")
                    if (c.oneshotRunning) add("WPS attack")
                    if (c.monitorMode) add("Monitor mode")
                }
                if (conflicts.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF330000))) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF6666), modifier = Modifier.size(16.dp))
                            Text("Conflict: ${conflicts.joinToString(", ")}. Stop them first.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF6666))
                        }
                    }
                }
            }

            if (statusMsg.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Info.copy(alpha = 0.1f))) {
                    Text(statusMsg, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                }
            }

            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusDot(isActive = status?.running == true)
                            Text(if (status?.running == true) "Running" else "Stopped", fontWeight = FontWeight.SemiBold, color = if (status?.running == true) Success else Error)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { showStartApConfirm = true }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Success), enabled = status?.running != true) { Text("Start") }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    repo.stopAp()
                                    if (cloneEnabled) repo.stopClone()
                                    statusMsg = "Evil AP stopped"
                                    refresh()
                                }
                            }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Error), enabled = status?.running == true) { Text("Stop") }
                        }
                    }
                }
            }

            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Basic Setup", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("AP Interface", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                            var expandedIface by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = expandedIface, onExpandedChange = { expandedIface = it }) {
                                OutlinedTextField(value = selectedIface, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), textStyle = MaterialTheme.typography.bodySmall, singleLine = true)
                                ExposedDropdownMenu(expanded = expandedIface, onDismissRequest = { expandedIface = false }) {
                                    availableInterfaces.forEach { iface -> DropdownMenuItem(text = { Text(iface) }, onClick = { selectedIface = iface; expandedIface = false }) }
                                }
                            }
                            Text("Channel (1-14)", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                            OutlinedTextField(value = channel, onValueChange = { channel = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = MaterialTheme.typography.bodySmall)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("SSID Name", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                            OutlinedTextField(value = ssid, onValueChange = { ssid = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = MaterialTheme.typography.bodySmall)
                            Text("Security", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                            var expandedSec by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = expandedSec, onExpandedChange = { expandedSec = it }) {
                                OutlinedTextField(value = if (security == "open") "OPEN" else "WPA-PSK", onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), textStyle = MaterialTheme.typography.bodySmall, singleLine = true)
                                ExposedDropdownMenu(expanded = expandedSec, onDismissRequest = { expandedSec = false }) {
                                    DropdownMenuItem(text = { Text("OPEN") }, onClick = { security = "open"; expandedSec = false })
                                    DropdownMenuItem(text = { Text("WPA-PSK") }, onClick = { security = "wpa"; expandedSec = false })
                                }
                            }
                            if (security == "wpa") {
                                Text("Password (if WPA)", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                                OutlinedTextField(value = apPassword, onValueChange = { apPassword = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Portal Template", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    var expandedPortal by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedPortal, onExpandedChange = { expandedPortal = it }) {
                        OutlinedTextField(value = selectedPortal, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), textStyle = MaterialTheme.typography.bodySmall, singleLine = true)
                        ExposedDropdownMenu(expanded = expandedPortal, onDismissRequest = { expandedPortal = false }) {
                            availablePortals.forEach { portal -> DropdownMenuItem(text = { Text(portal) }, onClick = { selectedPortal = portal; expandedPortal = false }) }
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF002200)), shape = RoundedCornerShape(4.dp)) {
                        Text("Portal files are managed from Storage page — upload your own .html there.", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, color = Color(0xFF88FF88))
                    }
                }
            }

            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("WPA Validate", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToggleButton(active = wpaEnabled, label = if (wpaEnabled) "YES" else "NO", onClick = { wpaEnabled = !wpaEnabled })
                        Text("Verify password by connecting to real AP", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(when (wpaStatus) { "running" -> Warning; "success" -> Success; "failed" -> Error; else -> SubtleText }))
                        Text(when (wpaStatus) { "running" -> "Verifying..."; "success" -> "Password Validated"; "failed" -> "Validation Failed"; else -> "Idle" }, style = MaterialTheme.typography.bodySmall, color = SubtleText)
                    }
                    if (wpaEnabled) {
                        Text("Verify Interface", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                        var expandedWpa by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expandedWpa, onExpandedChange = { expandedWpa = it }) {
                            OutlinedTextField(value = wpaIface, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), textStyle = MaterialTheme.typography.bodySmall, singleLine = true)
                            ExposedDropdownMenu(expanded = expandedWpa, onDismissRequest = { expandedWpa = false }) {
                                availableInterfaces.forEach { iface -> DropdownMenuItem(text = { Text(iface) }, onClick = { wpaIface = iface; expandedWpa = false }) }
                            }
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF002200)), shape = RoundedCornerShape(4.dp)) {
                            Text("Same as AP IF → AP stops during verification. Fail → auto-restart.", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, color = Color(0xFF88FF88))
                        }
                    }
                }
            }

            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Karma Attack", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToggleButton(active = karmaEnabled, label = if (karmaEnabled) "YES" else "NO", onClick = { karmaEnabled = !karmaEnabled })
                        Text("Respond to all probe requests", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                    }
                    if (karmaEnabled) {
                        Text("Karma Interface", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                        var expandedKarma by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expandedKarma, onExpandedChange = { expandedKarma = it }) {
                            OutlinedTextField(value = karmaIface, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), textStyle = MaterialTheme.typography.bodySmall, singleLine = true)
                            ExposedDropdownMenu(expanded = expandedKarma, onDismissRequest = { expandedKarma = false }) {
                                availableInterfaces.forEach { iface -> DropdownMenuItem(text = { Text(iface) }, onClick = { karmaIface = iface; expandedKarma = false }) }
                            }
                        }
                    }
                }
            }

            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Clone AP (Evil Twin)", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToggleButton(active = cloneEnabled, label = if (cloneEnabled) "YES" else "NO", onClick = { cloneEnabled = !cloneEnabled })
                        Text("Dual-IF: deauth original + clone SSID/BSSID", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                    }
                    if (cloneEnabled) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Deauth Interface", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                                var expandedDeauth by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(expanded = expandedDeauth, onExpandedChange = { expandedDeauth = it }) {
                                    OutlinedTextField(value = deauthIface, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), textStyle = MaterialTheme.typography.bodySmall, singleLine = true)
                                    ExposedDropdownMenu(expanded = expandedDeauth, onDismissRequest = { expandedDeauth = false }) {
                                        availableInterfaces.forEach { iface -> DropdownMenuItem(text = { Text(iface) }, onClick = { deauthIface = iface; expandedDeauth = false }) }
                                    }
                                }
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Clone Interface", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                                var expandedClone by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(expanded = expandedClone, onExpandedChange = { expandedClone = it }) {
                                    OutlinedTextField(value = cloneIface, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), textStyle = MaterialTheme.typography.bodySmall, singleLine = true)
                                    ExposedDropdownMenu(expanded = expandedClone, onDismissRequest = { expandedClone = false }) {
                                        availableInterfaces.forEach { iface -> DropdownMenuItem(text = { Text(iface) }, onClick = { cloneIface = iface; expandedClone = false }) }
                                    }
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Target BSSID", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                                OutlinedTextField(value = targetBssid, onValueChange = { targetBssid = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = MaterialTheme.typography.bodySmall, placeholder = { Text("AA:BB:CC:DD:EE:FF", style = MaterialTheme.typography.bodySmall) })
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Target SSID (auto-fill)", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                                OutlinedTextField(value = targetSsid, onValueChange = { targetSsid = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = MaterialTheme.typography.bodySmall, placeholder = { Text("Same as SSID if blank", style = MaterialTheme.typography.bodySmall) })
                            }
                        }
                        OutlinedButton(onClick = {
                            scope.launch {
                                scanningTargets = true
                                val result = repo.scanTargets(deauthIface)
                                targetScanResults = result.getOrNull() ?: emptyList()
                                scanningTargets = false
                            }
                        }, shape = RoundedCornerShape(8.dp), enabled = !scanningTargets) {
                            if (scanningTargets) { CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp) } else { Text("\uD83D\uDD0D Scan for Targets") }
                        }
                        if (targetScanResults.isNotEmpty()) {
                            Text("Select Target", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                            var expandedTarget by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = expandedTarget, onExpandedChange = { expandedTarget = it }) {
                                val selectedLabel = targetScanResults.firstOrNull { it.bssid == targetBssid }?.let { "${it.ssif()} (${it.bssid})" } ?: "-- Select target --"
                                OutlinedTextField(value = selectedLabel, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), textStyle = MaterialTheme.typography.bodySmall, singleLine = true)
                                ExposedDropdownMenu(expanded = expandedTarget, onDismissRequest = { expandedTarget = false }) {
                                    targetScanResults.forEach { target ->
                                        DropdownMenuItem(text = { Text("${target.ssif()} (${target.bssid}) ch${target.channel}") }, onClick = {
                                            targetBssid = target.bssid
                                            targetSsid = target.ssid
                                            if (target.channel.isNotEmpty()) channel = target.channel
                                            expandedTarget = false
                                        })
                                    }
                                }
                            }
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF332200)), shape = RoundedCornerShape(4.dp)) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFFF88), modifier = Modifier.size(14.dp))
                                Text("Requires 2 Wi-Fi interfaces (built-in + USB dongle)", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFFF88))
                            }
                        }
                    }
                }
            }

            if (status?.running == true) {
                ModernCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Live Status", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column { Text("Clients", style = MaterialTheme.typography.bodySmall, color = SubtleText); Text("${clients.size}", fontWeight = FontWeight.Bold, color = Primary) }
                            Column { Text("Passwords", style = MaterialTheme.typography.bodySmall, color = SubtleText); Text("${creds.size}", fontWeight = FontWeight.Bold, color = Warning) }
                            Column { Text("Mode", style = MaterialTheme.typography.bodySmall, color = SubtleText); Text(status?.mode ?: "Basic", fontWeight = FontWeight.Bold, color = Info) }
                        }
                    }
                }
            }

            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Connected Clients", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    if (clients.isEmpty()) {
                        Text("No clients connected", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("IP", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = SubtleText)
                                Text("MAC", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = SubtleText)
                                Text("Hostname", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = SubtleText)
                                Text("", modifier = Modifier.width(60.dp))
                            }
                            clients.forEach { client ->
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(client.ip, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                    Text(client.mac, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                    Text(client.hostname.ifEmpty { "-" }, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    TextButton(onClick = { showDeauthConfirm = client }, contentPadding = PaddingValues(4.dp), modifier = Modifier.width(60.dp)) {
                                        Text("Deauth", style = MaterialTheme.typography.bodySmall, color = Error)
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
                        OutlinedButton(onClick = { scope.launch { refresh() } }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) { Text("Refresh", style = MaterialTheme.typography.bodySmall) }
                    }
                    if (creds.isEmpty()) {
                        Text("No credentials captured yet", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                    } else {
                        creds.forEach { cred ->
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(cred.time, style = MaterialTheme.typography.bodySmall, color = SubtleText, modifier = Modifier.weight(1f))
                                Text(cred.password, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = Color(0xFFFF6666), modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    clipboard.setPrimaryClip(ClipData.newPlainText("password", cred.password))
                                    statusMsg = "Copied password"
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp), tint = SubtleText)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }

            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Live Monitor", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        listOf("dns" to "DNS", "http" to "HTTP", "devices" to "Devices").forEach { (key, label) ->
                            Box(modifier = Modifier
                                .clip(RoundedCornerShape(topStart = if (key == "dns") 4.dp else 0.dp, topEnd = if (key == "devices") 4.dp else 0.dp))
                                .background(if (monitorTab == key) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                .clickable { monitorTab = key }
                                .padding(horizontal = 14.dp, vertical = 6.dp)) {
                                Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = if (monitorTab == key) FontWeight.Medium else FontWeight.Normal, color = if (monitorTab == key) MaterialTheme.colorScheme.onSurface else SubtleText)
                            }
                        }
                    }
                    Card(shape = RoundedCornerShape(topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                            when (monitorTab) {
                                "dns" -> {
                                    val dns = monitorData?.dns ?: emptyList()
                                    if (dns.isEmpty()) { Text("No DNS queries yet", style = MaterialTheme.typography.bodySmall, color = SubtleText) }
                                    else { dns.take(20).forEach { Text("${it.domain} from ${it.client}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF88FF88)) } }
                                }
                                "http" -> {
                                    val http = monitorData?.http ?: emptyList()
                                    if (http.isEmpty()) { Text("No HTTP requests yet", style = MaterialTheme.typography.bodySmall, color = SubtleText) }
                                    else { http.take(20).forEach { Text("${it.url} (${it.os})", style = MaterialTheme.typography.bodySmall, color = Color(0xFF88FF88)) } }
                                }
                                "devices" -> {
                                    val devices = monitorData?.devices ?: emptyList()
                                    if (devices.isEmpty()) { Text("No devices detected yet", style = MaterialTheme.typography.bodySmall, color = SubtleText) }
                                    else { devices.take(20).forEach { Text("${it.hostname} (${it.mac}) - ${it.ip}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF88FF88)) } }
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
            confirmButton = { TextButton(onClick = { showDeauthConfirm = null; scope.launch { wifiRepo.deauth(client.mac, "ff:ff:ff:ff:ff:ff", 5, 1); statusMsg = "Deauth sent to ${client.mac}" } }) { Text("Deauth", color = Error) } },
            dismissButton = { TextButton(onClick = { showDeauthConfirm = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StatusDot(isActive: Boolean) {
    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(if (isActive) Success else Error))
}

@Composable
private fun ToggleButton(active: Boolean, label: String, onClick: () -> Unit) {
    Box(modifier = Modifier
        .clip(RoundedCornerShape(4.dp))
        .background(if (active) Success else Color(0xFF333333))
        .clickable { onClick() }
        .padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = if (active) Color.White else Color(0xFFAAAAAA))
    }
}

private fun ScanTargetResult.ssif(): String = ssid.ifEmpty { "[Hidden]" }
