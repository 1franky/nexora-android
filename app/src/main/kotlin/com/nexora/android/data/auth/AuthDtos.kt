package com.nexora.android.data.auth

import kotlinx.serialization.Serializable

/** Espejo de los DTOs de com.nexora.api.auth.web (nexora-api). */

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long,
)

/** A11: cuerpo vacío (200) exista o no la cuenta — ver AuthRepository.forgotPassword. */
@Serializable
data class ForgotPasswordRequest(val email: String)

/** A11: éxito 204; error 401 con mensaje genérico ("Código inválido o expirado."). */
@Serializable
data class ResetPasswordRequest(val email: String, val code: String, val newPassword: String)
