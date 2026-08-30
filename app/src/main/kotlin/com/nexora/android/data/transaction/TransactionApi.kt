package com.nexora.android.data.transaction

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface TransactionApi {
    /** Sin [accountId], trae los movimientos de todas las cuentas del usuario juntos (igual que nexora-web). */
    @GET("transactions")
    suspend fun listTransactions(@Query("accountId") accountId: String? = null): List<Transaction>

    @POST("transactions")
    suspend fun createTransaction(@Header("Idempotency-Key") idempotencyKey: String, @Body request: CreateTransactionRequest): Transaction

    @POST("transfers")
    suspend fun createTransfer(@Header("Idempotency-Key") idempotencyKey: String, @Body request: CreateTransferRequest): TransferResult
}
