package com.nexora.android.data.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Rota el refresh token automáticamente cuando una petición responde 401
 * (mismo esquema que el cliente axios de nexora-web). [refreshApi] se
 * construye con un OkHttpClient propio, sin este Authenticator ni el
 * AuthInterceptor, para no arriesgar una llamada circular al refrescar.
 */
class AuthAuthenticator(
    private val tokenStore: TokenStore,
    private val refreshApi: AuthApi,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Ya se reintentó una vez con un token nuevo y igual falló: no insistir más.
        if (responseCount(response) >= 2) return null

        val currentRefreshToken = tokenStore.getTokensBlocking()?.refreshToken ?: return null

        val newTokens = try {
            runBlocking { refreshApi.refresh(RefreshRequest(currentRefreshToken)) }
        } catch (_: Exception) {
            null
        }

        if (newTokens == null) {
            // El refresh token ya no sirve (expiró o fue revocado): cierra la sesión local.
            tokenStore.clearBlocking()
            return null
        }

        tokenStore.saveBlocking(TokenPair(newTokens.accessToken, newTokens.refreshToken))

        return response.request.newBuilder()
            .header("Authorization", "Bearer ${newTokens.accessToken}")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
