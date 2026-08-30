package com.nexora.android.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit

private const val SYNC_NOW_WORK_NAME = "nexora-sync-now"
private const val SYNC_PERIODIC_WORK_NAME = "nexora-sync-periodic"
private const val PERIODIC_INTERVAL_MINUTES = 15L

private val syncConstraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

/**
 * Encola [SyncWorker] contra WorkManager. Dos disparadores: "ya mismo"
 * (justo después de encolar una operación, o en cuanto ConnectivityObserver
 * ve que volvió la conexión — ver AppContainer) y un respaldo periódico por
 * si ambos se pierden (ej. la app estuvo cerrada cuando volvió la señal).
 */
class SyncScheduler(private val context: Context) {

    fun requestSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(syncConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(SYNC_NOW_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(PERIODIC_INTERVAL_MINUTES, TimeUnit.MINUTES)
            .setConstraints(syncConstraints)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(SYNC_PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
