package com.nexora.android.data.auth

import com.nexora.android.data.common.apiCall
import com.nexora.android.data.user.RegisterRequest
import com.nexora.android.data.user.UsersApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Orquesta login/registro/logout. El estado "¿hay sesión?" se deriva
 * directamente de [TokenStore] (fuente única de verdad, igual que
 * AuthContext + tokenStore en nexora-web) — no hay un estado paralelo que
 * pueda desincronizarse.
 */
class AuthRepository(
    private val authApi: AuthApi,
    private val usersApi: UsersApi,
    private val tokenStore: TokenStore,
) {
    val isAuthenticated: Flow<Boolean> = tokenStore.tokens.map { it != null }

    suspend fun login(email: String, password: String, fallbackError: String) {
        val response = apiCall(fallbackError) { authApi.login(LoginRequest(email, password)) }
        tokenStore.save(TokenPair(response.accessToken, response.refreshToken))
    }

    suspend fun register(displayName: String, email: String, password: String, fallbackError: String) {
        apiCall(fallbackError) { usersApi.register(RegisterRequest(email, password, displayName)) }
        login(email, password, fallbackError)
    }

    suspend fun logout() {
        val current = tokenStore.getTokensBlocking()
        tokenStore.clear()
        if (current != null) {
            // Best-effort: si falla (sin red, ya revocado), la sesión local ya quedó cerrada igual.
            runCatching { authApi.logout(RefreshRequest(current.refreshToken)) }
        }
    }
}
