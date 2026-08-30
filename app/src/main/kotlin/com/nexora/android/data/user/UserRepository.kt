package com.nexora.android.data.user

import com.nexora.android.data.common.apiCall

class UserRepository(private val usersApi: UsersApi) {
    suspend fun getCurrentUser(fallbackError: String): UserResponse =
        apiCall(fallbackError) { usersApi.getCurrentUser() }
}
