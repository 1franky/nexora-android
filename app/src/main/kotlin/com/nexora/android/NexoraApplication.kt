package com.nexora.android

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import androidx.work.WorkManager
import com.nexora.android.di.AppContainer
import com.nexora.android.sync.NexoraWorkerFactory

class NexoraApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // A10: AppLockManager observa el ciclo de vida del PROCESO (no de una Activity
        // puntual) para que cambiar de pantalla dentro de la app no dispare falsos
        // re-bloqueos — solo importa cuándo la app entera pasa a/vuelve de background.
        ProcessLifecycleOwner.get().lifecycle.addObserver(container.appLockManager)

        // Inicialización manual (no Configuration.Provider): WorkManager no tiene Hilt
        // (ver AppContainer) para inyectar SyncWorker por su cuenta, y necesita a
        // `container` ya construido para armar NexoraWorkerFactory — con
        // Configuration.Provider eso es un ciclo (WorkManager se auto-inicializaría
        // antes de que exista `container`). El orden aquí es lo que lo evita.
        WorkManager.initialize(this, Configuration.Builder().setWorkerFactory(NexoraWorkerFactory(container)).build())
        container.startSync()
    }
}
