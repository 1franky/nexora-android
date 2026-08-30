package com.nexora.android.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.auth.AuthRepository
import com.nexora.android.data.common.ApiException
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * No navega a Dashboard por sí mismo al loguearse: solo le pide al
 * repositorio que inicie sesión. NexoraNavHost observa
 * authRepository.isAuthenticated y reacciona — un único lugar decide la
 * navegación de auth, en vez de repetir esa lógica en cada pantalla.
 */
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    fun onEmailChange(value: String) {
        uiState = uiState.copy(email = value, error = null)
    }

    fun onPasswordChange(value: String) {
        uiState = uiState.copy(password = value, error = null)
    }

    fun submit(fallbackError: String) {
        if (uiState.isLoading) return
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                authRepository.login(uiState.email.trim(), uiState.password, fallbackError)
            } catch (e: ApiException) {
                uiState = uiState.copy(error = e.message ?: fallbackError)
            } finally {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }
}
