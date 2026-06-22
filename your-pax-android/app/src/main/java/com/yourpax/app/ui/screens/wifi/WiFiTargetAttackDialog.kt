package com.yourpax.app.ui.screens.wifi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import com.yourpax.app.R
import com.yourpax.app.ui.components.LottieAnim
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yourpax.app.data.api.models.WiFiNetwork
import com.yourpax.app.data.repository.WiFiRepository
import com.yourpax.app.ui.components.DotState
import com.yourpax.app.ui.components.StatusDot
import com.yourpax.app.ui.theme.rememberAppColors
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
    var deauthChannel by remember { mutableStateOf(network.channel) }
    var wpsPixie by remember { mutableStateOf(true) }
    var wpsBrute by remember { mutableStateOf(false) }
    var wpsPbc by remember { mutableStateOf(false) }
    var wpsPin by remember { mutableStateOf("") }
    var wpsDelay by remember { mutableStateOf("") }
    var wpsForce by remember { mutableStateOf(false) }
    var wpsShowCmd by remember { mutableStateOf(false) }
    var wpsVerbose by remember { mutableStateOf(false) }
    var wpsWrite by remember { mutableStateOf(false) }
    var wpsVulnList by remember { mutableStateOf("") }
    var wpsLoop by remember { mutableStateOf(false) }
    var wpsReverseScan by remember { mutableStateOf(false) }
    var wpsMtkWifi by remember { mutableStateOf(false) }
    var wpsIfaceDown by remember { mutableStateOf(true) }
    var pendingAttackConfirm by remember { mutableStateOf<AttackType?>(null) }
    var showStopAllConfirm by remember { mutableStateOf(false) }
    val networkRepo = remember { com.yourpax.app.data.repository.NetworkRepository() }
    val appColors = rememberAppColors()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Network header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = appColors.surfaceContainerLow,
                tonalElevation = 0.dp
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(network.ssid.ifEmpty { "<Hidden>" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        val sigVal = network.signal.toIntOrNull() ?: -100
                        val sigColor = when {
                            sigVal >= -50 -> appColors.success
                            sigVal >= -70 -> appColors.warning
                            else -> MaterialTheme.colorScheme.error
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${network.bssid} \u00b7 Ch ${network.channel}", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
                            Spacer(Modifier.width(6.dp))
                            Text("${network.signal}dBm", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = sigColor)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Attack type chips
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = selectedAttack == AttackType.HANDSHAKE, onClick = { focusManager.clearFocus(); selectedAttack = AttackType.HANDSHAKE }, label = { Text("Handshake", maxLines = 1) }, leadingIcon = { LottieAnim(rawResId = R.raw.attack_bruteforce, modifier = Modifier.size(14.dp)) }, modifier = Modifier.weight(1f))
                FilterChip(selected = selectedAttack == AttackType.DEAUTH, onClick = { focusManager.clearFocus(); selectedAttack = AttackType.DEAUTH }, label = { Text("Deauth", maxLines = 1) }, leadingIcon = { LottieAnim(rawResId = R.raw.attack_deauth, modifier = Modifier.size(14.dp)) }, modifier = Modifier.weight(1f))
                FilterChip(selected = selectedAttack == AttackType.PMKID, onClick = { focusManager.clearFocus(); selectedAttack = AttackType.PMKID }, label = { Text("PMKID", maxLines = 1) }, leadingIcon = { LottieAnim(rawResId = R.raw.attack_scan, modifier = Modifier.size(14.dp)) }, modifier = Modifier.weight(1f))
                FilterChip(selected = selectedAttack == AttackType.WPS, onClick = { focusManager.clearFocus(); selectedAttack = AttackType.WPS }, label = { Text("WPS", maxLines = 1) }, leadingIcon = { LottieAnim(rawResId = R.raw.attack_wps, modifier = Modifier.size(14.dp)) }, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))

            // Attack card with animation
            AnimatedVisibility(
                visible = selectedAttack != null,
                enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 4 },
                exit = androidx.compose.animation.fadeOut(tween(150))
            ) {
                when (selectedAttack) {
                    AttackType.HANDSHAKE -> HandshakeCard(repo, scope, hsRunning, hsOutput, hsPrefix, onConfirmStart={ pendingAttackConfirm = AttackType.HANDSHAKE }, onHsRunning={hsRunning=it}, onHsOutput={hsOutput=it}, onHsPrefix={hsPrefix=it}, onStatus=onStatus)
                    AttackType.DEAUTH -> DeauthCard(network, repo, scope, deauthOutput, showDeauthOutput, deauthClient, deauthCount, deauthContinuous, deauthChannel, onConfirmStart={ pendingAttackConfirm = AttackType.DEAUTH }, onDeauthOutput={deauthOutput=it}, onShowDeauthOutput={showDeauthOutput=it}, onDeauthClient={deauthClient=it}, onDeauthCount={deauthCount=it}, onDeauthContinuous={deauthContinuous=it}, onDeauthChannel={deauthChannel=it}, onStatus=onStatus)
                    AttackType.PMKID -> PmkidCard(repo, scope, pmkidRunning, pmkidOutput, pmkidPrefix, onConfirmStart={ pendingAttackConfirm = AttackType.PMKID }, onPmkidRunning={pmkidRunning=it}, onPmkidOutput={pmkidOutput=it}, onPmkidPrefix={pmkidPrefix=it}, onStatus=onStatus)
                    AttackType.WPS -> WpsCard(repo, scope, wpsRunning, wpsOutput, wpsPixie, wpsBrute, wpsPbc, wpsPin, wpsDelay, wpsForce, wpsShowCmd, wpsVerbose, wpsIfaceDown, wpsWrite, wpsVulnList, wpsLoop, wpsReverseScan, wpsMtkWifi, onConfirmStart={ pendingAttackConfirm = AttackType.WPS }, onWpsRunning={wpsRunning=it}, onWpsOutput={wpsOutput=it}, onWpsPixie={wpsPixie=it}, onWpsBrute={wpsBrute=it}, onWpsPbc={wpsPbc=it}, onWpsPin={wpsPin=it}, onWpsDelay={wpsDelay=it}, onWpsForce={wpsForce=it}, onWpsShowCmd={wpsShowCmd=it}, onWpsVerbose={wpsVerbose=it}, onWpsIfaceDown={wpsIfaceDown=it}, onWpsWrite={wpsWrite=it}, onWpsVulnList={wpsVulnList=it}, onWpsLoop={wpsLoop=it}, onWpsReverseScan={wpsReverseScan=it}, onWpsMtkWifi={wpsMtkWifi=it}, onStatus=onStatus)
                    null -> {}
                }
            }

            if (selectedAttack == null) {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Text("Select an attack type above", color = appColors.subtleText, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Stop All button
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                OutlinedButton(
                    onClick = { showStopAllConfirm = true },
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Stop All Attacks")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    // Status polling
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

    // Confirmation dialogs
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
            text = { Text("Send ${if (deauthContinuous) "continuous" else deauthCount} deauth packets to ${network.bssid}?") },
            confirmButton = { TextButton(onClick = { pendingAttackConfirm = null; scope.launch { val count = if (deauthContinuous) 0 else (deauthCount.toIntOrNull() ?: 10); val r = repo.deauth(network.bssid, deauthClient, count, deauthChannel.toIntOrNull() ?: 1); if (r.isSuccess) { deauthOutput = "Deauth sent: $count packets\n"; showDeauthOutput = true; onStatus("Deauth sent") } else { onStatus(r.exceptionOrNull()?.message ?: "Failed") } } }) { Text("Send", color = MaterialTheme.colorScheme.error) } },
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
            text = { Text("Start WPS attack on ${network.ssid.ifEmpty { network.bssid }}? This may take several minutes.") },
            confirmButton = { TextButton(onClick = { pendingAttackConfirm = null; scope.launch { val params = mutableMapOf<String, Any>("bssid" to network.bssid, "pixie" to wpsPixie, "bruteforce" to wpsBrute, "pbc" to wpsPbc); if (wpsPin.isNotEmpty()) params["pin"] = wpsPin; if (wpsDelay.isNotEmpty()) params["delay"] = wpsDelay.toFloatOrNull() ?: 0f; params["pixie_force"] = wpsForce; params["show_pixie_cmd"] = wpsShowCmd; params["verbose"] = wpsVerbose; params["iface_down"] = wpsIfaceDown; params["write"] = wpsWrite; if (wpsVulnList.isNotEmpty()) params["vuln_list"] = wpsVulnList; params["loop"] = wpsLoop; params["reverse_scan"] = wpsReverseScan; params["mtk_wifi"] = wpsMtkWifi; val r = repo.startOneshot(params); if (r.isSuccess) { wpsRunning = true; wpsOutput = "Starting OneShot...\n"; onStatus("WPS started") } else { onStatus(r.exceptionOrNull()?.message ?: "Failed") } } }) { Text("Start Attack") } },
            dismissButton = { TextButton(onClick = { pendingAttackConfirm = null }) { Text("Cancel") } }
        )
        null -> {}
    }

    if (showStopAllConfirm) {
        AlertDialog(
            onDismissRequest = { showStopAllConfirm = false },
            title = { Text("Stop All Attacks?") },
            text = { Text("Stop all running WiFi attacks including handshake capture, PMKID capture, WPS attack, and deauth?") },
            confirmButton = { TextButton(onClick = { showStopAllConfirm = false; scope.launch { networkRepo.stopAll().onSuccess { onStatus("All attacks stopped"); hsRunning = false; pmkidRunning = false; wpsRunning = false }.onFailure { onStatus("Stop all failed: ${it.message}") } } }) { Text("Stop All", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showStopAllConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun HandshakeCard(repo: WiFiRepository, scope: kotlinx.coroutines.CoroutineScope, hsRunning: Boolean, hsOutput: String, hsPrefix: String, onConfirmStart: () -> Unit = {}, onHsRunning: (Boolean) -> Unit, onHsOutput: (String) -> Unit, onHsPrefix: (String) -> Unit, onStatus: (String) -> Unit) {
    val appColors = rememberAppColors()
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = appColors.surfaceContainerLow, tonalElevation = 0.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusDot(if (hsRunning) DotState.Active else DotState.Inactive)
                Text("Handshake Capture", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            }
            Text("Capture WPA/WPA2 4-way handshake by listening on the channel. Monitor mode required.", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
            OutlinedTextField(
                value = hsPrefix, onValueChange = onHsPrefix,
                label = { Text("Output prefix") }, placeholder = { Text("auto") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { /* done */ })
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConfirmStart, enabled = !hsRunning, shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f)) { Text("Start Capture") }
                OutlinedButton(onClick = { scope.launch { repo.stopHandshake(); onHsRunning(false); onHsOutput(""); onStatus("Handshake stopped") } }, enabled = hsRunning, shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f)) { Text("Stop") }
            }
            AnimatedVisibility(visible = hsOutput.isNotEmpty()) {
                Surface(color = appColors.terminalBackground, shape = MaterialTheme.shapes.extraSmall, tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp)) {
                    Text(hsOutput, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = appColors.terminalText)
                }
            }
        }
    }
}

@Composable
private fun DeauthCard(network: WiFiNetwork, repo: WiFiRepository, scope: kotlinx.coroutines.CoroutineScope, deauthOutput: String, showDeauthOutput: Boolean, deauthClient: String, deauthCount: String, deauthContinuous: Boolean, deauthChannel: String, onConfirmStart: () -> Unit = {}, onDeauthOutput: (String) -> Unit, onShowDeauthOutput: (Boolean) -> Unit, onDeauthClient: (String) -> Unit, onDeauthCount: (String) -> Unit, onDeauthContinuous: (Boolean) -> Unit, onDeauthChannel: (String) -> Unit, onStatus: (String) -> Unit) {
    val appColors = rememberAppColors()
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = appColors.surfaceContainerLow, tonalElevation = 0.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusDot(if (showDeauthOutput) DotState.Active else DotState.Inactive)
                Text("Deauth Attack", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            }
            Text("Send deauth packets to disconnect clients. Use with handshake capture to force reconnection.", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
            OutlinedTextField(value = deauthClient, onValueChange = onDeauthClient, label = { Text("Client MAC") }, placeholder = { Text("ff:ff:ff:ff:ff:ff = broadcast") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = deauthCount, onValueChange = onDeauthCount, label = { Text("Count") }, placeholder = { Text("0 = ∞") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next), modifier = Modifier.weight(1f))
                OutlinedTextField(value = deauthChannel, onValueChange = onDeauthChannel, label = { Text("Channel") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), modifier = Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Checkbox(checked = deauthContinuous, onCheckedChange = onDeauthContinuous)
                Text("Continuous", style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConfirmStart, shape = MaterialTheme.shapes.small, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.weight(1f)) { Text("Send Deauth") }
                OutlinedButton(onClick = { scope.launch { repo.deauth(network.bssid, "ff:ff:ff:ff:ff:ff", 0, deauthChannel.toIntOrNull() ?: 1).onSuccess { onDeauthOutput("Stop signal sent\n"); onShowDeauthOutput(true); onStatus("Deauth stop sent") }.onFailure { onStatus("Failed: ${it.message}") } } }, shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f)) { Text("Stop") }
            }
            AnimatedVisibility(visible = showDeauthOutput && deauthOutput.isNotEmpty()) {
                Surface(color = appColors.terminalBackground, shape = MaterialTheme.shapes.extraSmall, tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth().heightIn(max = 100.dp)) {
                    Text(deauthOutput, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun PmkidCard(repo: WiFiRepository, scope: kotlinx.coroutines.CoroutineScope, pmkidRunning: Boolean, pmkidOutput: String, pmkidPrefix: String, onConfirmStart: () -> Unit = {}, onPmkidRunning: (Boolean) -> Unit, onPmkidOutput: (String) -> Unit, onPmkidPrefix: (String) -> Unit, onStatus: (String) -> Unit) {
    val appColors = rememberAppColors()
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = appColors.surfaceContainerLow, tonalElevation = 0.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusDot(if (pmkidRunning) DotState.Active else DotState.Inactive)
                Text("PMKID Capture", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            }
            Text("Send fake association frames to capture PMKID hash from AP. Monitor mode required.", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)
            OutlinedTextField(value = pmkidPrefix, onValueChange = onPmkidPrefix, label = { Text("Output prefix") }, placeholder = { Text("auto") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConfirmStart, enabled = !pmkidRunning, shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f)) { Text("Start Capture") }
                OutlinedButton(onClick = { scope.launch { repo.stopPmkid(); onPmkidRunning(false); onPmkidOutput(""); onStatus("PMKID stopped") } }, enabled = pmkidRunning, shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f)) { Text("Stop") }
            }
            AnimatedVisibility(visible = pmkidOutput.isNotEmpty()) {
                Surface(color = appColors.terminalBackground, shape = MaterialTheme.shapes.extraSmall, tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp)) {
                    Text(pmkidOutput, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = appColors.terminalText)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WpsCard(repo: WiFiRepository, scope: kotlinx.coroutines.CoroutineScope, wpsRunning: Boolean, wpsOutput: String, wpsPixie: Boolean, wpsBrute: Boolean, wpsPbc: Boolean, wpsPin: String, wpsDelay: String, wpsForce: Boolean, wpsShowCmd: Boolean, wpsVerbose: Boolean, wpsIfaceDown: Boolean, wpsWrite: Boolean, wpsVulnList: String, wpsLoop: Boolean, wpsReverseScan: Boolean, wpsMtkWifi: Boolean, onConfirmStart: () -> Unit = {}, onWpsRunning: (Boolean) -> Unit, onWpsOutput: (String) -> Unit, onWpsPixie: (Boolean) -> Unit, onWpsBrute: (Boolean) -> Unit, onWpsPbc: (Boolean) -> Unit, onWpsPin: (String) -> Unit, onWpsDelay: (String) -> Unit, onWpsForce: (Boolean) -> Unit, onWpsShowCmd: (Boolean) -> Unit, onWpsVerbose: (Boolean) -> Unit, onWpsIfaceDown: (Boolean) -> Unit, onWpsWrite: (Boolean) -> Unit, onWpsVulnList: (String) -> Unit, onWpsLoop: (Boolean) -> Unit, onWpsReverseScan: (Boolean) -> Unit, onWpsMtkWifi: (Boolean) -> Unit, onStatus: (String) -> Unit) {
    val appColors = rememberAppColors()
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = appColors.surfaceContainerLow, tonalElevation = 0.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusDot(if (wpsRunning) DotState.Active else DotState.Inactive)
                    Text("WPS Attacks", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                }
                LottieAnim(rawResId = R.raw.attack_wps, modifier = Modifier.size(28.dp))
            }
            Text("WPS PIN recovery via Pixie Dust, bruteforce, or PBC. All OneShot options exposed.", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText)

            Text("Attack Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = wpsPixie, onClick = { onWpsPixie(!wpsPixie) }, label = { Text("Pixie", maxLines = 1) }, modifier = Modifier.weight(1f))
                FilterChip(selected = wpsBrute, onClick = { onWpsBrute(!wpsBrute) }, label = { Text("Brute", maxLines = 1) }, modifier = Modifier.weight(1f))
                FilterChip(selected = wpsPbc, onClick = { onWpsPbc(!wpsPbc) }, label = { Text("PBC", maxLines = 1) }, modifier = Modifier.weight(1f))
            }

            Text("PIN & Timing", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = wpsPin, onValueChange = onWpsPin, label = { Text("PIN") }, placeholder = { Text("12345678") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next), modifier = Modifier.weight(1f))
                OutlinedTextField(value = wpsDelay, onValueChange = onWpsDelay, label = { Text("Delay (s)") }, placeholder = { Text("0") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done), modifier = Modifier.width(90.dp))
            }

            Text("Advanced", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(selected = wpsForce, onClick = { onWpsForce(!wpsForce) }, label = { Text("Pixie Force") })
                FilterChip(selected = wpsShowCmd, onClick = { onWpsShowCmd(!wpsShowCmd) }, label = { Text("Show Cmd") })
                FilterChip(selected = wpsVerbose, onClick = { onWpsVerbose(!wpsVerbose) }, label = { Text("Verbose") })
                FilterChip(selected = wpsIfaceDown, onClick = { onWpsIfaceDown(!wpsIfaceDown) }, label = { Text("IFace Down") })
                FilterChip(selected = wpsWrite, onClick = { onWpsWrite(!wpsWrite) }, label = { Text("Save") })
                FilterChip(selected = wpsLoop, onClick = { onWpsLoop(!wpsLoop) }, label = { Text("Loop") })
                FilterChip(selected = wpsReverseScan, onClick = { onWpsReverseScan(!wpsReverseScan) }, label = { Text("Rev Scan") })
                FilterChip(selected = wpsMtkWifi, onClick = { onWpsMtkWifi(!wpsMtkWifi) }, label = { Text("MTK") })
            }

            Text("Files", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(value = wpsVulnList, onValueChange = onWpsVulnList, label = { Text("Vuln list path") }, placeholder = { Text("actions/vulnwsc.txt") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConfirmStart, enabled = !wpsRunning, shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f)) { Text("Start WPS Attack") }
                OutlinedButton(onClick = { scope.launch { repo.stopOneshot(); onWpsRunning(false); onWpsOutput("Stopped by user\n"); onStatus("WPS stopped") } }, enabled = wpsRunning, shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f)) { Text("Stop") }
            }
            OutlinedButton(onClick = {
                scope.launch {
                    val s = repo.getOneshotStatus().getOrNull()
                    if (s != null) onWpsOutput(s.output.joinToString("\n"))
                    onStatus("Output refreshed")
                }
            }, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) { Text("Refresh Output") }

            AnimatedVisibility(visible = wpsOutput.isNotEmpty()) {
                Surface(color = appColors.terminalBackground, shape = MaterialTheme.shapes.extraSmall, tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                    Text(wpsOutput, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = appColors.terminalText)
                }
            }
        }
    }
}