package com.nexora.android.ui.forgotpassword

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.auth.AuthRepository
import com.nexora.android.data.common.ApiException
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    // Tras un submit exitoso: el backend responde 200 vacío exista o no la cuenta (nunca
    // revela cuál fue el caso), así que este flag solo dispara el mensaje genérico fijo de
    // la pantalla — no depende de nada que haya devuelto la respuesta.
    val submitted: Boolean = false,
)

class ForgotPasswordViewModel(private val authRepository: AuthRepository) : ViewModel() {

    var uiState by mutableStateOf(ForgotPasswordUiState())
        private set

    fun onEmailChange(value: String) {
        uiState = uiState.copy(email = value, error = null)
    }

    fun submit(fallbackError: String) {
        if (uiState.isLoading) return
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                authRepository.forgotPassword(uiState.email.trim(), fallbackError)
                uiState = uiState.copy(submitted = true)
            } catch (e: ApiException) {
                // Solo llega acá por un fallo real de red/servidor (timeout, rate limit,
                // etc.) — el caso "no existe cuenta con ese email" nunca lanza, el backend
                // responde 200 igual que si existiera.
                uiState = uiState.copy(error = e.message ?: fallbackError)
            } finally {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }
}
