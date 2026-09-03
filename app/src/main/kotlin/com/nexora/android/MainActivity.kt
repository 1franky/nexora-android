package com.nexora.android

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.nexora.android.ui.navigation.NexoraNavHost
import com.nexora.android.ui.theme.NexoraTheme
import com.nexora.android.ui.theme.ThemeMode

/**
 * FragmentActivity (no ComponentActivity): BiometricPrompt (A10, ver
 * data/lock/BiometricAuthenticator.kt) necesita anclarse al FragmentManager
 * de la Activity para sobrevivir a rotación/recreación mientras el prompt
 * del sistema está abierto — FragmentActivity ya extiende ComponentActivity,
 * así que activity-compose (setContent, etc.) sigue funcionando igual.
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Endurecimiento de seguridad (A10, plan.md 14.3): evita que el contenido de
        // una app financiera aparezca en capturas de pantalla o en la miniatura del
        // selector de apps recientes.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

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
