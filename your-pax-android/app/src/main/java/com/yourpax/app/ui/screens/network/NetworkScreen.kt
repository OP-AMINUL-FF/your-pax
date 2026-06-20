package com.yourpax.app.ui.screens.network

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourpax.app.data.api.models.NetworkScanResponse
import com.yourpax.app.data.repository.NetworkRepository
import com.yourpax.app.ui.components.DemoModeBanner
import com.yourpax.app.ui.components.EmptyState
import com.yourpax.app.ui.components.FontSizeControl
import com.yourpax.app.ui.components.LoadingOverlay
import com.yourpax.app.ui.components.ModernCard
import com.yourpax.app.ui.components.StatusMessageBanner
import com.yourpax.app.ui.theme.rememberAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BRUTEFORCE_PROTOCOLS = listOf("all", "ssh", "ftp", "telnet", "smb", "sql", "rdp")

private enum class NetworkAction {
    SCAN, STOP_ALL, VULN_SCAN, STEAL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
    onOpenDrawer: () -> Unit = {},
    onNavigateToNetworkDetail: (String) -> Unit = {}
) {
    val repo = remember { NetworkRepository() }
    val scope = rememberCoroutineScope()
    var scanData by remember { mutableStateOf<NetworkScanResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showToolbar by remember { mutableStateOf(true) }
    var fontSize by remember { mutableFloatStateOf(13f) }
    var statusMsg by remember { mutableStateOf("") }
    var confirmAction by remember { mutableStateOf<NetworkAction?>(null) }
    var showBruteforceDialog by remember { mutableStateOf(false) }
    var bruteforceProtocol by remember { mutableStateOf("all") }
    var runningOps by remember { mutableStateOf<Set<String>>(emptySet()) }

    fun triggerOp(op: String, launch: suspend () -> Result<*>) {
        runningOps = runningOps + op
        scope.launch {
            launch()
            delay(10000)
            runningOps = runningOps - op
        }
    }

    LaunchedEffect(Unit) {
        scanData = repo.getNetworkData().getOrNull()
        isLoading = false
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60000)
            scanData = repo.getNetworkData().getOrNull()
        }
    }

    val appColors = rememberAppColors()

    Column(modifier = Modifier.fillMaxSize()) {
        if (showToolbar) {
            DemoModeBanner()
            TopAppBar(
                title = { Text("Network", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            repo.triggerScan()
                            delay(1000)
                            scanData = repo.getNetworkData().getOrNull()
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
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = { scope.launch { scanData = repo.getNetworkData().getOrNull() } }) { Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = appColors.subtleText) }
                    IconButton(onClick = { showToolbar = true }) { Icon(Icons.Default.FullscreenExit, contentDescription = "Show Toolbar", tint = appColors.subtleText) }
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (statusMsg.isNotEmpty()) {
                StatusMessageBanner(message = statusMsg)
            }

            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Quick Actions", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = false, onClick = { confirmAction = NetworkAction.SCAN }, label = { Text("Scan Now") }, leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(16.dp)) }, modifier = Modifier.height(36.dp))
                        FilterChip(selected = false, onClick = { confirmAction = NetworkAction.STOP_ALL }, label = { Text("Stop All") }, leadingIcon = { Icon(Icons.Default.Block, null, Modifier.size(16.dp)) }, modifier = Modifier.height(36.dp))
                        FilterChip(selected = false, onClick = { confirmAction = NetworkAction.VULN_SCAN }, label = { Text("Vuln Scan") }, leadingIcon = { Icon(Icons.Default.Security, null, Modifier.size(16.dp)) }, modifier = Modifier.height(36.dp))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = false, onClick = { showBruteforceDialog = true }, label = { Text("Bruteforce") }, leadingIcon = { Icon(Icons.Default.Lock, null, Modifier.size(16.dp)) }, modifier = Modifier.height(36.dp))
                        FilterChip(selected = false, onClick = { confirmAction = NetworkAction.STEAL }, label = { Text("Steal") }, leadingIcon = { Icon(Icons.Default.Download, null, Modifier.size(16.dp)) }, modifier = Modifier.height(36.dp))
                    }
                }
            }

            if (runningOps.isNotEmpty()) {
                ModernCard {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Running: ${runningOps.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            if (isLoading) {
                LoadingOverlay()
            } else if (scanData == null || scanData!!.rows.isEmpty()) {
                EmptyState(title = "No network data", subtitle = "Run a scan to discover hosts")
            } else {
                val data = scanData!!
                val ipIndex = remember(data.headers) { data.headers.indexOfFirst { it.equals("IPs", true) || it.equals("IP", true) } }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${data.rows.size} hosts", style = MaterialTheme.typography.labelSmall, color = appColors.subtleText)
                    FontSizeControl(
                        currentSize = fontSize,
                        onDecrease = { fontSize = maxOf(8f, fontSize - 1f) },
                        onIncrease = { fontSize = minOf(24f, fontSize + 1f) },
                        minSize = 8f
                    )
                }

                val scrollState = rememberScrollState()
                Column(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
                    Row {
                        data.headers.forEachIndexed { _, header ->
                            Box(modifier = Modifier.border(0.5.dp, MaterialTheme.colorScheme.outlineVariant).background(appColors.infoContainer).padding(horizontal = 10.dp, vertical = 6.dp).defaultMinSize(minWidth = 80.dp), contentAlignment = Alignment.CenterStart) {
                                Text(header, fontWeight = FontWeight.Bold, fontSize = fontSize.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    data.rows.forEach { row ->
                        val hostIp = if (ipIndex in row.indices) row[ipIndex] else ""
                        Row(modifier = Modifier.clickable(enabled = hostIp.isNotBlank()) { onNavigateToNetworkDetail(hostIp) }) {
                            row.forEachIndexed { _, cell ->
                                val isEmpty = cell.isBlank()
                                Box(modifier = Modifier.border(0.5.dp, MaterialTheme.colorScheme.outlineVariant).background(when { isEmpty -> appColors.warningContainer; cell.contains("success") -> appColors.successContainer; cell.contains("failed") -> appColors.warningContainer; else -> MaterialTheme.colorScheme.surface }).padding(horizontal = 8.dp, vertical = 4.dp).defaultMinSize(minWidth = 80.dp), contentAlignment = Alignment.CenterStart) {
                                    Text(cell.ifEmpty { "-" }, fontSize = fontSize.sp, fontFamily = FontFamily.Monospace, color = when { isEmpty -> appColors.subtleText; cell.contains("success") -> appColors.success; cell.contains("failed") -> MaterialTheme.colorScheme.error; else -> MaterialTheme.colorScheme.onSurface }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    when (confirmAction) {
        NetworkAction.SCAN -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Trigger Network Scan?") },
            text = { Text("Start a full network scan now? This will discover new hosts and open ports.") },
            confirmButton = { TextButton(onClick = { confirmAction = null; triggerOp("scan") { repo.triggerScan().also { statusMsg = if (it.isSuccess) "Scan triggered" else "Scan failed" } } }) { Text("Scan") } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
        NetworkAction.STOP_ALL -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Stop All Attacks?") },
            text = { Text("Stop all running attacks including scans, bruteforce, handshake capture, and WPS attacks?") },
            confirmButton = { TextButton(onClick = { confirmAction = null; scope.launch { repo.stopAll().onSuccess { statusMsg = "All stopped" }.onFailure { statusMsg = "Failed: ${it.message}" } } }) { Text("Stop All", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
        NetworkAction.VULN_SCAN -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Run Vulnerability Scan?") },
            text = { Text("Run nmap --script vuln on all targets? This may take a while.") },
            confirmButton = { TextButton(onClick = { confirmAction = null; triggerOp("vuln scan") { repo.triggerVulnScan().also { statusMsg = if (it.isSuccess) "Vuln scan triggered" else "Vuln scan failed" } } }) { Text("Scan") } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
        NetworkAction.STEAL -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Attempt to Steal Files?") },
            text = { Text("Attempt to steal files from all cracked targets using stored credentials?") },
            confirmButton = { TextButton(onClick = { confirmAction = null; triggerOp("steal") { repo.triggerSteal().also { statusMsg = if (it.isSuccess) "Steal triggered" else "Steal failed" } } }) { Text("Steal") } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
        null -> {}
    }

    if (showBruteforceDialog) {
        AlertDialog(
            onDismissRequest = { showBruteforceDialog = false },
            title = { Text("Bruteforce Attack") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select protocol to bruteforce:", style = MaterialTheme.typography.bodySmall)
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(value = bruteforceProtocol.uppercase(), onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), textStyle = MaterialTheme.typography.bodySmall, singleLine = true, label = { Text("Protocol") })
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            BRUTEFORCE_PROTOCOLS.forEach { proto ->
                                DropdownMenuItem(text = { Text(proto.uppercase(), style = MaterialTheme.typography.bodySmall) }, onClick = { bruteforceProtocol = proto; expanded = false })
                            }
                        }
                    }
                    Text(if (bruteforceProtocol == "all") "Will bruteforce all 6 protocols (SSH, FTP, Telnet, SMB, SQL, RDP) on alive targets." else "Will bruteforce ${bruteforceProtocol.uppercase()} on alive targets.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val proto = bruteforceProtocol
                    showBruteforceDialog = false
                    triggerOp("bruteforce") { repo.triggerBruteforce(proto).also { statusMsg = if (it.isSuccess) "Bruteforce ($proto) triggered" else "Bruteforce failed" } }
                }) { Text("Start Bruteforce") }
            },
            dismissButton = { TextButton(onClick = { showBruteforceDialog = false }) { Text("Cancel") } }
        )
    }
}
