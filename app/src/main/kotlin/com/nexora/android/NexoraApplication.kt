package com.nexora.android

import android.app.Application
import androidx.work.Configuration
import com.nexora.android.di.AppContainer
import com.nexora.android.sync.NexoraWorkerFactory

class NexoraApplication : Application(), Configuration.Provider {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    // WorkManager no tiene Hilt (ver AppContainer) para inyectar SyncWorker por su cuenta;
    // este Configuration.Provider es lo que hace que use NexoraWorkerFactory en su lugar.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(NexoraWorkerFactory(container))
            .build()
}
