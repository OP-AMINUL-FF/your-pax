package com.yourpax.app.ui.screens.wifi

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yourpax.app.data.api.models.WiFiNetwork
import com.yourpax.app.data.repository.WiFiRepository
import com.yourpax.app.ui.components.*
import com.yourpax.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AttackType { HANDSHAKE, DEAUTH, PMKID, WPS }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WiFiTargetAttackDialog(
    network: WiFiNetwork,
    onDismiss: () -> Unit,
    repo: WiFiRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onStatus: (String) -> Unit
) {
    var selectedAttack by remember { mutableStateOf<AttackType?>(null) }
    var hsRunning by remember { mutableStateOf(false) }
    var pmkidRunning by remember { mutableStateOf(false) }
    var wpsRunning by remember { mutableStateOf(false) }
    var hsOutput by remember { mutableStateOf("") }
    var pmkidOutput by remember { mutableStateOf("") }
    var deauthOutput by remember { mutableStateOf("") }
    var wpsOutput by remember { mutableStateOf("") }
    var showDeauthOutput by remember { mutableStateOf(false) }
    var hsPrefix by remember { mutableStateOf(network.ssid.ifEmpty { "hidden" }) }
    var pmkidPrefix by remember { mutableStateOf(network.ssid.ifEmpty { "hidden" }) }
    var deauthClient by remember { mutableStateOf("ff:ff:ff:ff:ff:ff") }
    var deauthCount by remember { mutableStateOf("10") }
    var deauthContinuous by remember { mutableStateOf(false) }
    var wpsPixie by remember { mutableStateOf(true) }
    var wpsBrute by remember { mutableStateOf(false) }
    var wpsPbc by remember { mutableStateOf(false) }
    var wpsPin by remember { mutableStateOf("") }
    var wpsDelay by remember { mutableStateOf("") }
    var wpsForce by remember { mutableStateOf(false) }
    var wpsShowCmd by remember { mutableStateOf(false) }
    var wpsVerbose by remember { mutableStateOf(false) }
    var wpsIfaceDown by remember { mutableStateOf(true) }
    var pendingAttackConfirm by remember { mutableStateOf<AttackType?>(null) }
    var showStopAllConfirm by remember { mutableStateOf(false) }
    val networkRepo = remember { com.yourpax.app.data.repository.NetworkRepository() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wifi, contentDescription = null, tint = Primary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(network.ssid.ifEmpty { "<Hidden>" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${network.bssid} \u00b7 Ch ${network.channel} \u00b7 ${network.signal}dBm", style = MaterialTheme.typography.bodySmall, color = SubtleText)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = selectedAttack == AttackType.HANDSHAKE, onClick = { selectedAttack = AttackType.HANDSHAKE }, label = { Text("Handshake") }, leadingIcon = { Icon(Icons.Default.Lock, null, Modifier.size(16.dp)) })
                FilterChip(selected = selectedAttack == AttackType.DEAUTH, onClick = { selectedAttack = AttackType.DEAUTH }, label = { Text("Deauth") }, leadingIcon = { Icon(Icons.Default.Block, null, Modifier.size(16.dp)) })
                FilterChip(selected = selectedAttack == AttackType.PMKID, onClick = { selectedAttack = AttackType.PMKID }, label = { Text("PMKID") }, leadingIcon = { Icon(Icons.Default.Key, null, Modifier.size(16.dp)) })
                FilterChip(selected = selectedAttack == AttackType.WPS, onClick = { selectedAttack = AttackType.WPS }, label = { Text("WPS") }, leadingIcon = { Icon(Icons.Default.VpnKey, null, Modifier.size(16.dp)) })
            }

            Spacer(Modifier.height(16.dp))

            when (selectedAttack) {
                AttackType.HANDSHAKE -> HandshakeCard(network, repo, scope, hsRunning, hsOutput, hsPrefix, onConfirmStart={ pendingAttackConfirm = AttackType.HANDSHAKE }, onHsRunning={hsRunning=it}, onHsOutput={hsOutput=it}, onHsPrefix={hsPrefix=it}, onStatus=onStatus)
                AttackType.DEAUTH -> DeauthCard(network, repo, scope, deauthOutput, showDeauthOutput, deauthClient, deauthCount, deauthContinuous, onConfirmStart={ pendingAttackConfirm = AttackType.DEAUTH }, onDeauthOutput={deauthOutput=it}, onShowDeauthOutput={showDeauthOutput=it}, onDeauthClient={deauthClient=it}, onDeauthCount={deauthCount=it}, onDeauthContinuous={deauthContinuous=it}, onStatus=onStatus)
                AttackType.PMKID -> PmkidCard(network, repo, scope, pmkidRunning, pmkidOutput, pmkidPrefix, onConfirmStart={ pendingAttackConfirm = AttackType.PMKID }, onPmkidRunning={pmkidRunning=it}, onPmkidOutput={pmkidOutput=it}, onPmkidPrefix={pmkidPrefix=it}, onStatus=onStatus)
                AttackType.WPS -> WpsCard(network, repo, scope, wpsRunning, wpsOutput, wpsPixie, wpsBrute, wpsPbc, wpsPin, wpsDelay, wpsForce, wpsShowCmd, wpsVerbose, wpsIfaceDown, onConfirmStart={ pendingAttackConfirm = AttackType.WPS }, onWpsRunning={wpsRunning=it}, onWpsOutput={wpsOutput=it}, onWpsPixie={wpsPixie=it}, onWpsBrute={wpsBrute=it}, onWpsPbc={wpsPbc=it}, onWpsPin={wpsPin=it}, onWpsDelay={wpsDelay=it}, onWpsForce={wpsForce=it}, onWpsShowCmd={wpsShowCmd=it}, onWpsVerbose={wpsVerbose=it}, onWpsIfaceDown={wpsIfaceDown=it}, onStatus=onStatus)
                null -> Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) { Text("Select an attack type above", color = SubtleText) }
            }

            Spacer(Modifier.height(16.dp))

            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                OutlinedButton(
                    onClick = { showStopAllConfirm = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Stop All Attacks")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    LaunchedEffect(hsRunning) {
        if (hsRunning) {
            while (true) {
                delay(2000)
                val s = repo.getHandshakeStatus().getOrNull()
                if (s != null && !s.running) { hsRunning = false; hsOutput += "\nCapture stopped"; break }
            }
        }
    }

    LaunchedEffect(pmkidRunning) {
        if (pmkidRunning) {
            while (true) {
                delay(2000)
                val s = repo.getPmkidStatus().getOrNull()
                if (s != null && !s.running) { pmkidRunning = false; pmkidOutput += "\nPMKID capture stopped"; break }
            }
        }
    }

    LaunchedEffect(wpsRunning) {
        if (wpsRunning) {
            while (true) {
                delay(2000)
                val s = repo.getOneshotStatus().getOrNull()
                if (s != null) {
                    wpsOutput = s.output.joinToString("\n")
                    if (!s.running) { wpsRunning = false; wpsOutput += "\nWPS attack stopped"; break }
                }
            }
        }
    }

    when (pendingAttackConfirm) {
        AttackType.HANDSHAKE -> AlertDialog(
            onDismissRequest = { pendingAttackConfirm = null },
            title = { Text("Start Handshake Capture?") },
            text = { Text("Start capturing WPA handshake on ${network.ssid.ifEmpty { network.bssid }} (Ch ${network.channel})? Requires monitor mode.") },
            confirmButton = { TextButton(onClick = { pendingAttackConfirm = null; scope.launch { val r = repo.startHandshake(network.bssid, network.channel, hsPrefix.ifEmpty { network.ssid.ifEmpty { "hidden" } }); if (r.isSuccess) { hsRunning = true; hsOutput = "Capturing...\n"; onStatus("Handshake started") } else { onStatus(r.exceptionOrNull()?.message ?: "Failed") } } }) { Text("Start") } },
            dismissButton = { TextButton(onClick = { pendingAttackConfirm = null }) { Text("Cancel") } }
        )
        AttackType.DEAUTH -> AlertDialog(
            onDismissRequest = { pendingAttackConfirm = null },
            title = { Text("Send Deauth Packets?") },
            text = { Text("Send ${if (deauthContinuous) "continuous" else deauthCount} deauth packets to ${network.bssid}? Clients will temporarily lose connection.") },
            confirmButton = { TextButton(onClick = { pendingAttackConfirm = null; scope.launch { val count = if (deauthContinuous) 0 else (deauthCount.toIntOrNull() ?: 10); val r = repo.deauth(network.bssid, deauthClient, count, network.channel.toIntOrNull() ?: 1); if (r.isSuccess) { deauthOutput = "Deauth sent: $count packets\n"; showDeauthOutput = true; onStatus("Deauth sent") } else { onStatus(r.exceptionOrNull()?.message ?: "Failed") } } }) { Text("Send", color = Error) } },
            dismissButton = { TextButton(onClick = { pendingAttackConfirm = null }) { Text("Cancel") } }
        )
        AttackType.PMKID -> AlertDialog(
            onDismissRequest = { pendingAttackConfirm = null },
            title = { Text("Start PMKID Capture?") },
            text = { Text("Start PMKID capture on ${network.ssid.ifEmpty { network.bssid }} (Ch ${network.channel})? Requires monitor mode.") },
            confirmButton = { TextButton(onClick = { pendingAttackConfirm = null; scope.launch { val r = repo.startPmkid(network.bssid, network.channel, pmkidPrefix.ifEmpty { network.ssid.ifEmpty { "hidden" } }); if (r.isSuccess) { pmkidRunning = true; pmkidOutput = "Capturing PMKID with fakeauth...\n"; onStatus("PMKID started") } else { onStatus(r.exceptionOrNull()?.message ?: "Failed") } } }) { Text("Start") } },
            dismissButton = { TextButton(onClick = { pendingAttackConfirm = null }) { Text("Cancel") } }
        )
        AttackType.WPS -> AlertDialog(
            onDismissRequest = { pendingAttackConfirm = null },
            title = { Text("Start WPS Attack?") },
            text = { Text("Start WPS attack on ${network.ssid.ifEmpty { network.bssid }}? This may take several minutes. The Wi-Fi interface may be taken down during the attack.") },
            confirmButton = { TextButton(onClick = { pendingAttackConfirm = null; scope.launch { val params = mutableMapOf<String, Any>("bssid" to network.bssid, "pixie" to wpsPixie, "bruteforce" to wpsBrute, "pbc" to wpsPbc); if (wpsPin.isNotEmpty()) params["pin"] = wpsPin; if (wpsDelay.isNotEmpty()) params["delay"] = wpsDelay.toFloatOrNull() ?: 0f; params["pixie_force"] = wpsForce; params["show_pixie_cmd"] = wpsShowCmd; params["verbose"] = wpsVerbose; params["iface_down"] = wpsIfaceDown; val r = repo.startOneshot(params); if (r.isSuccess) { wpsRunning = true; wpsOutput = "Starting OneShot...\n"; onStatus("WPS started") } else { onStatus(r.exceptionOrNull()?.message ?: "Failed") } } }) { Text("Start Attack") } },
            dismissButton = { TextButton(onClick = { pendingAttackConfirm = null }) { Text("Cancel") } }
        )
        null -> {}
    }

    if (showStopAllConfirm) {
        AlertDialog(
            onDismissRequest = { showStopAllConfirm = false },
            title = { Text("Stop All Attacks?") },
            text = { Text("Stop all running WiFi attacks including handshake capture, PMKID capture, WPS attack, and deauth?") },
            confirmButton = { TextButton(onClick = { showStopAllConfirm = false; scope.launch { networkRepo.stopAll().onSuccess { onStatus("All attacks stopped"); hsRunning = false; pmkidRunning = false; wpsRunning = false }.onFailure { onStatus("Stop all failed: ${it.message}") } } }) { Text("Stop All", color = Error) } },
            dismissButton = { TextButton(onClick = { showStopAllConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StatusDot(state: String) {
    val color = when (state) {
        "on" -> Success; "busy" -> Warning
        else -> Color(0xFF444444)
    }
    Box(Modifier.size(8.dp).background(color, RoundedCornerShape(50)))
}

@Composable
private fun HandshakeCard(network: WiFiNetwork, repo: WiFiRepository, scope: kotlinx.coroutines.CoroutineScope, hsRunning: Boolean, hsOutput: String, hsPrefix: String, onConfirmStart: () -> Unit = {}, onHsRunning: (Boolean) -> Unit, onHsOutput: (String) -> Unit, onHsPrefix: (String) -> Unit, onStatus: (String) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = CardDefaults.outlinedCardBorder().copy(width = 0.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusDot(if (hsRunning) "busy" else "off")
                Text("Handshake Capture", fontWeight = FontWeight.SemiBold)
            }
            Text("Capture WPA/WPA2 4-way handshake by listening on the channel", style = MaterialTheme.typography.bodySmall, color = SubtleText)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(value = hsPrefix, onValueChange = onHsPrefix, label = { Text("Output prefix") }, placeholder = { Text("auto") }, singleLine = true, modifier = Modifier.weight(1f))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(onClick = onConfirmStart, enabled = !hsRunning, shape = RoundedCornerShape(8.dp)) { Text("Start Capture") }
                    OutlinedButton(onClick = { scope.launch { repo.stopHandshake(); onHsRunning(false); onHsOutput(""); onStatus("Handshake stopped") } }, enabled = hsRunning, shape = RoundedCornerShape(8.dp)) { Text("Stop") }
                }
            }
            if (hsOutput.isNotEmpty()) {
                Surface(color = Color(0xFF0D0D0D), shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp)) {
                    Text(hsOutput, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = Success)
                }
            }
        }
    }
}

@Composable
private fun DeauthCard(network: WiFiNetwork, repo: WiFiRepository, scope: kotlinx.coroutines.CoroutineScope, deauthOutput: String, showDeauthOutput: Boolean, deauthClient: String, deauthCount: String, deauthContinuous: Boolean, onConfirmStart: () -> Unit = {}, onDeauthOutput: (String) -> Unit, onShowDeauthOutput: (Boolean) -> Unit, onDeauthClient: (String) -> Unit, onDeauthCount: (String) -> Unit, onDeauthContinuous: (Boolean) -> Unit, onStatus: (String) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusDot("off")
                Text("Deauth Attack", fontWeight = FontWeight.SemiBold)
            }
            Text("Send deauth packets to force clients to reconnect and capture handshake", style = MaterialTheme.typography.bodySmall, color = SubtleText)
            OutlinedTextField(value = deauthClient, onValueChange = onDeauthClient, label = { Text("Client MAC (FF:FF:FF:FF:FF:FF = broadcast)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = deauthCount, onValueChange = onDeauthCount, label = { Text("Packet Count (0 = continuous)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Checkbox(checked = deauthContinuous, onCheckedChange = onDeauthContinuous)
                Text("Continuous", style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConfirmStart, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Error)) { Text("Send Deauth") }
                OutlinedButton(onClick = { scope.launch { repo.deauth(network.bssid, "ff:ff:ff:ff:ff:ff", 0, 0); onDeauthOutput("Stopped\n"); onShowDeauthOutput(true); onStatus("Deauth stopped") } }, shape = RoundedCornerShape(8.dp)) { Text("Stop") }
            }
            if (showDeauthOutput && deauthOutput.isNotEmpty()) {
                Surface(color = Color(0xFF0D0D0D), shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 100.dp)) {
                    Text(deauthOutput, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = Error)
                }
            }
        }
    }
}

