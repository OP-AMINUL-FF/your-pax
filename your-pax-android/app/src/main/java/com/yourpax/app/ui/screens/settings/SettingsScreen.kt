package com.yourpax.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourpax.app.data.repository.ConfigRepository
import com.yourpax.app.data.repository.NetworkRepository
import com.yourpax.app.data.repository.SystemRepository
import com.yourpax.app.data.repository.WiFiRepository
import com.yourpax.app.ui.components.DemoModeBanner
import com.yourpax.app.ui.components.ModernCard
import com.yourpax.app.ui.components.SmallActionBtn
import com.yourpax.app.ui.components.StatusMessageBanner
import com.yourpax.app.ui.theme.rememberAppColors
import kotlinx.coroutines.launch

private enum class SettingsAction {
    CLEAR_FILES, CLEAR_LIGHT, REBOOT, SHUTDOWN, RESTART, INIT_CSVS, STOP_ORCH, START_ORCH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenDrawer: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToConfig: () -> Unit = {}
) {
    val configRepo = remember { ConfigRepository() }
    val networkRepo = remember { NetworkRepository() }
    val wifiRepo = remember { WiFiRepository() }
    val scope = rememberCoroutineScope()
    var statusMsg by remember { mutableStateOf("") }
    var confirmAction by remember { mutableStateOf<SettingsAction?>(null) }
    var currentMode by remember { mutableStateOf("web_app") }
    var modeDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        configRepo.getModeConfig().onSuccess { cfg ->
            currentMode = cfg["connection_mode"] as? String ?: "web_app"
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DemoModeBanner()
        TopAppBar(
            title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (statusMsg.isNotEmpty()) {
                StatusMessageBanner(message = statusMsg)
            }

            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("System", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallActionBtn("Clear Files", Icons.Default.Delete, onClick = { confirmAction = SettingsAction.CLEAR_FILES })
                        SmallActionBtn("Clear Light", Icons.Default.CleaningServices, onClick = { confirmAction = SettingsAction.CLEAR_LIGHT })
                        SmallActionBtn("Init CSVs", Icons.Default.TableChart, onClick = { confirmAction = SettingsAction.INIT_CSVS })
                    }
                }
            }

            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Power", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallActionBtn("Reboot", Icons.Default.PowerSettingsNew, onClick = { confirmAction = SettingsAction.REBOOT })
                        SmallActionBtn("Shutdown", Icons.Default.PowerOff, onClick = { confirmAction = SettingsAction.SHUTDOWN })
                        SmallActionBtn("Restart", Icons.Default.Refresh, onClick = { confirmAction = SettingsAction.RESTART })
                    }
                }
            }

            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Orchestrator", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallActionBtn("Stop Orch.", Icons.Default.Block, onClick = { confirmAction = SettingsAction.STOP_ORCH })
                        SmallActionBtn("Start Orch.", Icons.Default.PlayArrow, onClick = { confirmAction = SettingsAction.START_ORCH })
                    }
                }
            }

            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Connectivity", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallActionBtn("Disc. WiFi", Icons.Default.Block, onClick = { scope.launch { wifiRepo.disconnectWifi(); statusMsg = "WiFi disconnected" } })
                    }
                }
            }

            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Mode Selector", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    ExposedDropdownMenuBox(expanded = modeDropdownExpanded, onExpandedChange = { modeDropdownExpanded = it }) {
                        OutlinedTextField(
                            value = currentMode.replace("_", " ").replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true
                        )
                        ExposedDropdownMenu(expanded = modeDropdownExpanded, onDismissRequest = { modeDropdownExpanded = false }) {
                            listOf("web_only", "app_only", "web_app").forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.replace("_", " ").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall) },
                                    onClick = {
                                        modeDropdownExpanded = false
                                        currentMode = mode
                                        scope.launch {
                                            configRepo.switchMode(mode)
                                                .onSuccess { statusMsg = "Mode switched to $mode" }
                                                .onFailure { statusMsg = "Mode switch failed: ${it.message}" }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Service Control", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallActionBtn("Start Web", Icons.Default.Language, onClick = { scope.launch { systemRepo.startWebService().onSuccess { statusMsg = "Web started" }.onFailure { statusMsg = "Web start failed: ${it.message}" } } })
                        SmallActionBtn("Stop Web", Icons.Default.Stop, onClick = { scope.launch { systemRepo.stopWebService().onSuccess { statusMsg = "Web stopped" }.onFailure { statusMsg = "Web stop failed: ${it.message}" } } })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallActionBtn("Start NAP", Icons.Default.Bluetooth, onClick = { scope.launch { systemRepo.startNapService().onSuccess { statusMsg = "NAP started" }.onFailure { statusMsg = "NAP start failed: ${it.message}" } } })
                        SmallActionBtn("Stop NAP", Icons.Default.Stop, onClick = { scope.launch { systemRepo.stopNapService().onSuccess { statusMsg = "NAP stopped" }.onFailure { statusMsg = "NAP stop failed: ${it.message}" } } })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallActionBtn("Start SPP", Icons.Default.Wifi, onClick = { scope.launch { systemRepo.startSppService().onSuccess { statusMsg = "SPP started" }.onFailure { statusMsg = "SPP start failed: ${it.message}" } } })
                        SmallActionBtn("Stop SPP", Icons.Default.Stop, onClick = { scope.launch { systemRepo.stopSppService().onSuccess { statusMsg = "SPP stopped" }.onFailure { statusMsg = "SPP stop failed: ${it.message}" } } })
                    }
                }
            }

            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Data", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Button(onClick = onNavigateToBackup, modifier = Modifier.fillMaxWidth().height(44.dp), shape = MaterialTheme.shapes.small) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Backup / Restore")
                    }
                    OutlinedButton(onClick = onNavigateToConfig, modifier = Modifier.fillMaxWidth().height(44.dp), shape = MaterialTheme.shapes.small) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Config Editor")
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    when (confirmAction) {
        SettingsAction.CLEAR_FILES -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Clear All Files?") },
            text = { Text("This will permanently delete ALL data files, configs, backups, and logs. This cannot be undone. Continue?") },
            confirmButton = { TextButton(onClick = { confirmAction = null; scope.launch { configRepo.clearFiles(); statusMsg = "Files cleared" } }) { Text("Delete All", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
        SettingsAction.CLEAR_LIGHT -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Clear Files (Light)?") },
            text = { Text("This will delete logs, stolen files, cracked passwords, scan results, and caches. Continue?") },
            confirmButton = { TextButton(onClick = { confirmAction = null; scope.launch { configRepo.clearFilesLight(); statusMsg = "Files light cleared" } }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
        SettingsAction.REBOOT -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Reboot your-pax?") },
            text = { Text("Reboot the Raspberry Pi? All running attacks will be stopped.") },
            confirmButton = { TextButton(onClick = { confirmAction = null; scope.launch { configRepo.reboot(); statusMsg = "Rebooting" } }) { Text("Reboot", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
        SettingsAction.SHUTDOWN -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Shut down your-pax?") },
            text = { Text("Shut down the Raspberry Pi completely? Power off.") },
            confirmButton = { TextButton(onClick = { confirmAction = null; scope.launch { configRepo.shutdown(); statusMsg = "Shutdown" } }) { Text("Shutdown", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
        SettingsAction.RESTART -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Restart your-pax Service?") },
            text = { Text("Restart the your-pax service? The web server and orchestrator will restart.") },
            confirmButton = { TextButton(onClick = { confirmAction = null; scope.launch { configRepo.restartService(); statusMsg = "Service restarted" } }) { Text("Restart", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
        SettingsAction.INIT_CSVS -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Re-initialize CSVs?") },
            text = { Text("Re-initialize CSV files? This will reset the network knowledge base and live status.") },
            confirmButton = { TextButton(onClick = { confirmAction = null; scope.launch { configRepo.initializeCsv(); statusMsg = "CSVs initialized" } }) { Text("Initialize", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
        SettingsAction.STOP_ORCH -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Stop Orchestrator?") },
            text = { Text("Stop the orchestrator? No automatic attacks will run until restarted.") },
            confirmButton = { TextButton(onClick = { confirmAction = null; scope.launch { networkRepo.stopOrchestrator(); statusMsg = "Orchestrator stopped" } }) { Text("Stop", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
        SettingsAction.START_ORCH -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Start Orchestrator?") },
            text = { Text("Start the orchestrator? Automatic attacks will resume.") },
            confirmButton = { TextButton(onClick = { confirmAction = null; scope.launch { networkRepo.startOrchestrator(); statusMsg = "Orchestrator started" } }) { Text("Start", color = MaterialTheme.colorScheme.primary) } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
        null -> {}
    }
}
