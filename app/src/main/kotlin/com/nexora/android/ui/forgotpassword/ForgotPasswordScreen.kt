package com.nexora.android.ui.forgotpassword

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
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nexora.android.R
import com.nexora.android.data.auth.AuthRepository
import com.nexora.android.ui.login.nexoraFieldColors

/**
 * Primer paso de A11 (plan-recuperacion-password.md sección 9.1: dos
 * pantallas, no un solo formulario): solo pide el email. Tras un submit
 * exitoso muestra un mensaje genérico fijo — nunca lo que devolvió el
 * backend, que responde 200 vacío exista o no la cuenta a propósito, para no
 * revelar si un email está registrado.
 */
@Composable
fun ForgotPasswordScreen(
    authRepository: AuthRepository,
    onNavigateBack: () -> Unit,
    onNavigateToResetPassword: (email: String) -> Unit,
) {
    val viewModel: ForgotPasswordViewModel = viewModel(
        factory = viewModelFactory { initializer { ForgotPasswordViewModel(authRepository) } },
    )
    val uiState = viewModel.uiState
    val fallbackError = stringResource(R.string.login_error_generic)

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

        if (uiState.submitted) {
            Icon(
                imageVector = Icons.Filled.MarkEmailRead,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(stringResource(R.string.forgot_password_sent_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = stringResource(R.string.forgot_password_sent_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(
                onClick = { onNavigateToResetPassword(uiState.email.trim()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(top = 28.dp),
            ) {
                Text(stringResource(R.string.forgot_password_continue), style = MaterialTheme.typography.titleSmall)
            }
        } else {
            Text(stringResource(R.string.forgot_password_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = stringResource(R.string.forgot_password_subtitle),
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

                if (uiState.error != null) {
                    Text(
                        text = uiState.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Button(
                    onClick = { viewModel.submit(fallbackError) },
                    enabled = !uiState.isLoading && uiState.email.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(stringResource(R.string.forgot_password_submit), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}
