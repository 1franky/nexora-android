package com.nexora.android.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nexora.android.R
import com.nexora.android.data.lock.AppLockManager
import com.nexora.android.data.lock.canAuthenticateWithLock
import com.nexora.android.data.lock.launchBiometricPrompt

/**
 * Ajustes (A10, plan.md sección 13): pantalla nueva y mínima — por ahora solo
 * el toggle de bloqueo con huella, no una sección de Ajustes completa (fuera
 * de alcance de esta fase).
 */
@Composable
fun SettingsScreen(
    appLockManager: AppLockManager,
    onNavigateBack: () -> Unit,
    onNavigateToSat: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory { initializer { SettingsViewModel(appLockManager) } },
    )
    // No cambia mientras la Activity vive: no hace falta re-evaluarlo en cada recomposición.
    val biometricAvailable = remember { canAuthenticateWithLock(context) }

    // A12: entrar a "Conexión SAT" exige un BiometricPrompt fresco, independiente de si
    // el bloqueo general de la app (A10, el switch de abajo) está activado — la
    // información fiscal es sensible de por sí. No usa AppLockManager.isUnlocked (eso es
    // el estado del bloqueo de TODA la app): es un gate local a esta navegación puntual.
    var satGateError by remember { mutableStateOf<String?>(null) }
    var showSatUnavailableDialog by remember { mutableStateOf(false) }
    val satPromptTitle = stringResource(R.string.sat_gate_prompt_title)
    val genericLockError = stringResource(R.string.lock_error_generic)

    fun requestSatAccess() {
        satGateError = null
        if (!canAuthenticateWithLock(context)) {
            showSatUnavailableDialog = true
            return
        }
        val activity = context as? FragmentActivity ?: return
        launchBiometricPrompt(
            activity = activity,
            title = satPromptTitle,
            onSuccess = onNavigateToSat,
            onError = { message -> satGateError = message.ifBlank { genericLockError } },
        )
    }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(stringResource(R.string.settings_lock_toggle), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(
                        if (biometricAvailable) R.string.settings_lock_toggle_hint else R.string.settings_lock_unavailable,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = viewModel.lockEnabled,
                onCheckedChange = viewModel::onLockEnabledChange,
                enabled = biometricAvailable,
            )
        }

        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = ::requestSatAccess)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(stringResource(R.string.sat_menu_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.sat_menu_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (satGateError != null) {
                    Text(satGateError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }

    if (showSatUnavailableDialog) {
        AlertDialog(
            onDismissRequest = { showSatUnavailableDialog = false },
            title = { Text(stringResource(R.string.sat_gate_unavailable_title)) },
            text = { Text(stringResource(R.string.sat_gate_unavailable_message)) },
            confirmButton = {
                TextButton(onClick = { showSatUnavailableDialog = false }) {
                    Text(stringResource(R.string.sat_gate_unavailable_confirm))
                }
            },
        )
    }
}
