package com.yourpax.app.ui.screens.wifi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourpax.app.data.api.models.AttackStatusResponse
import com.yourpax.app.data.api.models.WiFiNetwork
import com.yourpax.app.data.repository.WiFiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WiFiViewModel : ViewModel() {

    private val repo = WiFiRepository()

    private val _networks = MutableStateFlow<List<WiFiNetwork>>(emptyList())
    val networks: StateFlow<List<WiFiNetwork>> = _networks

    private val _handshakeStatus = MutableStateFlow(AttackStatusResponse())
    val handshakeStatus: StateFlow<AttackStatusResponse> = _handshakeStatus

    private val _pmkidStatus = MutableStateFlow(AttackStatusResponse())
    val pmkidStatus: StateFlow<AttackStatusResponse> = _pmkidStatus

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadNetworks()
    }

    private fun loadNetworks() {
        viewModelScope.launch {
            _networks.value = repo.scanNetworks().getOrDefault(emptyList())
            _handshakeStatus.value = repo.getHandshakeStatus().getOrNull() ?: AttackStatusResponse()
            _pmkidStatus.value = repo.getPmkidStatus().getOrNull() ?: AttackStatusResponse()
            _isLoading.value = false
        }
    }

    fun startHandshake(bssid: String, channel: String, prefix: String) {
        viewModelScope.launch { repo.startHandshake(bssid, channel, prefix) }
    }

    fun stopHandshake() {
        viewModelScope.launch { repo.stopHandshake() }
    }

    fun startPmkid(bssid: String, channel: String, prefix: String) {
        viewModelScope.launch { repo.startPmkid(bssid, channel, prefix) }
    }

    fun stopPmkid() {
        viewModelScope.launch { repo.stopPmkid() }
    }

    fun deauth(bssid: String, client: String, count: Int, channel: Int) {
        viewModelScope.launch { repo.deauth(bssid, client, count, channel) }
    }
}
