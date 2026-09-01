package com.nexora.android.data.transaction

import com.nexora.android.data.common.apiCall
import com.nexora.android.data.offline.OfflineCache
import com.nexora.android.data.offline.OperationType
import com.nexora.android.data.offline.PendingOperationDao
import com.nexora.android.data.offline.WriteOutcome
import com.nexora.android.data.offline.cachedApiCall
import com.nexora.android.data.offline.writeCall
import com.nexora.android.sync.SyncScheduler
import kotlinx.serialization.json.Json

private fun cacheKeyTransactions(accountId: String?) = "transactions:${accountId ?: "all"}"

class TransactionRepository(
    private val transactionApi: TransactionApi,
    private val offlineCache: OfflineCache,
    private val pendingOperationDao: PendingOperationDao,
    private val syncScheduler: SyncScheduler,
    private val json: Json,
) {

    suspend fun listTransactions(accountId: String?, fallbackError: String): List<Transaction> =
        cachedApiCall(offlineCache, cacheKeyTransactions(accountId), fallbackError) { transactionApi.listTransactions(accountId) }

    suspend fun createTransaction(request: CreateTransactionRequest, fallbackError: String): WriteOutcome<Transaction> {
        val outcome = writeCall(
            fallbackMessage = fallbackError,
            pendingOperationDao = pendingOperationDao,
            json = json,
            type = OperationType.CREATE_TRANSACTION,
            summary = "${request.type}: ${request.amount}",
            payload = request,
        ) { key -> transactionApi.createTransaction(key, request) }
        if (outcome is WriteOutcome.Queued) syncScheduler.requestSync()
        return outcome
    }

    /** Sin caché ni cola offline a propósito: el usuario espera ver el resultado (o el error) de inmediato. */
    suspend fun updateTransaction(id: String, request: UpdateTransactionRequest, fallbackError: String): Transaction =
        apiCall(fallbackError) { transactionApi.updateTransaction(id, request) }

    /** Sin caché ni cola offline a propósito: mismo criterio que [updateTransaction]. */
    suspend fun deleteTransaction(id: String, fallbackError: String) {
        apiCall(fallbackError) { transactionApi.deleteTransaction(id) }
    }

    suspend fun createTransfer(request: CreateTransferRequest, fallbackError: String): WriteOutcome<TransferResult> {
        val outcome = writeCall(
            fallbackMessage = fallbackError,
            pendingOperationDao = pendingOperationDao,
            json = json,
            type = OperationType.CREATE_TRANSFER,
            summary = "Transferencia: ${request.amount}",
            payload = request,
        ) { key -> transactionApi.createTransfer(key, request) }
        if (outcome is WriteOutcome.Queued) syncScheduler.requestSync()
        return outcome
    }
}
