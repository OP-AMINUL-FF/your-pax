package com.yourpax.app.ui.screens.network

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourpax.app.data.api.models.NetworkScanResponse
import com.yourpax.app.data.repository.NetworkRepository
import com.yourpax.app.ui.components.DemoModeBanner
import com.yourpax.app.ui.components.EmptyState
import com.yourpax.app.ui.components.LoadingOverlay
import com.yourpax.app.ui.components.ModernCard
import com.yourpax.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BRUTEFORCE_PROTOCOLS = listOf("all", "ssh", "ftp", "telnet", "smb", "sql", "rdp")

private enum class NetworkAction {
    SCAN, STOP_ALL, VULN_SCAN, STEAL
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NetworkScreen() {
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

    Column(modifier = Modifier.fillMaxSize()) {
        if (showToolbar) {
            DemoModeBanner()
            TopAppBar(
                title = { Text("Network", fontWeight = FontWeight.SemiBold) },
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
                    IconButton(onClick = { scope.launch { scanData = repo.getNetworkData().getOrNull() } }) { Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = SubtleText) }
                    IconButton(onClick = { showToolbar = true }) { Icon(Icons.Default.FullscreenExit, contentDescription = "Show Toolbar", tint = SubtleText) }
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (statusMsg.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Info.copy(alpha = 0.1f))) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Info, modifier = Modifier.size(14.dp))
                        Text(statusMsg, style = MaterialTheme.typography.bodySmall, color = Info)
                    }
                }
            }

            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Quick Actions", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = false, onClick = { confirmAction = NetworkAction.SCAN }, label = { Text("Scan Now") }, leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(16.dp)) })
                        FilterChip(selected = false, onClick = { confirmAction = NetworkAction.STOP_ALL }, label = { Text("Stop All") }, leadingIcon = { Icon(Icons.Default.Block, null, Modifier.size(16.dp)) })
                        FilterChip(selected = false, onClick = { confirmAction = NetworkAction.VULN_SCAN }, label = { Text("Vuln Scan") }, leadingIcon = { Icon(Icons.Default.Security, null, Modifier.size(16.dp)) })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = false, onClick = { showBruteforceDialog = true }, label = { Text("Bruteforce") }, leadingIcon = { Icon(Icons.Default.Lock, null, Modifier.size(16.dp)) })
                        FilterChip(selected = false, onClick = { confirmAction = NetworkAction.STEAL }, label = { Text("Steal") }, leadingIcon = { Icon(Icons.Default.Download, null, Modifier.size(16.dp)) })
                    }
                }
            }

            if (isLoading) {
                LoadingOverlay()
            } else if (scanData == null || scanData!!.rows.isEmpty()) {
                EmptyState(title = "No network data", subtitle = "Run a scan to discover hosts")
            } else {
                val data = scanData!!
                Text("${data.rows.size} hosts", style = MaterialTheme.typography.labelSmall, color = SubtleText)

                val scrollState = rememberScrollState()
                Column(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
                    Row {
                        data.headers.forEachIndexed { _, header ->
                            Box(modifier = Modifier.border(0.5.dp, DividerColor).background(Primary.copy(alpha = 0.1f)).padding(horizontal = 12.dp, vertical = 8.dp).defaultMinSize(minWidth = 100.dp), contentAlignment = Alignment.CenterStart) {
                                Text(header, fontWeight = FontWeight.Bold, fontSize = fontSize.sp, fontFamily = FontFamily.Monospace, color = Primary)
                            }
                        }
                    }
                    data.rows.forEach { row ->
                        Row {
                            row.forEachIndexed { _, cell ->
                                val isEmpty = cell.isBlank()
                                Box(modifier = Modifier.border(0.5.dp, DividerColor).background(when { isEmpty -> Error.copy(alpha = 0.08f); cell.contains("success") -> Success.copy(alpha = 0.1f); cell.contains("failed") -> Error.copy(alpha = 0.1f); else -> MaterialTheme.colorScheme.surface }).padding(horizontal = 12.dp, vertical = 6.dp).defaultMinSize(minWidth = 100.dp), contentAlignment = Alignment.CenterStart) {
                                    Text(cell.ifEmpty { "-" }, fontSize = fontSize.sp, fontFamily = FontFamily.Monospace, color = when { isEmpty -> SubtleText; cell.contains("success") -> Success; cell.contains("failed") -> Error; else -> MaterialTheme.colorScheme.onSurface })
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
            confirmButton = { TextButton(onClick = { confirmAction = null; scope.launch { repo.triggerScan().onSuccess { statusMsg = "Scan triggered" }.onFailure { statusMsg = "Scan failed: ${it.message}" } } }) { Text("Scan") } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
        NetworkAction.STOP_ALL -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Stop All Attacks?") },
            text = { Text("Stop all running attacks including scans, bruteforce, handshake capture, and WPS attacks?") },
            confirmButton = { TextButton(onClick = { confirmAction = null; scope.launch { repo.stopAll().onSuccess { statusMsg = "All stopped" }.onFailure { statusMsg = "Failed: ${it.message}" } } }) { Text("Stop All", color = Error) } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
        NetworkAction.VULN_SCAN -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Run Vulnerability Scan?") },
            text = { Text("Run nmap --script vuln on all targets? This may take a while.") },
            confirmButton = { TextButton(onClick = { confirmAction = null; scope.launch { repo.triggerVulnScan().onSuccess { statusMsg = "Vuln scan triggered" }.onFailure { statusMsg = "Vuln scan failed: ${it.message}" } } }) { Text("Scan") } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
        NetworkAction.STEAL -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Attempt to Steal Files?") },
            text = { Text("Attempt to steal files from all cracked targets using stored credentials?") },
            confirmButton = { TextButton(onClick = { confirmAction = null; scope.launch { repo.triggerSteal().onSuccess { statusMsg = "Steal triggered" }.onFailure { statusMsg = "Steal failed: ${it.message}" } } }) { Text("Steal") } },
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
                    Text(if (bruteforceProtocol == "all") "Will bruteforce all 6 protocols (SSH, FTP, Telnet, SMB, SQL, RDP) on alive targets." else "Will bruteforce ${bruteforceProtocol.uppercase()} on alive targets.", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val proto = bruteforceProtocol
                    showBruteforceDialog = false
                    scope.launch {
                        repo.triggerBruteforce(proto).onSuccess { statusMsg = "Bruteforce ($proto) triggered" }.onFailure { statusMsg = "Bruteforce failed: ${it.message}" }
                    }
                }) { Text("Start Bruteforce") }
            },
            dismissButton = { TextButton(onClick = { showBruteforceDialog = false }) { Text("Cancel") } }
        )
    }
}
