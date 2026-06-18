package com.yourpax.app.ui.screens.wificonnect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.yourpax.app.data.api.models.BluetoothStatus
import com.yourpax.app.data.api.models.WiFiNetwork
import com.yourpax.app.data.api.models.WiFiStatusResponse
import com.yourpax.app.data.repository.BluetoothRepository
import com.yourpax.app.data.repository.WiFiRepository
import com.yourpax.app.ui.components.*
import com.yourpax.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WiFiConnectScreen() {
    val wifiRepo = remember { WiFiRepository() }
    val btRepo = remember { BluetoothRepository() }
    val scope = rememberCoroutineScope()
    var wifiStatus by remember { mutableStateOf<WiFiStatusResponse?>(null) }
    var btStatus by remember { mutableStateOf<BluetoothStatus?>(null) }
    var networks by remember { mutableStateOf<List<WiFiNetwork>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf("") }
    var showConnectDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        wifiStatus = wifiRepo.getWifiStatus().getOrNull()
        btStatus = btRepo.getBluetoothStatus().getOrNull()
        isLoading = false
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            wifiStatus = wifiRepo.getWifiStatus().getOrNull()
            btStatus = btRepo.getBluetoothStatus().getOrNull()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DemoModeBanner()
        TopAppBar(
            title = { Text("BT Connect", fontWeight = FontWeight.SemiBold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        if (isLoading) {
            LoadingOverlay()
        } else {
            LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // WiFi Status card
            item {
                ModernCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("WiFi Status", fontWeight = FontWeight.SemiBold)
                        wifiStatus?.let { s ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Status:", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                                Text(
                                    if (s.connected) "Connected" else "Disconnected",
                                    color = if (s.connected) Success else Error,
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text("Current SSID: ${s.ssid.ifEmpty { "-" }}", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                            Text("Signal: ${s.signal.ifEmpty { "-" }}", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                        } ?: Text("Checking...", style = MaterialTheme.typography.bodySmall, color = SubtleText)

                        if (wifiStatus?.connected == true) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val r = wifiRepo.disconnectWifi()
                                        statusMsg = if (r.isSuccess) "Disconnected" else (r.exceptionOrNull()?.message ?: "Failed")
                                        wifiStatus = wifiRepo.getWifiStatus().getOrNull()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Disconnect") }
                        }
                    }
                }
            }

            // Bluetooth NAP Status card
            item {
                ModernCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Bluetooth NAP", fontWeight = FontWeight.SemiBold)
                        btStatus?.let { bt ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("NAP Status:", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                                Text(
                                    if (bt.active) "Active" else "Inactive",
                                    color = if (bt.active) Success else Error,
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text("Bridge IP: ${bt.bridgeIp.ifEmpty { "-" }}", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                            Text("Connected Clients: ${bt.connectedClients}", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                            if (bt.bridgeIp.isNotEmpty()) {
                                Text(
                                    "Accessible at: http://${bt.bridgeIp}:${bt.port}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Primary
                                )
                            }
                        } ?: Text("Checking...", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                    }
                }
            }

            // Status message
            if (statusMsg.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Info.copy(alpha = 0.1f))
                    ) {
                        Text(statusMsg, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Scan Networks button + manual connect
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                scanning = true
                                val result = wifiRepo.scanNetworks()
                                if (result.isSuccess) {
                                    networks = result.getOrDefault(emptyList())
                                } else {
                                    statusMsg = result.exceptionOrNull()?.message ?: "Scan failed"
                                }
                                scanning = false
                            }
                        },
                        enabled = !scanning,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (scanning) "Scanning..." else "Scan Networks")
                    }
                    OutlinedButton(
                        onClick = { showConnectDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Manual Connect")
                    }
                }
            }

            // Manual connect dialog
            if (showConnectDialog) {
                item {
                    ModernCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Connect to Network", fontWeight = FontWeight.SemiBold)
                            var manualSsid by remember { mutableStateOf("") }
                            var manualPassword by remember { mutableStateOf("") }
                            var showPwd by remember { mutableStateOf(false) }
                            OutlinedTextField(
                                value = manualSsid,
                                onValueChange = { manualSsid = it },
                                label = { Text("SSID") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null) }
                            )
                            OutlinedTextField(
                                value = manualPassword,
                                onValueChange = { manualPassword = it },
                                label = { Text("Password") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPwd = !showPwd }) {
                                        Icon(if (showPwd) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { showConnectDialog = false }, shape = RoundedCornerShape(12.dp)) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            if (manualSsid.isBlank()) { statusMsg = "Enter an SSID"; return@launch }
                                            val r = wifiRepo.connectWifi(manualSsid, manualPassword)
                                            statusMsg = if (r.isSuccess) "Connecting to $manualSsid..." else (r.exceptionOrNull()?.message ?: "Failed")
                                            wifiStatus = wifiRepo.getWifiStatus().getOrNull()
                                            showConnectDialog = false
                                        }
                                    },
                                    enabled = manualSsid.isNotBlank(),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Connect") }
                            }
                        }
                    }
                }
            }

            // Available networks list
            if (networks.isNotEmpty()) {
                item {
                    Text(
                        "Available Networks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(networks) { net ->
                    val ssid = net.ssid.ifEmpty { "(hidden)" }
                    val enc = if (net.wpa.isNotEmpty()) net.wpa else "Open"
                    val sig = net.signal.ifEmpty { "?" }
                    var showPwdDialog by remember { mutableStateOf(false) }
                    var pwd by remember { mutableStateOf("") }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPwdDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Wifi,
                                contentDescription = null,
                                tint = when {
                                    sig.toIntOrNull()?.let { it > -60 } == true -> Success
                                    sig.toIntOrNull()?.let { it > -75 } == true -> Warning
                                    else -> Error
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ssid, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("[$enc]", style = MaterialTheme.typography.bodySmall, color = Primary)
                                    Text("Signal: ${sig}dBm", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SubtleText)
                        }
                    }

                    if (showPwdDialog) {
                        AlertDialog(
                            onDismissRequest = { showPwdDialog = false },
                            title = { Text("Connect to \"$ssid\"") },
                            text = {
                                OutlinedTextField(
                                    value = pwd,
                                    onValueChange = { pwd = it },
                                    label = { Text("Password (leave empty for open)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            confirmButton = {
                                Button(onClick = {
                                    scope.launch {
                                        val r = wifiRepo.connectWifi(net.ssid, pwd)
                                        statusMsg = if (r.isSuccess) "Connecting to $ssid..." else (r.exceptionOrNull()?.message ?: "Failed")
                                        wifiStatus = wifiRepo.getWifiStatus().getOrNull()
                                        showPwdDialog = false
                                    }
                                }, shape = RoundedCornerShape(12.dp)) { Text("Connect") }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = { showPwdDialog = false }, shape = RoundedCornerShape(12.dp)) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    }
}
