package com.yourpax.app.ui.screens.splash

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SplashViewModel : ViewModel() {

    private val _connectionSteps = MutableStateFlow(listOf(
        ConnectionStep("Bluetooth", "Checking Bluetooth status…", StepStatus.PENDING),
        ConnectionStep("Scanning", "Looking for your-pax device…", StepStatus.PENDING),
        ConnectionStep("Connecting", "Establishing connection…", StepStatus.PENDING),
        ConnectionStep("Ready", "Connection established", StepStatus.PENDING)
    ))
    val connectionSteps: StateFlow<List<ConnectionStep>> = _connectionSteps

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected
}

data class ConnectionStep(
    val title: String,
    val description: String,
    val status: StepStatus
)

enum class StepStatus {
    PENDING, IN_PROGRESS, COMPLETED, ERROR
}
