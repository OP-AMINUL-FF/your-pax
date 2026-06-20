package com.yourpax.app.ui.screens.splash

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.PermissionChecker
import com.yourpax.app.data.bluetooth.BluetoothConnectionState
import com.yourpax.app.data.bluetooth.BluetoothScanner
import com.yourpax.app.data.comm.BtCommManager
import com.yourpax.app.data.comm.CommHolder
import com.yourpax.app.data.comm.EventBus
import com.yourpax.app.ui.theme.rememberAppColors
import kotlinx.coroutines.launch

private fun requiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

@Composable
fun SplashScreen(onConnected: () -> Unit, onSkip: () -> Unit) {
    var connectionState by remember { mutableStateOf<BluetoothConnectionState>(BluetoothConnectionState.Idle) }
    val bluetoothAdapter = remember { BluetoothAdapter.getDefaultAdapter() }
    val context = LocalContext.current
    val scanner = remember { BluetoothScanner(bluetoothAdapter, context) }
    val scope = rememberCoroutineScope()

    fun startScanAndConnect() {
        connectionState = BluetoothConnectionState.Scanning
        var foundDevice: BluetoothDevice? = null
        scanner.startScan(
            onFound = { device ->
                val name = try { device.name } catch (_: SecurityException) { null }
                if (name?.contains("your-pax", ignoreCase = true) == true &&
                    foundDevice == null
                ) {
                    foundDevice = device
                    connectionState = BluetoothConnectionState.Connecting
                    scanner.stopScan()
                }
            },
            onFinished = {
                val device = foundDevice ?: scanner.findYourPaxDevice()
                if (device == null) {
                    connectionState = BluetoothConnectionState.Error(
                        "your-pax not found.\nMake sure it is powered on and discoverable."
                    )
                } else {
                    scope.launch {
                        connectionState = BluetoothConnectionState.Connecting
                        val btComm = BtCommManager(device.address)
                        try {
                            btComm.connect()
                            btComm.onEvent = { event, data -> EventBus.emit(event, data) }
                            CommHolder.comm = btComm
                            onConnected()
                        } catch (_: Exception) {
                            connectionState = BluetoothConnectionState.Error(
                                "Could not connect to your-pax via Bluetooth SPP.\n" +
                                    "Make sure your-pax is running and try again."
                            )
                        }
                    }
                }
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.values.all { it }) startScanAndConnect()
        else connectionState = BluetoothConnectionState.Error("Bluetooth permissions are required to connect.")
    }

    val appColors = rememberAppColors()

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(appColors.gradientStart, appColors.gradientEnd))),
        contentAlignment = Alignment.Center
    ) {
        when (val state = connectionState) {
            is BluetoothConnectionState.Idle -> SplashContent(onConnect = {
                if (bluetoothAdapter == null) connectionState = BluetoothConnectionState.Error("Bluetooth not supported")
                else if (!bluetoothAdapter.isEnabled) connectionState = BluetoothConnectionState.BluetoothOff
                else {
                    if (requiredPermissions().all { PermissionChecker.checkSelfPermission(context, it) == PermissionChecker.PERMISSION_GRANTED }) startScanAndConnect()
                    else permissionLauncher.launch(requiredPermissions())
                }
            }, onSkip = onSkip)
            is BluetoothConnectionState.CheckingBluetooth -> CheckingContent()
            is BluetoothConnectionState.BluetoothOff -> BluetoothOffContent(onEnable = { bluetoothAdapter?.enable(); startScanAndConnect() }, onSkip = onSkip)
            is BluetoothConnectionState.Scanning -> ScanningContent()
            is BluetoothConnectionState.Pairing -> PairingContent()
            is BluetoothConnectionState.AwaitingPan -> AwaitingPanContent(deviceName = state.deviceName, onContinueAnyway = { onConnected() })
            is BluetoothConnectionState.Connecting -> ConnectingContent()
            is BluetoothConnectionState.TestingApi -> TestingContent()
            is BluetoothConnectionState.Connected -> ConnectedContent(onContinue = onConnected)
            is BluetoothConnectionState.PermissionGranted -> CheckingContent()
            is BluetoothConnectionState.Error -> ErrorContent(message = state.message, onRetry = { connectionState = BluetoothConnectionState.Idle }, onSkip = onSkip)
        }
    }
}

