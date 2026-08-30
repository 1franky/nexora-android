package com.nexora.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.nexora.android.ui.navigation.NexoraNavHost
import com.nexora.android.ui.theme.NexoraTheme
import com.nexora.android.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as NexoraApplication).container

        setContent {
            val themeMode by container.themePreference.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

            NexoraTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NexoraNavHost(container = container)
                }
            }
        }
    }
}
