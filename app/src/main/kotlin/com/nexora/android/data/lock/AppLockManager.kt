package com.nexora.android.data.lock

import android.content.Context
import android.os.SystemClock
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

private val Context.lockDataStore by preferencesDataStore(name = "nexora_lock")
private val LOCK_ENABLED_KEY = booleanPreferencesKey("lock_enabled")

/**
 * Margen de gracia (plan.md, sección 13.2): al volver de segundo plano antes
 * de que pase este tiempo, no se vuelve a pedir huella. Decisión confirmada
 * con el usuario el 2026-09-02 (alternativa descartada: bloqueo inmediato).
 */
private const val GRACE_PERIOD_MS = 30_000L

/**
 * Bloqueo de la app encima de la sesión ya persistida (plan.md, sección 13,
 * A10) — "logueado" (AuthRepository.isAuthenticated) y "desbloqueado" son dos
 * estados distintos, igual que en una app bancaria.
 *
 * - [lockEnabled]: preferencia del usuario, persistida en DataStore (mismo
 *   mecanismo que TokenStore/ThemePreference). Opt-in, desactivada por
 *   defecto — el usuario la activa desde Ajustes.
 * - [isUnlocked]: **solo en memoria**, nunca persistido. Arranca en `false`
 *   en cada proceso nuevo; pasa a `true` tras un BiometricPrompt exitoso.
 *
 * Implementa DefaultLifecycleObserver para registrarse una sola vez sobre
 * ProcessLifecycleOwner a nivel de aplicación (ver NexoraApplication) — así
 * navegar entre pantallas dentro de la app no dispara falsos re-bloqueos, y
 * solo importa cuándo la app entera pasa a/vuelve de segundo plano.
 */
class AppLockManager(private val context: Context) : DefaultLifecycleObserver {

    val lockEnabled: Flow<Boolean> = context.lockDataStore.data.map { prefs -> prefs[LOCK_ENABLED_KEY] ?: false }

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var backgroundedAt: Long? = null

    suspend fun setLockEnabled(enabled: Boolean) {
        context.lockDataStore.edit { prefs -> prefs[LOCK_ENABLED_KEY] = enabled }
    }

    fun lock() {
        _isUnlocked.value = false
    }

    fun reportUnlocked() {
        _isUnlocked.value = true
    }

    /** La app pasa a segundo plano: se anota cuándo, para evaluarlo en [onStart]. */
    override fun onStop(owner: LifecycleOwner) {
        backgroundedAt = SystemClock.elapsedRealtime()
    }

    /** La app vuelve a primer plano: re-bloquea solo si pasó el margen de gracia. */
    override fun onStart(owner: LifecycleOwner) {
        val since = backgroundedAt ?: return // primer arranque del proceso, no hubo onStop previo
        backgroundedAt = null
        if (SystemClock.elapsedRealtime() - since >= GRACE_PERIOD_MS) lock()
    }
}
