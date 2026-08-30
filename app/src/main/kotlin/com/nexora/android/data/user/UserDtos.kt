package com.nexora.android.data.user

import kotlinx.serialization.Serializable

/** Espejo de com.nexora.api.user.web (nexora-api). */

@Serializable
data class RegisterRequest(val email: String, val password: String, val displayName: String)

@Serializable
data class UserResponse(val id: String, val email: String, val displayName: String, val createdAt: String)
