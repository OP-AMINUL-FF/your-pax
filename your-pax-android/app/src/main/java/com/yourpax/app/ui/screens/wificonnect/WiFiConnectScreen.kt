package com.yourpax.app.ui.screens.wificonnect

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yourpax.app.data.api.models.BluetoothStatus
import com.yourpax.app.data.api.models.WiFiNetwork
import com.yourpax.app.data.api.models.WiFiStatusResponse
import com.yourpax.app.data.repository.BluetoothRepository
import com.yourpax.app.data.repository.WiFiRepository
import com.yourpax.app.ui.components.DemoModeBanner
import com.yourpax.app.ui.components.LoadingOverlay
import com.yourpax.app.ui.components.ModernCard
import com.yourpax.app.ui.components.StatusMessageBanner
import com.yourpax.app.ui.theme.rememberAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WiFiConnectScreen(onOpenDrawer: () -> Unit = {}) {
    val wifiRepo = remember { WiFiRepository() }
    val btRepo = remember { BluetoothRepository() }
    val scope = rememberCoroutineScope()
    var wifiStatus by remember { mutableStateOf<WiFiStatusResponse?>(null) }
    var btStatus by remember { mutableStateOf<BluetoothStatus?>(null) }
    var networks by remember { mutableStateOf<List<WiFiNetwork>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf("") }
    var showConnectDialog by remember { mutableStateOf(false) }
    var showHiddenDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val appColors = rememberAppColors()

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
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "Open drawer")
                }
            }
        )

        if (isLoading) {
            LoadingOverlay()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ModernCard {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("WiFi Status", fontWeight = FontWeight.SemiBold)
                            wifiStatus?.let { s ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Status:", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                    Text(
                                        if (s.connected) "Connected" else "Disconnected",
                                        color = if (s.connected) appColors.success else MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text("Current SSID: ${s.ssid.ifEmpty { "-" }}", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                Text("Signal: ${s.signal.ifEmpty { "-" }}", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                            } ?: Text("Checking...", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)

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
                                    shape = MaterialTheme.shapes.medium,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Disconnect") }
                            }
                        }
                    }
                }

                item {
                    ModernCard {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Bluetooth NAP", fontWeight = FontWeight.SemiBold)
                            btStatus?.let { bt ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("NAP Status:", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                    Text(
                                        if (bt.active) "Active" else "Inactive",
                                        color = if (bt.active) appColors.success else MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text("Bridge IP: ${bt.bridgeIp.ifEmpty { "-" }}", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                Text("Connected Clients: ${bt.connectedClients}", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                if (bt.bridgeIp.isNotEmpty()) {
                                    Text(
                                        "Accessible at: http://${bt.bridgeIp}:${bt.port}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } ?: Text("Checking...", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                        }
                    }
                }

                if (statusMsg.isNotEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            color = appColors.info.copy(alpha = 0.1f),
                            tonalElevation = 0.dp
                        ) {
                            Text(statusMsg, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

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
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (scanning) "Scanning..." else "Scan Networks")
                        }
                        OutlinedButton(
                            onClick = { showConnectDialog = true },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Manual Connect")
                        }
                    }
                    OutlinedButton(
                        onClick = { showHiddenDialog = true },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Hidden Network")
                    }
                }

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
                                    OutlinedButton(onClick = { showConnectDialog = false }, shape = MaterialTheme.shapes.medium) {
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
                                        shape = MaterialTheme.shapes.medium
                                    ) { Text("Connect") }
                                }
                            }
                        }
                    }
                }

                if (networks.isNotEmpty()) {
                    item {
                        Text("Available Networks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    items(networks) { net ->
                        val ssid = net.ssid.ifEmpty { "(hidden)" }
                        val enc = if (net.wpa == true) "WPA" else "Open"
                        val sig = net.signal.ifEmpty { "?" }
                        var showPwdDialog by remember { mutableStateOf(false) }
                        var pwd by remember { mutableStateOf("") }

                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { showPwdDialog = true },
                            shape = MaterialTheme.shapes.medium,
                            color = appColors.surfaceContainerLow,
                            tonalElevation = 0.dp
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
                                        sig.toIntOrNull()?.let { it > -60 } == true -> appColors.success
                                        sig.toIntOrNull()?.let { it > -75 } == true -> appColors.warning
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ssid, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("[$enc]", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                        Text("Signal: ${sig}dBm", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = appColors.subtleText)
                            }
                        }

                        if (showPwdDialog) {
                            AlertDialog(
                                onDismissRequest = { showPwdDialog = false },
                                title = { Text("Connect to \"$ssid\"") },
                                text = {
                                    OutlinedTextField(value = pwd, onValueChange = { pwd = it }, label = { Text("Password (leave empty for open)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                },
                                confirmButton = {
                                    Button(onClick = {
                                        scope.launch {
                                            val r = wifiRepo.connectWifi(net.ssid, pwd)
                                            statusMsg = if (r.isSuccess) "Connecting to $ssid..." else (r.exceptionOrNull()?.message ?: "Failed")
                                            wifiStatus = wifiRepo.getWifiStatus().getOrNull()
                                            showPwdDialog = false
                                        }
                                    }, shape = MaterialTheme.shapes.medium) { Text("Connect") }
                                },
                                dismissButton = {
                                    OutlinedButton(onClick = { showPwdDialog = false }, shape = MaterialTheme.shapes.medium) {
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

    if (showHiddenDialog) {
        var hiddenSsid by remember { mutableStateOf("") }
        var hiddenPassword by remember { mutableStateOf("") }
        var showHiddenPwd by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showHiddenDialog = false },
            title = { Text("Add Hidden Network") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Connect to a network that is not broadcasting its SSID.",
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.subtleText
                    )
                    OutlinedTextField(
                        value = hiddenSsid,
                        onValueChange = { hiddenSsid = it },
                        label = { Text("SSID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null) }
                    )
                    OutlinedTextField(
                        value = hiddenPassword,
                        onValueChange = { hiddenPassword = it },
                        label = { Text("Password (leave empty for open)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showHiddenPwd) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showHiddenPwd = !showHiddenPwd }) {
                                Icon(
                                    if (showHiddenPwd) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            if (hiddenSsid.isBlank()) {
                                statusMsg = "Enter an SSID"
                                return@launch
                            }
                            val r = wifiRepo.connectWifi(hiddenSsid, hiddenPassword, hidden = true)
                            statusMsg = if (r.isSuccess) "Connecting to hidden network $hiddenSsid..." else (r.exceptionOrNull()?.message ?: "Failed")
                            wifiStatus = wifiRepo.getWifiStatus().getOrNull()
                            showHiddenDialog = false
                        }
                    },
                    enabled = hiddenSsid.isNotBlank(),
                    shape = MaterialTheme.shapes.medium
                ) { Text("Connect") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showHiddenDialog = false }, shape = MaterialTheme.shapes.medium) {
                    Text("Cancel")
                }
            }
        )
    }
}
