package com.yourpax.app.ui.screens.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourpax.app.data.repository.SystemRepository
import com.yourpax.app.ui.components.DemoModeBanner
import com.yourpax.app.ui.components.LoadingOverlay
import com.yourpax.app.ui.components.ModernCard
import com.yourpax.app.ui.components.StatusMessageBanner
import com.yourpax.app.ui.theme.rememberAppColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    onOpenDrawer: () -> Unit = {}
) {
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

    val appColors = rememberAppColors()

    Column(modifier = Modifier.fillMaxSize()) {
        DemoModeBanner()
        TopAppBar(
            title = { Text("Backup & Restore", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        if (statusMsg.isNotEmpty()) {
            StatusMessageBanner(message = statusMsg, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Create Backup", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Text("Creates a ZIP archive of all data, configs, logs, captures, and credentials.", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
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
                        shape = MaterialTheme.shapes.small,
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
                        Surface(shape = MaterialTheme.shapes.extraSmall, color = appColors.successContainer, tonalElevation = 0.dp) {
                            Text(backupResult!!, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, color = appColors.terminalText)
                        }
                    }
                }
            }

            ModernCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Restore from Backup", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Text("Restores all data from a previously created backup ZIP file. This will overwrite current data.", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                    Button(
                        onClick = { filePickerLauncher.launch("application/zip") },
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = appColors.warning),
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
                }) { Text("Restore", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showRestoreConfirm = false; pendingRestoreBytes = null }) { Text("Cancel") } }
        )
    }

    if (isLoading) { LoadingOverlay(message = "Processing...") }
}
