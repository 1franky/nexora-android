package com.nexora.android.ui.sat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.common.ApiException
import com.nexora.android.data.offline.WriteOutcome
import com.nexora.android.data.sat.SatCertificateResponse
import com.nexora.android.data.sat.SatRepository
import kotlinx.coroutines.launch

sealed interface SatConnectionUiState {
    data object Loading : SatConnectionUiState
    data class Error(val message: String) : SatConnectionUiState
    data object NotConnected : SatConnectionUiState
    data class Connected(val certificate: SatCertificateResponse) : SatConnectionUiState
}

/**
 * Estado/acciones de la pantalla "Conexión SAT" (A12, plan-integracion-sat.md
 * sección 9): alta de la e.firma, panel de estado, forzar sincronización
 * (incremental o por rango explícito) y eliminar la conexión.
 */
class SatConnectionViewModel(private val satRepository: SatRepository) : ViewModel() {

    var uiState by mutableStateOf<SatConnectionUiState>(SatConnectionUiState.Loading)
        private set

    // --- Formulario de alta ---
    var password by mutableStateOf("")
        private set
    var isSubmitting by mutableStateOf(false)
        private set
    var submitError by mutableStateOf<String?>(null)
        private set

    // --- Sincronizar ---
    var isSyncing by mutableStateOf(false)
        private set
    var syncMessage by mutableStateOf<String?>(null)
        private set

    // --- Eliminar conexión ---
    var isDeleting by mutableStateOf(false)
        private set
    var deleteError by mutableStateOf<String?>(null)
        private set
    var pendingDeleteConfirmation by mutableStateOf(false)
        private set

    fun onPasswordChange(value: String) {
        password = value
    }

    fun load(fallbackError: String) {
        viewModelScope.launch {
            uiState = SatConnectionUiState.Loading
            uiState = try {
                val certificate = satRepository.getCertificateStatus(fallbackError)
                if (certificate != null) SatConnectionUiState.Connected(certificate) else SatConnectionUiState.NotConnected
            } catch (e: ApiException) {
                SatConnectionUiState.Error(e.message ?: fallbackError)
            }
        }
    }

    fun connect(
        cerBytes: ByteArray,
        cerFileName: String,
        keyBytes: ByteArray,
        keyFileName: String,
        fallbackError: String,
    ) {
        isSubmitting = true
        submitError = null
        viewModelScope.launch {
            uiState = try {
                val certificate = satRepository.connectCertificate(cerBytes, cerFileName, keyBytes, keyFileName, password, fallbackError)
                isSubmitting = false
                password = ""
                SatConnectionUiState.Connected(certificate)
            } catch (e: ApiException) {
                isSubmitting = false
                submitError = e.message ?: fallbackError
                return@launch
            }
        }
    }

    fun requestDelete() {
        pendingDeleteConfirmation = true
        deleteError = null
    }

    fun cancelDelete() {
        pendingDeleteConfirmation = false
    }

    fun confirmDelete(fallbackError: String) {
        isDeleting = true
        deleteError = null
        viewModelScope.launch {
            try {
                satRepository.deleteCertificate(fallbackError)
                isDeleting = false
                pendingDeleteConfirmation = false
                syncMessage = null
                uiState = SatConnectionUiState.NotConnected
            } catch (e: ApiException) {
                isDeleting = false
                deleteError = e.message ?: fallbackError
            }
        }
    }

    /** [desde]/[hasta] nulos = sincronización incremental; ambos presentes = rango explícito (sección 6.1 del plan). */
    fun sync(desde: String?, hasta: String?, queuedMessage: String, startedMessage: String, fallbackError: String) {
        isSyncing = true
        syncMessage = null
        viewModelScope.launch {
            try {
                val outcome = satRepository.requestSync(desde, hasta, fallbackError)
                isSyncing = false
                syncMessage = when (outcome) {
                    is WriteOutcome.Applied -> startedMessage
                    WriteOutcome.Queued -> queuedMessage
                }
            } catch (e: ApiException) {
                isSyncing = false
                syncMessage = e.message ?: fallbackError
            }
        }
    }
}