@Composable
private fun SplashContent(onConnect: () -> Unit, onSkip: () -> Unit) {
    val appColors = rememberAppColors()
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("YOUR-PAX", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Your Pocket Security Companion", style = MaterialTheme.typography.bodyLarge, color = appColors.subtleText, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onConnect, modifier = Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.medium) { Text("Connect via Bluetooth", fontWeight = FontWeight.SemiBold) }
        OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth().height(48.dp), shape = MaterialTheme.shapes.medium) { Text("Skip for now") }
    }
}

@Composable
private fun CheckingContent() {
    val appColors = rememberAppColors()
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Text("Checking Bluetooth\u2026", style = MaterialTheme.typography.bodyLarge, color = appColors.subtleText)
    }
}

@Composable
private fun BluetoothOffContent(onEnable: () -> Unit, onSkip: () -> Unit) {
    val appColors = rememberAppColors()
    Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Bluetooth is Off", style = MaterialTheme.typography.headlineMedium, color = appColors.warning)
        Text("your-pax needs Bluetooth to discover and connect.", style = MaterialTheme.typography.bodyMedium, color = appColors.subtleText, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onEnable, shape = MaterialTheme.shapes.medium) { Text("Enable Bluetooth") }
        TextButton(onClick = onSkip) { Text("Skip", color = appColors.subtleText) }
    }
}

@Composable
private fun ScanningContent() {
    val appColors = rememberAppColors()
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Text("Searching for your-pax device\u2026", style = MaterialTheme.typography.bodyLarge, color = appColors.subtleText)
        Text("Make sure your-pax is powered on.", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText.copy(alpha = 0.6f))
    }
}

@Composable
private fun ConnectingContent() {
    val appColors = rememberAppColors()
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator(color = appColors.success, modifier = Modifier.size(48.dp))
        Text("Found your-pax\u2026", style = MaterialTheme.typography.bodyLarge, color = appColors.success)
    }
}

@Composable
private fun PairingContent() {
    val appColors = rememberAppColors()
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator(color = appColors.success, modifier = Modifier.size(48.dp))
        Text("Pairing with your-pax\u2026", style = MaterialTheme.typography.bodyLarge, color = appColors.success)
        Text("Confirm the pairing on your phone if prompted.", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText.copy(alpha = 0.6f), textAlign = TextAlign.Center)
    }
}

@Composable
private fun AwaitingPanContent(deviceName: String, onContinueAnyway: () -> Unit) {
    val appColors = rememberAppColors()
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(color = appColors.info, modifier = Modifier.size(48.dp))
        Text("Almost there!", style = MaterialTheme.typography.headlineSmall, color = appColors.info)
        Text(
            "Pairing complete with \"$deviceName\".\nNow enable Internet access:\nAndroid Settings \u2192 Bluetooth \u2192 your-pax \u2192 turn on \"Internet access\".",
            style = MaterialTheme.typography.bodyMedium, color = appColors.subtleText, textAlign = TextAlign.Center
        )
        Text("Waiting for the connection\u2026", style = MaterialTheme.typography.bodySmall, color = appColors.subtleText.copy(alpha = 0.6f))
        OutlinedButton(onClick = onContinueAnyway, shape = MaterialTheme.shapes.medium) { Text("Continue anyway") }
    }
}

@Composable
private fun TestingContent() {
    val appColors = rememberAppColors()
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator(color = appColors.info, modifier = Modifier.size(48.dp))
        Text("Testing connection\u2026", style = MaterialTheme.typography.bodyLarge, color = appColors.info)
    }
}

@Composable
private fun ConnectedContent(onContinue: () -> Unit) {
    val appColors = rememberAppColors()
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Connected!", style = MaterialTheme.typography.headlineLarge, color = appColors.success)
        Text("your-pax is ready", style = MaterialTheme.typography.bodyLarge, color = appColors.subtleText)
        Button(onClick = onContinue, shape = MaterialTheme.shapes.medium) { Text("Continue") }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, onSkip: () -> Unit) {
    val appColors = rememberAppColors()
    Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Connection Failed", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = appColors.subtleText, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onSkip) { Text("Skip") }
            Button(onClick = onRetry, shape = MaterialTheme.shapes.medium) { Text("Retry") }
        }
    }
}
