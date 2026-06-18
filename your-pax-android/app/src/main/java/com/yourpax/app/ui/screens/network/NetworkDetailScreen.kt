package com.yourpax.app.ui.screens.network

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourpax.app.data.repository.NetworkRepository
import com.yourpax.app.data.repository.SystemRepository
import com.yourpax.app.ui.components.DemoModeBanner
import com.yourpax.app.ui.components.ModernCard
import com.yourpax.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDetailScreen(ip: String) {
    val networkRepo = remember { NetworkRepository() }
    val systemRepo = remember { SystemRepository() }
    val scope = rememberCoroutineScope()
    var portData by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var hostInfo by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var statusMsg by remember { mutableStateOf("") }
    var showBruteforceConfirm by remember { mutableStateOf(false) }
    var showVulnScanConfirm by remember { mutableStateOf(false) }
    var showManualAttackConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(ip) {
        val data = networkRepo.getNetworkData().getOrNull()
        if (data != null) {
            val ipIdx = data.headers.indexOfFirst { it.equals("IP", ignoreCase = true) || it.equals("ip", ignoreCase = true) }
            val row = data.rows.firstOrNull { r -> ipIdx >= 0 && r.getOrElse(ipIdx) { "" } == ip }
            if (row != null) {
                val info = mutableMapOf<String, String>()
                val ports = mutableMapOf<String, String>()
                data.headers.forEachIndexed { i, h ->
                    val v = row.getOrElse(i) { "" }
                    if (h.equals("IP", ignoreCase = true) || h.equals("MAC", ignoreCase = true) || h.equals("OS", ignoreCase = true) || h.equals("Hostname", ignoreCase = true)) {
                        if (v.isNotBlank()) info[h] = v
                    } else if (v.isNotBlank()) {
                        ports[h] = v
                    }
                }
                hostInfo = info
                portData = ports
            }
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DemoModeBanner()
        TopAppBar(
            title = { Text("Host: $ip", fontWeight = FontWeight.SemiBold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        if (statusMsg.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Info.copy(alpha = 0.1f))) {
                Text(statusMsg, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, color = Info)
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (hostInfo.isEmpty() && portData.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No data found for $ip", color = SubtleText)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ModernCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Host Information", fontWeight = FontWeight.SemiBold, color = Primary)
                            hostInfo.forEach { (key, value) ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(key, style = MaterialTheme.typography.bodySmall, color = SubtleText)
                                    Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                item {
                    ModernCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Open Ports", fontWeight = FontWeight.SemiBold, color = Primary)
                            if (portData.isEmpty()) {
                                Text("No open ports detected", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                            } else {
                                portData.forEach { (port, service) ->
                                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(16.dp))
                                            Text(port, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Medium)
                                        }
                                        Text(service, style = MaterialTheme.typography.bodySmall, color = SubtleText)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    ModernCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Actions", fontWeight = FontWeight.SemiBold, color = Primary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            statusMsg = "Triggering scan..."
                                            networkRepo.triggerScan().onSuccess { statusMsg = "Scan triggered" }.onFailure { statusMsg = "Scan failed: ${it.message}" }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Scan Ports") }

                                Button(
                                    onClick = { showBruteforceConfirm = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Error)
                                ) { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Bruteforce") }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showVulnScanConfirm = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Warning)
                                ) { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Vuln Scan") }

                                OutlinedButton(
                                    onClick = { showManualAttackConfirm = true },
                                    shape = RoundedCornerShape(12.dp)
                                ) { Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Manual Attack") }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showBruteforceConfirm) {
        AlertDialog(
            onDismissRequest = { showBruteforceConfirm = false },
            title = { Text("Bruteforce $ip?") },
            text = { Text("Start bruteforce attack against all alive targets? This will try credential combinations against open ports.") },
            confirmButton = { TextButton(onClick = { showBruteforceConfirm = false; scope.launch { statusMsg = "Triggering bruteforce..."; networkRepo.triggerBruteforce().onSuccess { statusMsg = "Bruteforce triggered" }.onFailure { statusMsg = "Bruteforce failed: ${it.message}" } } }) { Text("Start") } },
            dismissButton = { TextButton(onClick = { showBruteforceConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showVulnScanConfirm) {
        AlertDialog(
            onDismissRequest = { showVulnScanConfirm = false },
            title = { Text("Vulnerability Scan $ip?") },
            text = { Text("Run vulnerability scan against this host? This runs nmap --script vuln and may take a while.") },
            confirmButton = { TextButton(onClick = { showVulnScanConfirm = false; scope.launch { statusMsg = "Triggering vuln scan..."; networkRepo.triggerVulnScan().onSuccess { statusMsg = "Vuln scan triggered" }.onFailure { statusMsg = "Vuln scan failed: ${it.message}" } } }) { Text("Scan") } },
            dismissButton = { TextButton(onClick = { showVulnScanConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showManualAttackConfirm) {
        AlertDialog(
            onDismissRequest = { showManualAttackConfirm = false },
            title = { Text("Manual Attack on $ip?") },
            text = { Text("Execute a manual bruteforce attack against ${portData.keys.firstOrNull() ?: "first open port"} on $ip?") },
            confirmButton = { TextButton(onClick = { showManualAttackConfirm = false; scope.launch { val firstPort = portData.keys.firstOrNull() ?: ""; systemRepo.executeManualAttack(ip, firstPort, "bruteforce").onSuccess { statusMsg = "Manual attack executed" }.onFailure { statusMsg = "Attack failed: ${it.message}" } } }) { Text("Execute") } },
            dismissButton = { TextButton(onClick = { showManualAttackConfirm = false }) { Text("Cancel") } }
        )
    }
}
