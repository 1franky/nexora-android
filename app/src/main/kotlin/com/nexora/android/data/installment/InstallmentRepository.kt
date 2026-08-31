package com.nexora.android.data.installment

import com.nexora.android.data.common.apiCall
import com.nexora.android.data.offline.OfflineCache
import com.nexora.android.data.offline.OperationType
import com.nexora.android.data.offline.PendingOperationDao
import com.nexora.android.data.offline.WriteOutcome
import com.nexora.android.data.offline.cachedApiCall
import com.nexora.android.data.offline.writeCall
import com.nexora.android.sync.SyncScheduler
import kotlinx.serialization.json.Json

private fun cacheKeyPlans(cardId: String) = "installment-plans:$cardId"

class InstallmentRepository(
    private val installmentApi: InstallmentApi,
    private val offlineCache: OfflineCache,
    private val pendingOperationDao: PendingOperationDao,
    private val syncScheduler: SyncScheduler,
    private val json: Json,
) {
    suspend fun createPlan(cardId: String, request: CreateInstallmentPlanRequest, fallbackError: String): WriteOutcome<InstallmentPlan> {
        val outcome = writeCall(
            fallbackMessage = fallbackError,
            pendingOperationDao = pendingOperationDao,
            json = json,
            type = OperationType.CREATE_INSTALLMENT_PLAN,
            summary = "Plan MSI/MCI: ${request.merchant} — ${request.amount} en ${request.installmentCount}",
            payload = request,
            pathParams = cardId,
        ) { key -> installmentApi.createPlan(cardId, key, request) }
        if (outcome is WriteOutcome.Queued) syncScheduler.requestSync()
        return outcome
    }

    suspend fun listForCard(cardId: String, fallbackError: String): List<InstallmentPlan> =
        cachedApiCall(offlineCache, cacheKeyPlans(cardId), fallbackError) { installmentApi.listForCard(cardId) }

    suspend fun payInstallment(planId: String, installmentId: String, fallbackError: String): WriteOutcome<InstallmentPlan> {
        val outcome = writeCall(
            fallbackMessage = fallbackError,
            pendingOperationDao = pendingOperationDao,
            json = json,
            type = OperationType.PAY_INSTALLMENT,
            summary = "Cuota pagada",
            payload = Unit,
            pathParams = "$planId|$installmentId",
        ) { key -> installmentApi.payInstallment(planId, installmentId, key) }
        if (outcome is WriteOutcome.Queued) syncScheduler.requestSync()
        return outcome
    }

    /** Sin caché ni cola offline a propósito: el usuario espera ver el resultado (o el error) de inmediato. */
    suspend fun updatePlan(planId: String, request: UpdateInstallmentPlanRequest, fallbackError: String): InstallmentPlan =
        apiCall(fallbackError) { installmentApi.updatePlan(planId, request) }
}
