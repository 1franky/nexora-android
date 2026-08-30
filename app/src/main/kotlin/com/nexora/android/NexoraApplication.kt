package com.nexora.android

import android.app.Application
import com.nexora.android.di.AppContainer

class NexoraApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
