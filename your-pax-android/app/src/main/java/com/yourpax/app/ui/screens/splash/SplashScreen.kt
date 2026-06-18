package com.yourpax.app.ui.screens.splash

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.PermissionChecker
import com.yourpax.app.data.api.CsrfTokenManager
import com.yourpax.app.data.api.RetrofitProvider
import com.yourpax.app.data.bluetooth.BluetoothConnectionState
import com.yourpax.app.data.bluetooth.BluetoothScanner
import com.yourpax.app.ui.theme.*
import kotlinx.coroutines.launch

private fun requiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}

@Composable
fun SplashScreen(
    onConnected: () -> Unit,
    onSkip: () -> Unit
) {
    var connectionState by remember { mutableStateOf<BluetoothConnectionState>(BluetoothConnectionState.Idle) }
    val bluetoothAdapter = remember { BluetoothAdapter.getDefaultAdapter() }
    val scanner = remember { BluetoothScanner(bluetoothAdapter) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun startScanAndConnect() {
        connectionState = BluetoothConnectionState.Scanning
        scanner.startScan(onFound = { connectionState = BluetoothConnectionState.Connecting }, onFinished = {
            val device = scanner.findYourPaxDevice()
            if (device != null) {
                connectionState = BluetoothConnectionState.Connected("192.168.4.1")
                scope.launch {
                    try {
                        val api = RetrofitProvider.getApiService()
                        val resp = api.getCsrfToken()
                        if (resp.isSuccessful) {
                            CsrfTokenManager.token = resp.body()?.csrf_token ?: ""
                        }
                    } catch (_: Exception) {}
                    onConnected()
                }
            } else {
                connectionState = BluetoothConnectionState.Error("your-pax not found.\nMake sure it is powered on.")
            }
        })
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startScanAndConnect()
        } else {
            connectionState = BluetoothConnectionState.Error("Bluetooth permissions are required to connect.")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(BackgroundDark, BackgroundDark.copy(alpha = 0.95f), SurfaceDark))),
        contentAlignment = Alignment.Center
    ) {
        when (val state = connectionState) {
            is BluetoothConnectionState.Idle -> SplashContent(onConnect = {
                if (bluetoothAdapter == null) connectionState = BluetoothConnectionState.Error("Bluetooth not supported")
                else if (!bluetoothAdapter.isEnabled) connectionState = BluetoothConnectionState.BluetoothOff
                else {
                    val allGranted = requiredPermissions().all {
                        PermissionChecker.checkSelfPermission(context, it) == PermissionChecker.PERMISSION_GRANTED
                    }
                    if (allGranted) {
                        startScanAndConnect()
                    } else {
                        permissionLauncher.launch(requiredPermissions())
                    }
                }
            }, onSkip = onSkip)

            is BluetoothConnectionState.CheckingBluetooth -> CheckingContent()
            is BluetoothConnectionState.BluetoothOff -> BluetoothOffContent(onEnable = {
                bluetoothAdapter?.enable()
                startScanAndConnect()
            }, onSkip = onSkip)

            is BluetoothConnectionState.Scanning -> ScanningContent()
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
    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("YOUR-PAX", style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold, fontSize = 40.sp), color = Primary)
        Text("Your Pocket Security Companion", style = MaterialTheme.typography.bodyLarge, color = SubtleText, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onConnect, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(26.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("Connect via Bluetooth", fontWeight = FontWeight.SemiBold) }
        OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = SubtleText)) { Text("Skip for now") }
    }
}

@Composable
private fun CheckingContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator(color = Primary, modifier = Modifier.size(48.dp))
        Text("Checking Bluetooth\u2026", style = MaterialTheme.typography.bodyLarge, color = SubtleText)
    }
}

@Composable
private fun BluetoothOffContent(onEnable: () -> Unit, onSkip: () -> Unit) {
    Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Bluetooth is Off", style = MaterialTheme.typography.headlineMedium, color = Warning)
        Text("your-pax needs Bluetooth to discover and connect.", style = MaterialTheme.typography.bodyMedium, color = SubtleText, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onEnable, shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("Enable Bluetooth") }
        TextButton(onClick = onSkip) { Text("Skip", color = SubtleText) }
    }
}

@Composable
private fun ScanningContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator(color = Primary, modifier = Modifier.size(48.dp))
        Text("Searching for your-pax device\u2026", style = MaterialTheme.typography.bodyLarge, color = SubtleText)
        Text("Make sure your-pax is powered on.", style = MaterialTheme.typography.bodySmall, color = SubtleText.copy(alpha = 0.6f))
    }
}

@Composable
private fun ConnectingContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator(color = Success, modifier = Modifier.size(48.dp))
        Text("Connecting\u2026", style = MaterialTheme.typography.bodyLarge, color = Success)
    }
}

@Composable
private fun TestingContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator(color = Info, modifier = Modifier.size(48.dp))
        Text("Testing connection\u2026", style = MaterialTheme.typography.bodyLarge, color = Info)
    }
}

@Composable
private fun ConnectedContent(onContinue: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Connected!", style = MaterialTheme.typography.headlineLarge, color = Success)
        Text("your-pax is ready", style = MaterialTheme.typography.bodyLarge, color = SubtleText)
        Button(onClick = onContinue, shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("Continue") }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, onSkip: () -> Unit) {
    Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Connection Failed", style = MaterialTheme.typography.headlineMedium, color = Error)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = SubtleText, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onSkip) { Text("Skip") }
            Button(onClick = onRetry, shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("Retry") }
        }
    }
}
