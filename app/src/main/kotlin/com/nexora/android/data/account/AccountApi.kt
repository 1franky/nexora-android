package com.nexora.android.data.account

import retrofit2.http.GET

interface AccountApi {
    @GET("accounts")
    suspend fun listAccounts(): List<Account>
}
