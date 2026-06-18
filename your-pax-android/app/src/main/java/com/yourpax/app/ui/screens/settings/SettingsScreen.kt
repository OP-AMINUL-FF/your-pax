package com.yourpax.app.ui.screens.settings

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourpax.app.data.repository.ConfigRepository
import com.yourpax.app.data.repository.NetworkRepository
import com.yourpax.app.data.repository.SystemRepository
import com.yourpax.app.data.repository.WiFiRepository
import com.yourpax.app.ui.components.*
import com.yourpax.app.ui.theme.*
import kotlinx.coroutines.launch

private enum class SettingsAction {
    CLEAR_FILES, CLEAR_LIGHT, REBOOT, SHUTDOWN, RESTART, INIT_CSVS, STOP_ORCH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToBackup: () -> Unit = {},
    onNavigateToConfig: () -> Unit = {}
) {
    val configRepo = remember { ConfigRepository() }
    val networkRepo = remember { NetworkRepository() }
    val wifiRepo = remember { WiFiRepository() }
    val scope = rememberCoroutineScope()
    var statusMsg by remember { mutableStateOf("") }
    var confirmAction by remember { mutableStateOf<SettingsAction?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        DemoModeBanner()
        TopAppBar(
            title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                    Text("System", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallActionBtn("Clear Files", Icons.Default.Delete) { confirmAction = SettingsAction.CLEAR_FILES }
                        SmallActionBtn("Clear Light", Icons.Default.CleaningServices) { confirmAction = SettingsAction.CLEAR_LIGHT }
                        SmallActionBtn("Init CSVs", Icons.Default.TableChart) { confirmAction = SettingsAction.INIT_CSVS }
                    }
                }
            }

            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Power", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallActionBtn("Reboot", Icons.Default.PowerSettingsNew) { confirmAction = SettingsAction.REBOOT }
                        SmallActionBtn("Shutdown", Icons.Default.PowerOff) { confirmAction = SettingsAction.SHUTDOWN }
                        SmallActionBtn("Restart", Icons.Default.Refresh) { confirmAction = SettingsAction.RESTART }
                    }
                }
            }

            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Orchestrator", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallActionBtn("Stop Orch.", Icons.Default.Block) { confirmAction = SettingsAction.STOP_ORCH }
                        SmallActionBtn("Start Orch.", Icons.Default.PlayArrow) { scope.launch { networkRepo.startOrchestrator(); statusMsg = "Orchestrator started" } }
                    }
                }
            }

            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Connectivity", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallActionBtn("Disc. WiFi", Icons.Default.Block) { scope.launch { wifiRepo.disconnectWifi(); statusMsg = "WiFi disconnected" } }
                    }
                }
            }

            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Data", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Button(onClick = onNavigateToBackup, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Backup / Restore")
                    }
                    OutlinedButton(onClick = onNavigateToConfig, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
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
            confirmButton = { TextButton(onClick = { confirmAction = null; scope.launch { configRepo.clearFiles(); statusMsg = "Files cleared" } }) { Text("Delete All", color = Error) } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
        SettingsAction.CLEAR_LIGHT -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Clear Files (Light)?") },
            text = { Text("This will delete logs, stolen files, cracked passwords, scan results, and caches. Continue?") },
            confirmButton = { TextButton(onClick = { confirmAction = null; scope.launch { configRepo.clearFilesLight(); statusMsg = "Files light cleared" } }) { Text("Delete", color = Error) } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
        SettingsAction.REBOOT -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Reboot your-pax?") },
            text = { Text("Reboot the Raspberry Pi? All running attacks will be stopped.") },
            confirmButton = { TextButton(onClick = { confirmAction = null; scope.launch { configRepo.reboot(); statusMsg = "Rebooting" } }) { Text("Reboot", color = Error) } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
        SettingsAction.SHUTDOWN -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Shut down your-pax?") },
            text = { Text("Shut down the Raspberry Pi completely? Power off.") },
            confirmButton = { TextButton(onClick = { confirmAction = null; scope.launch { configRepo.shutdown(); statusMsg = "Shutdown" } }) { Text("Shutdown", color = Error) } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
        SettingsAction.RESTART -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Restart your-pax Service?") },
            text = { Text("Restart the your-pax service? The web server and orchestrator will restart.") },
            confirmButton = { TextButton(onClick = { confirmAction = null; scope.launch { configRepo.restartService(); statusMsg = "Service restarted" } }) { Text("Restart", color = Error) } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
        SettingsAction.INIT_CSVS -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Re-initialize CSVs?") },
            text = { Text("Re-initialize CSV files? This will reset the network knowledge base and live status.") },
            confirmButton = { TextButton(onClick = { confirmAction = null; scope.launch { configRepo.initializeCsv(); statusMsg = "CSVs initialized" } }) { Text("Initialize", color = Error) } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
        SettingsAction.STOP_ORCH -> AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text("Stop Orchestrator?") },
            text = { Text("Stop the orchestrator? No automatic attacks will run until restarted.") },
            confirmButton = { TextButton(onClick = { confirmAction = null; scope.launch { networkRepo.stopOrchestrator(); statusMsg = "Orchestrator stopped" } }) { Text("Stop", color = Error) } },
            dismissButton = { TextButton(onClick = { confirmAction = null }) { Text("Cancel") } }
        )
        null -> {}
    }
}

@Composable
private fun SmallActionBtn(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), shape = RoundedCornerShape(6.dp), modifier = Modifier.height(36.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.85f)
    }
}
