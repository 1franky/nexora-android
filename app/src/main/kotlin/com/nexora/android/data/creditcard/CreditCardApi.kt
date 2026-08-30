package com.nexora.android.data.creditcard

import com.nexora.android.data.transaction.Transaction
import com.nexora.android.data.transaction.TransferResult
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface CreditCardApi {
    @GET("credit-cards")
    suspend fun listCreditCards(): List<CreditCard>

    @POST("credit-cards")
    suspend fun createCreditCard(@Header("Idempotency-Key") idempotencyKey: String, @Body request: CreateCreditCardRequest): CreditCard

    @GET("credit-cards/{id}")
    suspend fun getCreditCard(@Path("id") id: String): CreditCard

    @POST("credit-cards/{id}/purchases")
    suspend fun purchase(
        @Path("id") id: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CreditCardPurchaseRequest,
    ): Transaction

    @POST("credit-cards/{id}/payments")
    suspend fun pay(
        @Path("id") id: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CreditCardPaymentRequest,
    ): TransferResult
}
