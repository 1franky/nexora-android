package com.nexora.android.data.account

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AccountApi {
    @GET("accounts")
    suspend fun listAccounts(): List<Account>

    @POST("accounts")
    suspend fun createAccount(@Body request: CreateAccountRequest): Account
}
