package com.yourpax.app.ui.screens.manual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.yourpax.app.R
import com.yourpax.app.ui.components.CopyButton
import com.yourpax.app.ui.components.LottieAnim
import com.yourpax.app.data.demo.ConnectionState
import com.yourpax.app.data.repository.NetworkRepository
import com.yourpax.app.data.repository.SystemRepository
import com.yourpax.app.ui.components.DemoModeBanner
import com.yourpax.app.ui.components.LoadingOverlay
import com.yourpax.app.ui.components.ModernCard
import com.yourpax.app.ui.components.StatusMessageBanner
import com.yourpax.app.ui.theme.rememberAppColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManualModeScreen(
    onOpenDrawer: () -> Unit = {}
) {
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

    val appColors = rememberAppColors()

    Column(modifier = Modifier.fillMaxSize()) {
        DemoModeBanner()
        TopAppBar(
            title = { Text("Manual Mode", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        if (isLoading) {
            LoadingOverlay()
        } else {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Manual Execution Panel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    LottieAnim(
                        rawResId = R.raw.terminal_typing,
                        modifier = Modifier.size(36.dp)
                    )
                }

                if (!isManualMode && !ConnectionState.isDemoMode) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = appColors.warningContainer,
                        tonalElevation = 0.dp
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Manual Mode Disabled", fontWeight = FontWeight.SemiBold, color = appColors.warning)
                            Text("Enable Manual Mode in Config to execute custom commands.", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                        }
                    }
                }

                if (statusMsg.isNotEmpty()) {
                    StatusMessageBanner(message = statusMsg)
                }

                if (isManualMode || ConnectionState.isDemoMode) {
                    if (netkbIps.isNotEmpty()) {
                        ModernCard {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Targeted Attack", fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.weight(1f)) {
                                        Text("IP", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                        var expandedIp by remember { mutableStateOf(false) }
                                        ExposedDropdownMenuBox(expanded = expandedIp, onExpandedChange = { expandedIp = it }) {
                                            OutlinedTextField(value = selectedIp, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), textStyle = MaterialTheme.typography.bodySmall, singleLine = true)
                                            ExposedDropdownMenu(expanded = expandedIp, onDismissRequest = { expandedIp = false }) {
                                                netkbIps.forEach { ip -> DropdownMenuItem(text = { Text(ip) }, onClick = { selectedIp = ip; expandedIp = false }) }
                                            }
                                        }
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text("Port", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                        var expandedPort by remember { mutableStateOf(false) }
                                        ExposedDropdownMenuBox(expanded = expandedPort, onExpandedChange = { expandedPort = it }) {
                                            OutlinedTextField(value = selectedPort, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), textStyle = MaterialTheme.typography.bodySmall, singleLine = true)
                                            ExposedDropdownMenu(expanded = expandedPort, onDismissRequest = { expandedPort = false }) {
                                                netkbPorts.forEach { port -> DropdownMenuItem(text = { Text(port) }, onClick = { selectedPort = port; expandedPort = false }) }
                                            }
                                        }
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text("Action", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
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
                                    shape = MaterialTheme.shapes.small,
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
                                    label = { Text("Action Class") },
                                    placeholder = { Text("e.g., SSHBruteforce, WiFiHandshake, NetworkScanner") },
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
                                                if (command.isBlank()) { statusMsg = "Enter an action class"; return@launch }
                                                output += "> execute_manual_attack(ip=\"$selectedIp\", port=\"$selectedPort\", action=\"$command\")\n"
                                                statusMsg = "Executing..."
                                                val r = systemRepo.executeManualAttack(selectedIp, selectedPort, command)
                                                if (r.isSuccess) {
                                                    output += "${r.getOrNull()?.message ?: "Command executed"}\n"
                                                    statusMsg = "Done"
                                                } else {
                                                    output += "Error: ${r.exceptionOrNull()?.message ?: "Failed"}\n"
                                                    statusMsg = "Failed"
                                                }
                                            }
                                        },
                                        enabled = command.isNotBlank() && selectedIp.isNotBlank(),
                                        shape = MaterialTheme.shapes.medium
                                    ) { Text("Execute") }
                                OutlinedButton(
                                    onClick = { output = ""; statusMsg = "Output cleared" },
                                    shape = MaterialTheme.shapes.medium
                                ) { Text("Clear Output") }
                            }
                        }
                    }

                    ModernCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Output", fontWeight = FontWeight.SemiBold)
                                    if (output.isNotEmpty()) {
                                        Spacer(Modifier.width(8.dp))
                                        CopyButton(textToCopy = output)
                                    }
                                }
                                if (output.isNotEmpty()) {
                                    TextButton(onClick = { output = "" }) { Text("Clear", style = MaterialTheme.typography.bodySmall) }
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small,
                                tonalElevation = 0.dp,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 400.dp)
                            ) {
                                Text(
                                    text = output.ifEmpty { "Output will appear here..." },
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (output.isEmpty()) appColors.subtleText else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    ModernCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Quick Commands", fontWeight = FontWeight.SemiBold)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                QuickChip("NetworkScanner", R.raw.attack_scan, onClick = { command = "NetworkScanner" })
                                QuickChip("SSHBruteforce", R.raw.attack_bruteforce, onClick = { command = "SSHBruteforce" })
                                QuickChip("FTPBruteforce", R.raw.attack_bruteforce, onClick = { command = "FTPBruteforce" })
                                QuickChip("SMB Bruteforce", R.raw.attack_bruteforce, onClick = { command = "SMBBruteforce" })
                                QuickChip("WiFiHandshake", R.raw.attack_scan, onClick = { command = "WiFiHandshake" })
                                QuickChip("WiFiPMKID", R.raw.attack_scan, onClick = { command = "WiFiPMKID" })
                                QuickChip("WiFiDeauth", R.raw.attack_deauth, onClick = { command = "WiFiDeauth" })
                                QuickChip("WiFiOneShot", R.raw.attack_deauth, onClick = { command = "WiFiOneShot" })
                                QuickChip("StealFilesSSH", R.raw.attack_steal, onClick = { command = "StealFilesSSH" })
                                QuickChip("StealFilesFTP", R.raw.attack_steal, onClick = { command = "StealFilesFTP" })
                                QuickChip("StealFilesSMB", R.raw.attack_steal, onClick = { command = "StealFilesSMB" })
                                QuickChip("StealDataSQL", R.raw.attack_steal, onClick = { command = "StealDataSQL" })
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
private fun QuickChip(text: String, rawResId: Int, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(text, style = MaterialTheme.typography.bodySmall) },
        leadingIcon = { LottieAnim(rawResId = rawResId, modifier = Modifier.size(16.dp)) },
        shape = MaterialTheme.shapes.small
    )
}
