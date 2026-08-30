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
 */
class TokenStore(private val context: Context) {

    val tokens: Flow<TokenPair?> = context.authDataStore.data.map { prefs ->
        val access = prefs[ACCESS_TOKEN_KEY]
        val refresh = prefs[REFRESH_TOKEN_KEY]
        if (access != null && refresh != null) TokenPair(access, refresh) else null
    }

    suspend fun save(tokens: TokenPair) {
        context.authDataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = tokens.accessToken
            prefs[REFRESH_TOKEN_KEY] = tokens.refreshToken
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
}
