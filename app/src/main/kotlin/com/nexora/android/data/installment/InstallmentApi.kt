package com.nexora.android.data.installment

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface InstallmentApi {
    @POST("credit-cards/{cardId}/installment-plans")
    suspend fun createPlan(@Path("cardId") cardId: String, @Body request: CreateInstallmentPlanRequest): InstallmentPlan

    @GET("credit-cards/{cardId}/installment-plans")
    suspend fun listForCard(@Path("cardId") cardId: String): List<InstallmentPlan>

    @POST("installment-plans/{id}/installments/{installmentId}/pay")
    suspend fun payInstallment(@Path("id") id: String, @Path("installmentId") installmentId: String): InstallmentPlan
}
