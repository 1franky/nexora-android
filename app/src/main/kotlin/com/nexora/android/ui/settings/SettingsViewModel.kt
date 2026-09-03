package com.nexora.android.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.lock.AppLockManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsViewModel(private val appLockManager: AppLockManager) : ViewModel() {

    var lockEnabled by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            appLockManager.lockEnabled.collectLatest { lockEnabled = it }
        }
    }

    fun onLockEnabledChange(enabled: Boolean) {
        // Optimista: refleja el toggle de inmediato, DataStore confirma en segundo
        // plano (mismo patrón que ThemePreference/setThemeMode desde su pantalla).
        lockEnabled = enabled
        viewModelScope.launch { appLockManager.setLockEnabled(enabled) }
    }
}
