package com.nexora.android.data.lock

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Huella + fallback a PIN/patrón/contraseña del dispositivo si la huella
 * falla o no hay biometría enrolada — Android resuelve ese fallback solo
 * (plan.md, sección 13.2), no hace falta construir una pantalla de PIN
 * propia. No usa CryptoObject: es un gate de presencia, no de cifrado (ver
 * plan.md 14.3 — atar esto a una clave de Keystore queda fuera de alcance
 * de A10).
 */
private val ALLOWED_AUTHENTICATORS = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

/**
 * ¿Tiene este dispositivo algún método de desbloqueo seguro configurado?
 * Usado para deshabilitar el toggle de bloqueo en Ajustes cuando no puede
 * funcionar (`BIOMETRIC_ERROR_NO_HARDWARE`/`NO_DEVICE_CREDENTIAL`, entre
 * otros) — no tiene sentido ofrecer la opción si no puede funcionar.
 */
fun canAuthenticateWithLock(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(ALLOWED_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

/**
 * Lanza el `BiometricPrompt` del sistema. Requiere un `FragmentActivity`
 * (MainActivity lo es, ver MainActivity.kt) porque `BiometricPrompt` se
 * ancla al `FragmentManager` de la Activity para sobrevivir a
 * recreaciones/rotación mientras el prompt está abierto.
 *
 * No se llama a `setNegativeButtonText`: al combinar BIOMETRIC_STRONG con
 * DEVICE_CREDENTIAL, Android ya provee su propio botón de cancelar y
 * prohíbe (lanza `IllegalArgumentException`) setear uno propio.
 */
fun launchBiometricPrompt(
    activity: FragmentActivity,
    title: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
) {
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
        .build()

    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // onAuthenticationFailed (huella no reconocida en un intento suelto, sin
                // cerrar el prompt) no llega aquí a propósito: el propio prompt del
                // sistema ya le indica al usuario que falló y deja reintentar sin que la
                // UI de la app tenga que hacer nada. Solo onAuthenticationError cierra el
                // prompt (cancelado, agotó intentos, etc.) y amerita mostrar un mensaje.
                onError(errString.toString())
            }
        },
    )
    prompt.authenticate(promptInfo)
}
