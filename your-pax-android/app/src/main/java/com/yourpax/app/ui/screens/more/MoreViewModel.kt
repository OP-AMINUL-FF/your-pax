package com.yourpax.app.ui.screens.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourpax.app.data.api.models.ConfigData
import com.yourpax.app.data.repository.ConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MoreViewModel : ViewModel() {

    private val configRepo = ConfigRepository()

    private val _config = MutableStateFlow<ConfigData?>(null)
    val config: StateFlow<ConfigData?> = _config

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadConfig()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            _config.value = configRepo.loadConfig().getOrNull()
            _isLoading.value = false
        }
    }

    fun saveConfig(updates: Map<String, Any>) {
        viewModelScope.launch { configRepo.saveConfig(updates) }
    }

    fun reboot() {
        viewModelScope.launch { configRepo.reboot() }
    }

    fun shutdown() {
        viewModelScope.launch { configRepo.shutdown() }
    }
}