@Composable
private fun PmkidCard(network: WiFiNetwork, repo: WiFiRepository, scope: kotlinx.coroutines.CoroutineScope, pmkidRunning: Boolean, pmkidOutput: String, pmkidPrefix: String, onConfirmStart: () -> Unit = {}, onPmkidRunning: (Boolean) -> Unit, onPmkidOutput: (String) -> Unit, onPmkidPrefix: (String) -> Unit, onStatus: (String) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusDot(if (pmkidRunning) "busy" else "off")
                Text("PMKID Capture (Association)", fontWeight = FontWeight.SemiBold)
            }
            Text("Send fake association frames to capture PMKID hash from AP", style = MaterialTheme.typography.bodySmall, color = SubtleText)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(value = pmkidPrefix, onValueChange = onPmkidPrefix, label = { Text("Output prefix") }, placeholder = { Text("auto") }, singleLine = true, modifier = Modifier.weight(1f))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(onClick = onConfirmStart, enabled = !pmkidRunning, shape = RoundedCornerShape(8.dp)) { Text("Start Capture") }
                    OutlinedButton(onClick = { scope.launch { repo.stopPmkid(); onPmkidRunning(false); onPmkidOutput(""); onStatus("PMKID stopped") } }, enabled = pmkidRunning, shape = RoundedCornerShape(8.dp)) { Text("Stop") }
                }
            }
            if (pmkidOutput.isNotEmpty()) {
                Surface(color = Color(0xFF0D0D0D), shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp)) {
                    Text(pmkidOutput, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = Success)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WpsCard(network: WiFiNetwork, repo: WiFiRepository, scope: kotlinx.coroutines.CoroutineScope, wpsRunning: Boolean, wpsOutput: String, wpsPixie: Boolean, wpsBrute: Boolean, wpsPbc: Boolean, wpsPin: String, wpsDelay: String, wpsForce: Boolean, wpsShowCmd: Boolean, wpsVerbose: Boolean, wpsIfaceDown: Boolean, onConfirmStart: () -> Unit = {}, onWpsRunning: (Boolean) -> Unit, onWpsOutput: (String) -> Unit, onWpsPixie: (Boolean) -> Unit, onWpsBrute: (Boolean) -> Unit, onWpsPbc: (Boolean) -> Unit, onWpsPin: (String) -> Unit, onWpsDelay: (String) -> Unit, onWpsForce: (Boolean) -> Unit, onWpsShowCmd: (Boolean) -> Unit, onWpsVerbose: (Boolean) -> Unit, onWpsIfaceDown: (Boolean) -> Unit, onStatus: (String) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusDot(if (wpsRunning) "busy" else "off")
                Text("WPS Attacks (OneShot)", fontWeight = FontWeight.SemiBold)
            }
            Text("WPS PIN recovery via Pixie Dust, bruteforce, or PBC. All options from OneShot.", style = MaterialTheme.typography.bodySmall, color = SubtleText)

            Text("Attack Type", style = MaterialTheme.typography.labelMedium, color = SubtleText)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = wpsPixie, onClick = { onWpsPixie(!wpsPixie) }, label = { Text("Pixie Dust") })
                FilterChip(selected = wpsBrute, onClick = { onWpsBrute(!wpsBrute) }, label = { Text("Bruteforce") })
                FilterChip(selected = wpsPbc, onClick = { onWpsPbc(!wpsPbc) }, label = { Text("PBC") })
            }

            Text("PIN & Timing", style = MaterialTheme.typography.labelMedium, color = SubtleText)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = wpsPin, onValueChange = onWpsPin, label = { Text("Custom PIN") }, placeholder = { Text("12345678") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = wpsDelay, onValueChange = onWpsDelay, label = { Text("Delay (s)") }, placeholder = { Text("0") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(100.dp))
            }

            Text("Advanced", style = MaterialTheme.typography.labelMedium, color = SubtleText)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = wpsForce, onClick = { onWpsForce(!wpsForce) }, label = { Text("Pixie Force") })
                FilterChip(selected = wpsShowCmd, onClick = { onWpsShowCmd(!wpsShowCmd) }, label = { Text("Show Pixie Cmd") })
                FilterChip(selected = wpsVerbose, onClick = { onWpsVerbose(!wpsVerbose) }, label = { Text("Verbose") })
                FilterChip(selected = wpsIfaceDown, onClick = { onWpsIfaceDown(!wpsIfaceDown) }, label = { Text("IFace Down") })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConfirmStart, enabled = !wpsRunning, shape = RoundedCornerShape(8.dp)) { Text("Start WPS Attack") }
                OutlinedButton(onClick = { scope.launch { repo.stopOneshot(); onWpsRunning(false); onWpsOutput("Stopped by user\n"); onStatus("WPS stopped") } }, enabled = wpsRunning, shape = RoundedCornerShape(8.dp)) { Text("Stop") }
                OutlinedButton(onClick = {
                    scope.launch {
                        val s = repo.getOneshotStatus().getOrNull()
                        if (s != null) onWpsOutput(s.output.joinToString("\n"))
                        onStatus("Output refreshed")
                    }
                }, enabled = true, shape = RoundedCornerShape(8.dp)) { Text("Refresh Output") }
            }

            if (wpsOutput.isNotEmpty()) {
                Surface(color = Color(0xFF0D0D0D), shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp)) {
                    Text(wpsOutput, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = Success)
                }
            }
        }
    }
}