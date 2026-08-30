package com.nexora.android.data.installment

import com.nexora.android.data.common.apiCall

class InstallmentRepository(private val installmentApi: InstallmentApi) {
    suspend fun createPlan(cardId: String, request: CreateInstallmentPlanRequest, fallbackError: String): InstallmentPlan =
        apiCall(fallbackError) { installmentApi.createPlan(cardId, request) }

    suspend fun listForCard(cardId: String, fallbackError: String): List<InstallmentPlan> =
        apiCall(fallbackError) { installmentApi.listForCard(cardId) }

    suspend fun payInstallment(planId: String, installmentId: String, fallbackError: String): InstallmentPlan =
        apiCall(fallbackError) { installmentApi.payInstallment(planId, installmentId) }
}
