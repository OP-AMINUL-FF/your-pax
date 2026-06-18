package com.yourpax.app.ui.screens.loot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourpax.app.data.api.models.CredentialFile
import com.yourpax.app.data.api.models.LootFile
import com.yourpax.app.data.api.models.StoreDataFull
import com.yourpax.app.data.repository.LootRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LootViewModel : ViewModel() {

    private val repo = LootRepository()

    private val _credentials = MutableStateFlow<List<CredentialFile>>(emptyList())
    val credentials: StateFlow<List<CredentialFile>> = _credentials

    private val _lootFiles = MutableStateFlow<List<LootFile>>(emptyList())
    val lootFiles: StateFlow<List<LootFile>> = _lootFiles

    private val _storeData = MutableStateFlow<StoreDataFull?>(null)
    val storeData: StateFlow<StoreDataFull?> = _storeData

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _credentials.value = repo.getCredentials().getOrDefault(emptyList())
            _lootFiles.value = repo.getLootFiles().getOrDefault(emptyList())
            _storeData.value = repo.getStoreData().getOrNull()
            _isLoading.value = false
        }
    }

    fun downloadFile(path: String) {
        viewModelScope.launch {
            repo.downloadFile(path)
        }
    }
}
