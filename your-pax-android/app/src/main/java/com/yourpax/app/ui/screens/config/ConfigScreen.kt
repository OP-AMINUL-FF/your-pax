package com.yourpax.app.ui.screens.config

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.yourpax.app.data.api.models.ConfigData
import com.yourpax.app.data.repository.ConfigRepository
import com.yourpax.app.data.repository.WiFiRepository
import com.yourpax.app.ui.components.*
import com.yourpax.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConfigScreen() {
    val configRepo = remember { ConfigRepository() }
    val scope = rememberCoroutineScope()
    var configMap by remember { mutableStateOf<Map<String, JsonElement>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var editValues by remember { mutableStateOf<MutableMap<String, String>>(mutableMapOf()) }
    var boolValues by remember { mutableStateOf<MutableMap<String, Boolean>>(mutableMapOf()) }
    var statusMsg by remember { mutableStateOf("") }
    var fontSize by remember { mutableFloatStateOf(13f) }
    var showWifiPanel by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    suspend fun loadConfigIntoMap(): Map<String, JsonElement>? {
        val cfg = configRepo.loadConfig().getOrNull() ?: return null
        val gson = Gson()
        val obj = gson.toJsonTree(cfg).asJsonObject
        val result = mutableMapOf<String, JsonElement>()
        obj.entrySet().forEach { (k, v) -> result[k] = v }
        return result
    }

    suspend fun initFromConfig() {
        val map = loadConfigIntoMap() ?: return
        configMap = map
        map.forEach { (key, value) ->
            when {
                value.isJsonPrimitive && value.asJsonPrimitive.isBoolean -> boolValues[key] = value.asBoolean
                value.isJsonPrimitive && value.asJsonPrimitive.isNumber -> editValues[key] = value.asNumber.toString()
                value.isJsonPrimitive -> editValues[key] = value.asString
                value.isJsonArray -> editValues[key] = value.asJsonArray.joinToString(",") { e ->
                    if (e.isJsonPrimitive) e.asString else e.toString()
                }
                else -> editValues[key] = value.toString()
            }
        }
    }

    LaunchedEffect(Unit) {
        initFromConfig()
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        DemoModeBanner()
        TopAppBar(
            title = { Text("Configuration", fontWeight = FontWeight.SemiBold) },
            actions = {
                IconButton(onClick = { fontSize = maxOf(8f, fontSize - 1f) }) { Icon(Icons.Default.TextDecrease, contentDescription = "Font -") }
                IconButton(onClick = {
                    scope.launch {
                        saveConfigMap(configRepo, editValues, boolValues, configMap ?: return@launch)
                        statusMsg = "Configuration saved"
                    }
                }) { Icon(Icons.Default.Save, contentDescription = "Save", tint = Success) }
                IconButton(onClick = { showRestoreConfirm = true }) { Icon(Icons.Default.Restore, contentDescription = "Restore Default") }
                IconButton(onClick = { showWifiPanel = !showWifiPanel }) {
                    Icon(Icons.Default.Wifi, contentDescription = "WiFi Scan", tint = if (showWifiPanel) Primary else MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { fontSize = minOf(24f, fontSize + 1f) }) { Icon(Icons.Default.TextIncrease, contentDescription = "Font +") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        if (statusMsg.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Info.copy(alpha = 0.1f))) {
                Text(statusMsg, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, color = Info)
            }
        }

        if (isLoading) {
            LoadingOverlay()
        } else if (configMap == null) {
            EmptyState(title = "Could not load config")
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    configMap!!.forEach { (key, value) ->
                    if (key.startsWith("__title_")) {
                        Text(value.asString, style = MaterialTheme.typography.titleMedium.copy(fontSize = fontSize.sp), fontWeight = FontWeight.Bold, color = Primary)
                    } else if (key in listOf("evil_ap_running", "scan_vuln_running")) {
                        // skip runtime-only fields
                    } else when {
                        value.isJsonPrimitive && value.asJsonPrimitive.isBoolean -> {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(key, style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize.sp))
                                Switch(checked = boolValues[key] ?: false, onCheckedChange = { boolValues[key] = it })
                            }
                        }
                        value.isJsonPrimitive && value.asJsonPrimitive.isNumber -> {
                            OutlinedTextField(
                                value = editValues[key] ?: "0",
                                onValueChange = { editValues[key] = it },
                                label = { Text(key) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = fontSize.sp)
                            )
                        }
                        value.isJsonArray -> {
                            OutlinedTextField(
                                value = editValues[key] ?: "",
                                onValueChange = { editValues[key] = it },
                                label = { Text(key) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text("comma-separated") },
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = fontSize.sp)
                            )
                        }
                        else -> {
                            OutlinedTextField(
                                value = editValues[key] ?: "",
                                onValueChange = { editValues[key] = it },
                                label = { Text(key) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = fontSize.sp)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            saveConfigMap(configRepo, editValues, boolValues, configMap ?: return@launch)
                            statusMsg = "Configuration saved"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save Configuration") }

                Spacer(Modifier.height(80.dp))
            }

            if (showWifiPanel) {
                WiFiScanPanel(onClose = { showWifiPanel = false }, onStatus = { statusMsg = it })
            }
        }
    }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore Default Config?") },
            text = { Text("Restore all configuration settings to factory defaults? This will overwrite all custom settings.") },
            confirmButton = { TextButton(onClick = { showRestoreConfirm = false; scope.launch { configRepo.restoreDefaultConfig().getOrNull(); initFromConfig(); statusMsg = "Default config restored" } }) { Text("Restore", color = Error) } },
            dismissButton = { TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun WiFiScanPanel(onClose: () -> Unit, onStatus: (String) -> Unit) {
    val repo = remember { WiFiRepository() }
    var wifiList by remember { mutableStateOf<List<String>>(emptyList()) }
    var curSsid by remember { mutableStateOf("") }
    var scanned by remember { mutableStateOf(false) }
    var showPwdDialog by remember { mutableStateOf(false) }
    var selectedSsid by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        val networks = repo.scanNetworks().getOrNull()
        val status = repo.getWifiStatus().getOrNull()
        if (networks != null) wifiList = networks.map { it.ssid.ifEmpty { "<Hidden: ${it.bssid.take(8)}>" } }.distinct()
        if (status != null) curSsid = status.ssid
        scanned = true
    }

    LaunchedEffect(Unit) { refresh() }

    LaunchedEffect(Unit) {
        while (true) { delay(5000); refresh() }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Available Wi-Fi Networks", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("5s", style = MaterialTheme.typography.labelSmall, color = SubtleText)
                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp)) }
                }
            }
            if (!scanned) {
                Text("Scanning...", style = MaterialTheme.typography.bodySmall, color = SubtleText)
            } else if (wifiList.isEmpty()) {
                Text("No networks found", style = MaterialTheme.typography.bodySmall, color = SubtleText)
            } else {
                wifiList.forEachIndexed { i, ssid ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selectedSsid = ssid; passwordInput = ""; showPwdDialog = true }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp), tint = Primary)
                        Text(ssid, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        if (ssid == curSsid) {
                            Text("Connected", style = MaterialTheme.typography.labelSmall, color = Success)
                        }
                    }
                    if (i < wifiList.lastIndex) HorizontalDivider()
                }
            }
        }
    }

    if (showPwdDialog) {
        AlertDialog(
            onDismissRequest = { showPwdDialog = false },
            title = { Text("Connect to $selectedSsid") },
            text = {
                Column {
                    Text("Enter password", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = passwordInput, onValueChange = { passwordInput = it }, label = { Text("Password") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showPwdDialog = false
                    scope.launch {
                        repo.connectWifi(selectedSsid, passwordInput).onSuccess { onStatus("Connected to $selectedSsid") }.onFailure { onStatus("Failed: ${it.message}") }
                    }
                }) { Text("Connect") }
            },
            dismissButton = { TextButton(onClick = { showPwdDialog = false }) { Text("Cancel") } }
        )
    }
}

private suspend fun saveConfigMap(configRepo: ConfigRepository, editValues: MutableMap<String, String>, boolValues: MutableMap<String, Boolean>, configMap: Map<String, JsonElement>) {
    val saveMap = mutableMapOf<String, Any?>()
    configMap.forEach { (key, value) ->
        if (key.startsWith("__title_") || key in listOf("evil_ap_running", "scan_vuln_running")) return@forEach
        when {
            value.isJsonPrimitive && value.asJsonPrimitive.isBoolean -> saveMap[key] = boolValues[key] ?: false
            value.isJsonPrimitive && value.asJsonPrimitive.isNumber -> {
                val s = editValues[key] ?: "0"
                saveMap[key] = if (s.contains(".")) s.toFloatOrNull() ?: 0f else s.toIntOrNull() ?: 0
            }
            value.isJsonArray -> {
                val s = editValues[key] ?: ""
                saveMap[key] = if (s.isBlank()) emptyList<Any>() else s.split(",").map { it.trim() }.map { it.toIntOrNull() ?: it }
            }
            else -> saveMap[key] = editValues[key] ?: ""
        }
    }
    @Suppress("UNCHECKED_CAST")
    configRepo.saveConfig(saveMap as Map<String, Any>)
}
