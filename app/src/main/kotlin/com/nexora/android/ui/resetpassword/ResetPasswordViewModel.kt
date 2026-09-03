package com.nexora.android.ui.resetpassword

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.auth.AuthRepository
import com.nexora.android.data.common.ApiException
import kotlinx.coroutines.launch

data class ResetPasswordUiState(
    val email: String = "",
    val code: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    /**
     * Habilita el botón "Restablecer": espeja las validaciones que el backend igual aplica
     * (código de 6 dígitos, contraseña de al menos 8 caracteres — ver
     * plan-recuperacion-password.md sección 5.5), más la confirmación de contraseña, que es
     * puramente de UX — el backend ni siquiera recibe `confirmPassword`. Extraída como
     * propiedad pura (sin tocar el ViewModel) para poder testearla sin mockear
     * [AuthRepository].
     */
    val isSubmittable: Boolean
        get() = !isLoading &&
            email.isNotBlank() &&
            code.length == 6 &&
            code.all(Char::isDigit) &&
            newPassword.length >= 8 &&
            confirmPassword == newPassword
}

/**
 * Segundo paso de A11. `initialEmail` llega prellenado desde
 * [com.nexora.android.ui.forgotpassword.ForgotPasswordScreen] (argumento de
 * navegación opcional), pero sigue siendo editable acá.
 */
class ResetPasswordViewModel(
    private val authRepository: AuthRepository,
    initialEmail: String,
) : ViewModel() {

    var uiState by mutableStateOf(ResetPasswordUiState(email = initialEmail))
        private set

    fun onEmailChange(value: String) {
        uiState = uiState.copy(email = value, error = null)
    }

    fun onCodeChange(value: String) {
        // Solo dígitos, máximo 6 — el backend igual valida el formato, pero esto evita que
        // el usuario pegue espacios/guiones del correo sin darse cuenta.
        uiState = uiState.copy(code = value.filter(Char::isDigit).take(6), error = null)
    }

    fun onNewPasswordChange(value: String) {
        uiState = uiState.copy(newPassword = value, error = null)
    }

    fun onConfirmPasswordChange(value: String) {
        uiState = uiState.copy(confirmPassword = value, error = null)
    }

    fun submit(fallbackError: String, passwordMismatchError: String, onSuccess: () -> Unit) {
        if (uiState.isLoading) return
        // Validación de cliente antes de llamar a la API (plan.md tarea 5): coincidencia de
        // contraseñas, el backend nunca la ve.
        if (uiState.newPassword != uiState.confirmPassword) {
            uiState = uiState.copy(error = passwordMismatchError)
            return
        }
        uiState = uiState.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                authRepository.resetPassword(uiState.email.trim(), uiState.code, uiState.newPassword, fallbackError)
                onSuccess()
            } catch (e: ApiException) {
                // Mensaje ya genérico desde el backend ("Código inválido o expirado.") —
                // no distingue código incorrecto, expirado o intentos agotados, a propósito.
                uiState = uiState.copy(error = e.message ?: fallbackError)
            } finally {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }
}
