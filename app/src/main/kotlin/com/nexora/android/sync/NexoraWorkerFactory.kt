package com.nexora.android.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.nexora.android.di.AppContainer

/**
 * Sin Hilt (ver AppContainer), WorkManager no puede inyectar el
 * constructor de [SyncWorker] por su cuenta — esta factory es el
 * equivalente manual: construye el Worker con las mismas dependencias que
 * ya viven en [AppContainer]. Se registra en NexoraApplication vía
 * Configuration.Provider.
 */
class NexoraWorkerFactory(private val container: AppContainer) : WorkerFactory() {

    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? =
        when (workerClassName) {
            SyncWorker::class.java.name -> SyncWorker(
                appContext,
                workerParameters,
                container.pendingOperationDao,
                OperationDispatcher(
                    container.json,
                    container.accountApi,
                    container.creditCardApi,
                    container.transactionApi,
                    container.installmentApi,
                    container.satApi,
                ),
            )
            else -> null
        }
}
