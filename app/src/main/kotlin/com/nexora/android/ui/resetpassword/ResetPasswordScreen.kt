package com.nexora.android.ui.resetpassword

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nexora.android.R
import com.nexora.android.data.auth.AuthRepository
import com.nexora.android.ui.login.nexoraFieldColors

/**
 * Segundo paso de A11: código de 6 dígitos + contraseña nueva + confirmar.
 * Éxito -> [onNavigateToLogin], que en NexoraNavHost limpia el back stack
 * (mismo patrón que el logout: `popUpTo(0) { inclusive = true }`) — el
 * usuario vuelve a loguearse con la contraseña nueva, ya que el backend
 * revocó todas sus sesiones activas.
 */
@Composable
fun ResetPasswordScreen(
    authRepository: AuthRepository,
    initialEmail: String,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val viewModel: ResetPasswordViewModel = viewModel(
        factory = viewModelFactory { initializer { ResetPasswordViewModel(authRepository, initialEmail) } },
    )
    val uiState = viewModel.uiState
    val fallbackError = stringResource(R.string.login_error_generic)
    val passwordMismatchError = stringResource(R.string.reset_password_mismatch)
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        IconButton(onClick = onNavigateBack, modifier = Modifier.padding(bottom = 8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
        }

        Text(stringResource(R.string.reset_password_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            text = stringResource(R.string.reset_password_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text(stringResource(R.string.login_email_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = nexoraFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.code,
                onValueChange = viewModel::onCodeChange,
                label = { Text(stringResource(R.string.reset_password_code_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                colors = nexoraFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.newPassword,
                onValueChange = viewModel::onNewPasswordChange,
                label = { Text(stringResource(R.string.reset_password_new_password_label)) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val description = stringResource(
                        if (passwordVisible) R.string.password_toggle_hide else R.string.password_toggle_show,
                    )
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = description,
                        )
                    }
                },
                colors = nexoraFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = { Text(stringResource(R.string.reset_password_confirm_password_label)) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = nexoraFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            if (uiState.error != null) {
                Text(
                    text = uiState.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(
                onClick = { viewModel.submit(fallbackError, passwordMismatchError, onSuccess = onNavigateToLogin) },
                enabled = uiState.isSubmittable,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.reset_password_submit), style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}
