package com.yourpax.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourpax.app.data.api.models.StoreDataFull
import com.yourpax.app.data.api.models.WiFiStatusResponse
import com.yourpax.app.data.repository.LootRepository
import com.yourpax.app.data.repository.NetworkRepository
import com.yourpax.app.data.repository.WiFiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val networkRepo = NetworkRepository()
    private val lootRepo = LootRepository()
    private val wifiRepo = WiFiRepository()

    private val _storeData = MutableStateFlow<StoreDataFull?>(null)
    val storeData: StateFlow<StoreDataFull?> = _storeData

    private val _wifiStatus = MutableStateFlow<WiFiStatusResponse?>(null)
    val wifiStatus: StateFlow<WiFiStatusResponse?> = _wifiStatus

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _storeData.value = lootRepo.getStoreData().getOrNull()
            _wifiStatus.value = wifiRepo.getWifiStatus().getOrNull()
            _isLoading.value = false
        }
    }

    fun triggerAction(action: suspend () -> Result<*>, label: String) {
        viewModelScope.launch {
            _statusMessage.value = "Executing $label…"
            try {
                val result = action()
                _statusMessage.value = if (result.isSuccess) "$label started" else "Failed"
            } catch (e: Exception) {
                _statusMessage.value = "$label failed: ${e.message}"
            }
        }
    }
}
