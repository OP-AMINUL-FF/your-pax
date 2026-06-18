package com.yourpax.app.ui.screens.manual

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
import androidx.compose.ui.unit.dp
import com.yourpax.app.data.demo.ConnectionState
import com.yourpax.app.data.repository.NetworkRepository
import com.yourpax.app.data.repository.SystemRepository
import com.yourpax.app.ui.components.*
import com.yourpax.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManualModeScreen() {
    val configRepo = remember { com.yourpax.app.data.repository.ConfigRepository() }
    val systemRepo = remember { SystemRepository() }
    val networkRepo = remember { NetworkRepository() }
    val scope = rememberCoroutineScope()
    var command by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isManualMode by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf("") }

    var netkbIps by remember { mutableStateOf<List<String>>(emptyList()) }
    var netkbPorts by remember { mutableStateOf<List<String>>(emptyList()) }
    var netkbActions by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedIp by remember { mutableStateOf("") }
    var selectedPort by remember { mutableStateOf("") }
    var selectedAction by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val cfg = configRepo.loadConfig().getOrNull()
        isManualMode = cfg?.manualMode ?: false
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
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DemoModeBanner()
        TopAppBar(
            title = { Text("Manual Mode", fontWeight = FontWeight.SemiBold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        if (isLoading) {
            LoadingOverlay()
        } else {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!isManualMode && !ConnectionState.isDemoMode) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.15f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Manual Mode Disabled", fontWeight = FontWeight.SemiBold, color = Warning)
                        Text("Enable Manual Mode in Config to execute custom commands.", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                    }
                }
            }

            if (statusMsg.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Info.copy(alpha = 0.1f))) {
                    Text(statusMsg, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                }
            }

            if (isManualMode || ConnectionState.isDemoMode) {
                if (netkbIps.isNotEmpty()) {
                    ModernCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Targeted Attack", fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text("IP", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                                    var expandedIp by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(expanded = expandedIp, onExpandedChange = { expandedIp = it }) {
                                        OutlinedTextField(value = selectedIp, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), textStyle = MaterialTheme.typography.bodySmall, singleLine = true)
                                        ExposedDropdownMenu(expanded = expandedIp, onDismissRequest = { expandedIp = false }) {
                                            netkbIps.forEach { ip -> DropdownMenuItem(text = { Text(ip) }, onClick = { selectedIp = ip; expandedIp = false }) }
                                        }
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("Port", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                                    var expandedPort by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(expanded = expandedPort, onExpandedChange = { expandedPort = it }) {
                                        OutlinedTextField(value = selectedPort, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), textStyle = MaterialTheme.typography.bodySmall, singleLine = true)
                                        ExposedDropdownMenu(expanded = expandedPort, onDismissRequest = { expandedPort = false }) {
                                            netkbPorts.forEach { port -> DropdownMenuItem(text = { Text(port) }, onClick = { selectedPort = port; expandedPort = false }) }
                                        }
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("Action", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                                    var expandedAct by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(expanded = expandedAct, onExpandedChange = { expandedAct = it }) {
                                        OutlinedTextField(value = selectedAction, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), textStyle = MaterialTheme.typography.bodySmall, singleLine = true)
                                        ExposedDropdownMenu(expanded = expandedAct, onDismissRequest = { expandedAct = false }) {
                                            netkbActions.forEach { act -> DropdownMenuItem(text = { Text(act) }, onClick = { selectedAction = act; expandedAct = false }) }
                                        }
                                    }
                                }
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        systemRepo.executeManualAttack(selectedIp, selectedPort, selectedAction).onSuccess {
                                            statusMsg = "Attack executed on $selectedIp:$selectedPort"
                                        }.onFailure {
                                            statusMsg = "Attack failed: ${it.message}"
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                enabled = selectedIp.isNotBlank()
                            ) { Text("Execute Attack") }
                        }
                    }
                }

                ModernCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Execute Command", fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = command,
                            onValueChange = { command = it },
                            label = { Text("Command") },
                            placeholder = { Text("e.g., scan --target 192.168.1.0/24") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                if (command.isNotEmpty()) {
                                    IconButton(onClick = { command = "" }) { Icon(Icons.Default.Clear, contentDescription = "Clear") }
                                }
                            }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        if (command.isBlank()) { statusMsg = "Enter a command"; return@launch }
                                        output += "> $command\n"
                                        statusMsg = "Executing..."
                                        val r = systemRepo.executeCommand(mapOf("command" to command))
                                        if (r.isSuccess) {
                                            output += "${r.getOrNull()?.message ?: "Command executed"}\n"
                                            statusMsg = "Done"
                                        } else {
                                            output += "Error: ${r.exceptionOrNull()?.message ?: "Failed"}\n"
                                            statusMsg = "Failed"
                                        }
                                    }
                                },
                                enabled = command.isNotBlank(),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Execute") }
                            OutlinedButton(
                                onClick = { output = ""; statusMsg = "Output cleared" },
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Clear Output") }
                        }
                    }
                }

                ModernCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Output", fontWeight = FontWeight.SemiBold)
                            if (output.isNotEmpty()) {
                                TextButton(onClick = { output = "" }) { Text("Clear", style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 400.dp)
                        ) {
                            Text(
                                text = output.ifEmpty { "Output will appear here..." },
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = if (output.isEmpty()) SubtleText else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                ModernCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Quick Commands", fontWeight = FontWeight.SemiBold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuickChip("scan --target 192.168.1.0/24", onClick = { command = "scan --target 192.168.1.0/24" })
                            QuickChip("bruteforce --protocol ssh", onClick = { command = "bruteforce --protocol ssh" })
                            QuickChip("steal --path /home", onClick = { command = "steal --path /home" })
                            QuickChip("vulnscan --target 192.168.1.1", onClick = { command = "vulnscan --target 192.168.1.1" })
                            QuickChip("handshake --bssid AA:BB:CC:DD:EE:FF", onClick = { command = "handshake --bssid AA:BB:CC:DD:EE:FF" })
                            QuickChip("deauth --bssid AA:BB:CC:DD:EE:FF", onClick = { command = "deauth --bssid AA:BB:CC:DD:EE:FF" })
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
    }
}

@Composable
private fun QuickChip(text: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(text, style = MaterialTheme.typography.bodySmall) },
        shape = RoundedCornerShape(8.dp)
    )
}
