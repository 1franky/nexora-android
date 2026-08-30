package com.nexora.android.data.transaction

import com.nexora.android.data.common.apiCall

class TransactionRepository(private val transactionApi: TransactionApi) {

    suspend fun listTransactions(accountId: String?, fallbackError: String): List<Transaction> =
        apiCall(fallbackError) { transactionApi.listTransactions(accountId) }

    suspend fun createTransaction(request: CreateTransactionRequest, fallbackError: String): Transaction =
        apiCall(fallbackError) { transactionApi.createTransaction(request) }

    suspend fun createTransfer(request: CreateTransferRequest, fallbackError: String): TransferResult =
        apiCall(fallbackError) { transactionApi.createTransfer(request) }
}
