package com.nexora.android.data.creditcard

import com.nexora.android.data.offline.OfflineCache
import com.nexora.android.data.offline.OperationType
import com.nexora.android.data.offline.PendingOperationDao
import com.nexora.android.data.offline.WriteOutcome
import com.nexora.android.data.offline.cachedApiCall
import com.nexora.android.data.offline.writeCall
import com.nexora.android.data.transaction.Transaction
import com.nexora.android.data.transaction.TransferResult
import com.nexora.android.sync.SyncScheduler
import kotlinx.serialization.json.Json

private const val CACHE_KEY_CARDS = "credit-cards"
private fun cacheKeyCard(id: String) = "credit-card:$id"

class CreditCardRepository(
    private val creditCardApi: CreditCardApi,
    private val offlineCache: OfflineCache,
    private val pendingOperationDao: PendingOperationDao,
    private val syncScheduler: SyncScheduler,
    private val json: Json,
) {
    suspend fun listCreditCards(fallbackError: String): List<CreditCard> =
        cachedApiCall(offlineCache, CACHE_KEY_CARDS, fallbackError) { creditCardApi.listCreditCards() }

    suspend fun getCreditCard(id: String, fallbackError: String): CreditCard =
        cachedApiCall(offlineCache, cacheKeyCard(id), fallbackError) { creditCardApi.getCreditCard(id) }

    suspend fun createCreditCard(request: CreateCreditCardRequest, fallbackError: String): WriteOutcome<CreditCard> {
        val outcome = writeCall(
            fallbackMessage = fallbackError,
            pendingOperationDao = pendingOperationDao,
            json = json,
            type = OperationType.CREATE_CREDIT_CARD,
            summary = "Tarjeta nueva: ${request.name}",
            payload = request,
        ) { key -> creditCardApi.createCreditCard(key, request) }
        if (outcome is WriteOutcome.Queued) syncScheduler.requestSync()
        return outcome
    }

    suspend fun purchase(id: String, request: CreditCardPurchaseRequest, fallbackError: String): WriteOutcome<Transaction> {
        val outcome = writeCall(
            fallbackMessage = fallbackError,
            pendingOperationDao = pendingOperationDao,
            json = json,
            type = OperationType.CREDIT_CARD_PURCHASE,
            summary = "Compra: ${request.merchant} — ${request.amount}",
            payload = request,
            pathParams = id,
        ) { key -> creditCardApi.purchase(id, key, request) }
        if (outcome is WriteOutcome.Queued) syncScheduler.requestSync()
        return outcome
    }

    suspend fun pay(id: String, request: CreditCardPaymentRequest, fallbackError: String): WriteOutcome<TransferResult> {
        val outcome = writeCall(
            fallbackMessage = fallbackError,
            pendingOperationDao = pendingOperationDao,
            json = json,
            type = OperationType.CREDIT_CARD_PAYMENT,
            summary = "Pago de tarjeta: ${request.amount}",
            payload = request,
            pathParams = id,
        ) { key -> creditCardApi.pay(id, key, request) }
        if (outcome is WriteOutcome.Queued) syncScheduler.requestSync()
        return outcome
    }
}
