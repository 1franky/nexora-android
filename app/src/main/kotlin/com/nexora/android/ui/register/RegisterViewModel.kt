package com.nexora.android.ui.register

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.auth.AuthRepository
import com.nexora.android.data.common.ApiException
import kotlinx.coroutines.launch

data class RegisterUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    var uiState by mutableStateOf(RegisterUiState())
        private set

    fun onDisplayNameChange(value: String) {
        uiState = uiState.copy(displayName = value, error = null)
    }

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
                authRepository.register(uiState.displayName.trim(), uiState.email.trim(), uiState.password, fallbackError)
            } catch (e: ApiException) {
                uiState = uiState.copy(error = e.message ?: fallbackError)
            } finally {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }
}
