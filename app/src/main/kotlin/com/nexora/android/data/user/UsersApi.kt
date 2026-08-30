package com.nexora.android.data.user

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface UsersApi {
    @POST("users")
    suspend fun register(@Body request: RegisterRequest): UserResponse

    @GET("users/me")
    suspend fun getCurrentUser(): UserResponse
}
