package com.nexora.android.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.authDataStore by preferencesDataStore(name = "nexora_auth")
private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")

data class TokenPair(val accessToken: String, val refreshToken: String)

/**
 * Persiste el par de tokens (access corto + refresh que se rota en cada
 * uso — mismo esquema que nexora-web, ver TokenResponse en nexora-api B7).
 * Expone tanto una versión suspend (para repos/ViewModels) como una
 * bloqueante (`getTokensBlocking`/`saveBlocking`) para poder leerse/
 * escribirse desde el Interceptor/Authenticator de OkHttp, que no son
 * suspend — corren en el pool de hilos de red de OkHttp, así que un read
 * corto de DataStore ahí es aceptable (mismo patrón usado en los codelabs
 * oficiales de Android para este caso exacto).
 *
 * Cifrado en reposo (A10, plan.md 14.3): los valores se cifran con
 * [TokenCipher] (AES-256-GCM, clave de Android Keystore) antes de escribirse
 * en DataStore — el archivo en disco solo contiene blobs Base64, nunca los
 * tokens en texto plano. Compatibilidad hacia atrás: si un valor guardado
 * por una versión anterior de la app (texto plano, sin cifrar) no puede
 * descifrarse, se usa tal cual en vez de cerrar la sesión de golpe — queda
 * re-cifrado solo, sin migración explícita, en el próximo `save()` (login o
 * rotación de refresh token, que ocurre seguido).
 */
class TokenStore(
    private val context: Context,
    private val cipher: TokenCipher = TokenCipher(),
) {

    val tokens: Flow<TokenPair?> = context.authDataStore.data.map { prefs ->
        val access = prefs[ACCESS_TOKEN_KEY]?.let(::decryptOrLegacy)
        val refresh = prefs[REFRESH_TOKEN_KEY]?.let(::decryptOrLegacy)
        if (access != null && refresh != null) TokenPair(access, refresh) else null
    }

    suspend fun save(tokens: TokenPair) {
        context.authDataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = cipher.encrypt(tokens.accessToken)
            prefs[REFRESH_TOKEN_KEY] = cipher.encrypt(tokens.refreshToken)
        }
    }

    suspend fun clear() {
        context.authDataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN_KEY)
            prefs.remove(REFRESH_TOKEN_KEY)
        }
    }

    fun getTokensBlocking(): TokenPair? = runBlocking { tokens.firstOrNull() }

    fun saveBlocking(tokens: TokenPair) = runBlocking { save(tokens) }

    fun clearBlocking() = runBlocking { clear() }

    private fun decryptOrLegacy(stored: String): String = cipher.decrypt(stored) ?: stored
}
