package com.nexora.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexora.android.R
import com.nexora.android.data.offline.ConnectivityObserver
import com.nexora.android.data.offline.PendingOperationDao
import com.nexora.android.data.offline.PendingOperationStatus
import kotlinx.coroutines.launch

/**
 * Estado de sincronización de A8, visible en toda la app autenticada (ver
 * NexoraNavHost): sin conexión, o con operaciones encoladas que no se
 * pudieron aplicar. Nada que mostrar cuando no hay nada pendiente y hay
 * conexión — no compite por atención el resto del tiempo.
 */
@Composable
fun OfflineBanner(connectivityObserver: ConnectivityObserver, pendingOperationDao: PendingOperationDao) {
    val isOnline by connectivityObserver.isOnline.collectAsStateWithLifecycle()
    val pending by pendingOperationDao.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    val failedCount = pending.count { it.status == PendingOperationStatus.FAILED.name }
    val pendingCount = pending.size - failedCount

    val message = when {
        !isOnline -> stringResource(R.string.sync_offline_banner)
        failedCount > 0 -> stringResource(R.string.sync_failed_banner, failedCount)
        pendingCount > 0 -> stringResource(R.string.sync_pending_banner, pendingCount)
        else -> null
    } ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (failedCount > 0) {
            TextButton(onClick = { scope.launch { pendingOperationDao.deleteFailed() } }) {
                Text(stringResource(R.string.sync_discard_failed))
            }
        }
    }
}
