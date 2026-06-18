package com.yourpax.app.ui.screens.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourpax.app.data.api.models.NetKBResponse
import com.yourpax.app.data.api.models.NetworkScanResponse
import com.yourpax.app.data.repository.NetworkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NetworkViewModel : ViewModel() {

    private val repo = NetworkRepository()

    private val _scanData = MutableStateFlow<NetworkScanResponse?>(null)
    val scanData: StateFlow<NetworkScanResponse?> = _scanData

    private val _netKBData = MutableStateFlow<NetKBResponse?>(null)
    val netKBData: StateFlow<NetKBResponse?> = _netKBData

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _scanData.value = repo.getNetworkData().getOrNull()
            _netKBData.value = repo.getNetKBData().getOrNull()
            _isLoading.value = false
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            repo.triggerScan()
            kotlinx.coroutines.delay(2000)
            loadData()
        }
    }
}
