package com.nexora.android.data.account

import com.nexora.android.data.common.apiCall

class AccountRepository(private val accountApi: AccountApi) {
    suspend fun listAccounts(fallbackError: String): List<Account> =
        apiCall(fallbackError) { accountApi.listAccounts() }
}
