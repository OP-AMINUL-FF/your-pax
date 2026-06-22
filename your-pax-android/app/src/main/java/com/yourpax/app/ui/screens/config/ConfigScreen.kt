package com.yourpax.app.ui.screens.config

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.yourpax.app.R
import com.yourpax.app.data.api.models.ConfigData
import com.yourpax.app.data.repository.ConfigRepository
import com.yourpax.app.data.repository.WiFiRepository
import com.yourpax.app.ui.components.DemoModeBanner
import com.yourpax.app.ui.components.EmptyState
import com.yourpax.app.ui.components.FontSizeControl
import com.yourpax.app.ui.components.LoadingOverlay
import com.yourpax.app.ui.components.StatusMessageBanner
import com.yourpax.app.ui.components.LottieAnim
import com.yourpax.app.ui.components.CopyButton
import com.yourpax.app.ui.theme.rememberAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val configLabels: Map<String, String> = mapOf(
    "manual_mode" to "Manual Mode",
    "websrv" to "Web Server",
    "web_increment" to "Web Auto-Increment",
    "debug_mode" to "Debug Mode",
    "retry_success_actions" to "Retry Success Actions",
    "retry_failed_actions" to "Retry Failed Actions",
    "blacklistcheck" to "Blacklist Check",
    "displaying_csv" to "Displaying CSV",
    "log_debug" to "Log Debug",
    "log_info" to "Log Info",
    "log_warning" to "Log Warning",
    "log_error" to "Log Error",
    "log_critical" to "Log Critical",
    "startup_delay" to "Startup Delay (s)",
    "web_delay" to "Web Delay (s)",
    "screen_delay" to "Screen Delay (s)",
    "comment_delaymin" to "Comment Delay Min (s)",
    "comment_delaymax" to "Comment Delay Max (s)",
    "livestatus_delay" to "Live Status Delay (s)",
    "image_display_delaymin" to "Image Display Delay Min (s)",
    "image_display_delaymax" to "Image Display Delay Max (s)",
    "scan_interval" to "Scan Interval (s)",
    "scan_vuln_interval" to "Vuln Scan Interval (s)",
    "failed_retry_delay" to "Failed Retry Delay (s)",
    "success_retry_delay" to "Success Retry Delay (s)",
    "has_display" to "Has Display",
    "ref_width" to "Reference Width",
    "ref_height" to "Reference Height",
    "epd_type" to "EPD Type",
    "wifi_interface" to "WiFi Interface",
    "monitor_interface" to "Monitor Interface",
    "evil_ap_ssid" to "Evil AP SSID",
    "evil_ap_channel" to "Evil AP Channel",
    "evil_ap_interface" to "Evil AP Interface",
    "evil_ap_fakeauth_interval" to "PMKID Fakeauth Interval (s)",
    "portlist" to "Port List",
    "ip_scan_blacklist" to "IP Blacklist",
    "mac_scan_blacklist" to "MAC Blacklist",
    "steal_file_names" to "Steal File Names",
    "steal_file_extensions" to "Steal File Extensions",
    "bluetooth_nap_ssid" to "Bluetooth NAP SSID",
    "bluetooth_nap_bridge_ip" to "Bluetooth NAP Bridge IP",
    "nmap_scan_aggressivity" to "Nmap Aggressivity",
    "portstart" to "Port Start",
    "portend" to "Port End",
    "timewait_smb" to "SMB Time Wait (s)",
    "timewait_ssh" to "SSH Time Wait (s)",
    "timewait_telnet" to "Telnet Time Wait (s)",
    "timewait_ftp" to "FTP Time Wait (s)",
    "timewait_sql" to "SQL Time Wait (s)",
    "timewait_rdp" to "RDP Time Wait (s)",
    "enable_monitor_mode" to "Enable Monitor Mode",
)

private fun friendlyLabel(key: String): String = configLabels[key] ?: key

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    onOpenDrawer: () -> Unit = {}
) {
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
    val appColors = rememberAppColors()

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
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            },
            actions = {
                FontSizeControl(currentSize = fontSize, onDecrease = { fontSize = maxOf(8f, fontSize - 1f) }, onIncrease = { fontSize = minOf(24f, fontSize + 1f) }, minSize = 8f)
                IconButton(onClick = {
                    scope.launch {
                        saveConfigMap(configRepo, editValues, boolValues, configMap ?: return@launch)
                        statusMsg = "Configuration saved"
                    }
                }) { Icon(Icons.Default.Save, contentDescription = "Save", tint = appColors.success) }
                IconButton(onClick = { showRestoreConfirm = true }) { Icon(Icons.Default.Restore, contentDescription = "Restore Default") }
                IconButton(onClick = { showWifiPanel = !showWifiPanel }) {
                    Icon(Icons.Default.Wifi, contentDescription = "WiFi Scan", tint = if (showWifiPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        if (statusMsg.isNotEmpty()) {
            StatusMessageBanner(message = statusMsg, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }

        if (isLoading) {
            LoadingOverlay()
        } else if (configMap == null) {
            EmptyState(title = "Could not load config")
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("System Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            val configJsonString = Gson().toJson(editValues + boolValues)
                            CopyButton(textToCopy = configJsonString, size = 20.dp)
                        }
                        LottieAnim(
                            rawResId = R.raw.settings_gear,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    configMap!!.forEach { (key, value) ->
                        if (key.startsWith("__title_")) {
                            Text(value.asString, style = MaterialTheme.typography.titleMedium.copy(fontSize = fontSize.sp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        } else if (key in listOf("evil_ap_running", "scan_vuln_running")) {
                            // skip runtime-only fields
                        } else when {
                            value.isJsonPrimitive && value.asJsonPrimitive.isBoolean -> {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(friendlyLabel(key), style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSize.sp))
                                    Switch(checked = boolValues[key] ?: false, onCheckedChange = { boolValues[key] = it })
                                }
                            }
                            value.isJsonPrimitive && value.asJsonPrimitive.isNumber -> {
                                OutlinedTextField(
                                    value = editValues[key] ?: "0",
                                    onValueChange = { editValues[key] = it },
                                    label = { Text(friendlyLabel(key)) },
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
                                    label = { Text(friendlyLabel(key)) },
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
                                    label = { Text(friendlyLabel(key)) },
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
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = MaterialTheme.shapes.small
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
            confirmButton = { TextButton(onClick = { showRestoreConfirm = false; scope.launch { configRepo.restoreDefaultConfig().getOrNull(); initFromConfig(); statusMsg = "Default config restored" } }) { Text("Restore", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun WiFiScanPanel(onClose: () -> Unit, onStatus: (String) -> Unit) {
    val appColors = rememberAppColors()
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

    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = MaterialTheme.shapes.medium,
        color = appColors.surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Available Wi-Fi Networks", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("5s", style = MaterialTheme.typography.labelSmall, color = appColors.subtleText)
                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp)) }
                }
            }
            if (!scanned) {
                Text("Scanning...", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
            } else if (wifiList.isEmpty()) {
                Text("No networks found", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
            } else {
                wifiList.forEachIndexed { i, ssid ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selectedSsid = ssid; passwordInput = ""; showPwdDialog = true }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(ssid, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        if (ssid == curSsid) {
                            Text("Connected", style = MaterialTheme.typography.labelSmall, color = appColors.success)
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
    val saveMap = mutableMapOf<String, Any>()
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
    configRepo.saveConfig(saveMap)
}
