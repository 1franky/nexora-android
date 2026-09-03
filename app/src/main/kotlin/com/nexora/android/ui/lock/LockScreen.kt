package com.nexora.android.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.nexora.android.R
import com.nexora.android.data.lock.AppLockManager
import com.nexora.android.data.lock.launchBiometricPrompt
import com.nexora.android.ui.theme.NexoraExtendedTheme

/**
 * Pantalla de bloqueo (plan.md, sección 13, A10). NexoraNavHost la superpone
 * encima del NavHost normal (que se mantiene montado debajo) cuando
 * `lockEnabled && !isUnlocked` — no es una ruta más del back stack (evita que
 * un `popBackStack` la esquive), así que no recibe `onNavigateBack`; la única
 * salida es desbloquear o cerrar sesión. Pinta un fondo sólido (no
 * transparente) a propósito: es lo que oculta el contenido financiero de
 * debajo mientras la app está bloqueada.
 */
@Composable
fun LockScreen(
    appLockManager: AppLockManager,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    val promptTitle = stringResource(R.string.lock_prompt_title)
    val genericError = stringResource(R.string.lock_error_generic)

    val unlock: () -> Unit = unlock@{
        error = null
        val activity = context as? FragmentActivity ?: return@unlock
        launchBiometricPrompt(
            activity = activity,
            title = promptTitle,
            onSuccess = appLockManager::reportUnlocked,
            onError = { message -> error = message.ifBlank { genericError } },
        )
    }

    // Dispara el prompt automáticamente al entrar a la pantalla (igual que una app
    // bancaria) — el botón "Desbloquear" de abajo queda como reintento manual tras
    // un error o cancelación.
    LaunchedEffect(Unit) { unlock() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(NexoraExtendedTheme.colors.accentContainer, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(30.dp),
            )
        }

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 18.dp),
        )
        Text(
            text = stringResource(R.string.lock_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )

        if (error != null) {
            Text(
                text = error!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 20.dp),
            )
        }

        Button(
            onClick = unlock,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(top = 28.dp),
        ) {
            Text(stringResource(R.string.lock_unlock_button), style = MaterialTheme.typography.titleSmall)
        }

        // Escape hatch (plan.md 14.2): necesaria si el dispositivo cambió de dueño
        // temporal o el usuario no puede autenticarse por biometría — cierra sesión de
        // verdad en vez de dejarlo atascado en esta pantalla.
        TextButton(onClick = onLogout, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.logout), style = MaterialTheme.typography.labelLarge)
        }
    }
}
