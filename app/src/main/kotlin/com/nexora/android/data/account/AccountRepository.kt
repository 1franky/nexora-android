package com.nexora.android.data.account

import com.nexora.android.data.offline.OfflineCache
import com.nexora.android.data.offline.OperationType
import com.nexora.android.data.offline.PendingOperationDao
import com.nexora.android.data.offline.WriteOutcome
import com.nexora.android.data.offline.cachedApiCall
import com.nexora.android.data.offline.writeCall
import com.nexora.android.sync.SyncScheduler
import kotlinx.serialization.json.Json

private const val CACHE_KEY_ACCOUNTS = "accounts"

class AccountRepository(
    private val accountApi: AccountApi,
    private val offlineCache: OfflineCache,
    private val pendingOperationDao: PendingOperationDao,
    private val syncScheduler: SyncScheduler,
    private val json: Json,
) {
    suspend fun listAccounts(fallbackError: String): List<Account> =
        cachedApiCall(offlineCache, CACHE_KEY_ACCOUNTS, fallbackError) { accountApi.listAccounts() }

    suspend fun createAccount(request: CreateAccountRequest, fallbackError: String): WriteOutcome<Account> {
        val outcome = writeCall(
            fallbackMessage = fallbackError,
            pendingOperationDao = pendingOperationDao,
            json = json,
            type = OperationType.CREATE_ACCOUNT,
            summary = "Cuenta nueva: ${request.name}",
            payload = request,
        ) { key -> accountApi.createAccount(key, request) }
        if (outcome is WriteOutcome.Queued) syncScheduler.requestSync()
        return outcome
    }
}
