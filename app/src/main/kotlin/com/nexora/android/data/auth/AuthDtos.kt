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
