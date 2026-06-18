package com.yourpax.app.ui.screens.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourpax.app.data.repository.SystemRepository
import com.yourpax.app.ui.components.DemoModeBanner
import com.yourpax.app.ui.components.LoadingOverlay
import com.yourpax.app.ui.components.ModernCard
import com.yourpax.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen() {
    val systemRepo = remember { SystemRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var statusMsg by remember { mutableStateOf("") }
    var backupResult by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var pendingRestoreBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingRestoreName by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes() ?: return@launch
                    val fileName = uri.lastPathSegment ?: "backup.zip"
                    pendingRestoreBytes = bytes
                    pendingRestoreName = fileName
                    showRestoreConfirm = true
                } catch (e: Exception) {
                    statusMsg = "Error reading file: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DemoModeBanner()
        TopAppBar(
            title = { Text("Backup & Restore", fontWeight = FontWeight.SemiBold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        if (statusMsg.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Info.copy(alpha = 0.1f))) {
                Text(statusMsg, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, color = Info)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Create Backup", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Text("Creates a ZIP archive of all data, configs, logs, captures, and credentials.", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                statusMsg = ""
                                backupResult = null
                                systemRepo.backup().onSuccess { resp ->
                                    backupResult = "Backup created: ${resp.filename}\nDownload URL: ${resp.url}"
                                    statusMsg = "Backup created successfully"
                                }.onFailure {
                                    statusMsg = "Backup failed: ${it.message}"
                                }
                                isLoading = false
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                        }
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Create Backup")
                    }
                    if (backupResult != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF002200)), shape = RoundedCornerShape(4.dp)) {
                            Text(backupResult!!, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, color = Color(0xFF88FF88))
                        }
                    }
                }
            }

            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Restore from Backup", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Text("Restores all data from a previously created backup ZIP file. This will overwrite current data.", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                    Button(
                        onClick = { filePickerLauncher.launch("application/zip") },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Warning),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.RestorePage, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Select Backup File")
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false; pendingRestoreBytes = null },
            title = { Text("Restore from Backup?") },
            text = { Text("This will overwrite all current data with the backup from $pendingRestoreName. This cannot be undone. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    scope.launch {
                        isLoading = true
                        pendingRestoreBytes?.let { bytes ->
                            systemRepo.restore(bytes, pendingRestoreName).onSuccess {
                                statusMsg = "Backup restored successfully"
                            }.onFailure {
                                statusMsg = "Restore failed: ${it.message}"
                            }
                        }
                        pendingRestoreBytes = null
                        isLoading = false
                    }
                }) { Text("Restore", color = Error) }
            },
            dismissButton = { TextButton(onClick = { showRestoreConfirm = false; pendingRestoreBytes = null }) { Text("Cancel") } }
        )
    }

    if (isLoading) { LoadingOverlay(message = "Processing...") }
}
