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

    /**
     * A11: siempre 200 con cuerpo vacío, exista o no una cuenta con ese email (el backend
     * nunca revela si existe, ver plan-recuperacion-password.md sección 3.7) — por eso no
     * hay nada que devolver ni leer de la respuesta. No toca [tokenStore]: no hay sesión
     * involucrada todavía.
     */
    suspend fun forgotPassword(email: String, fallbackError: String) {
        apiCall(fallbackError) { authApi.forgotPassword(ForgotPasswordRequest(email)) }
    }

    /**
     * A11: éxito 204, error 401 con mensaje genérico ("Código inválido o expirado.") que ya
     * llega en español desde el backend — [apiCall] lo propaga tal cual. No toca
     * [tokenStore]: el backend revoca todas las sesiones activas del usuario al resetear, así
     * que la app no guarda tokens acá; el usuario vuelve a loguearse normal.
     */
    suspend fun resetPassword(email: String, code: String, newPassword: String, fallbackError: String) {
        apiCall(fallbackError) { authApi.resetPassword(ResetPasswordRequest(email, code, newPassword)) }
    }
}
