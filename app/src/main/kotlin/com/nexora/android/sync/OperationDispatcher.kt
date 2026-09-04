package com.nexora.android.sync

import com.nexora.android.data.account.AccountApi
import com.nexora.android.data.account.CreateAccountRequest
import com.nexora.android.data.creditcard.CreateCreditCardRequest
import com.nexora.android.data.creditcard.CreditCardApi
import com.nexora.android.data.creditcard.CreditCardPaymentRequest
import com.nexora.android.data.creditcard.CreditCardPurchaseRequest
import com.nexora.android.data.installment.CreateInstallmentPlanRequest
import com.nexora.android.data.installment.InstallmentApi
import com.nexora.android.data.offline.OperationType
import com.nexora.android.data.offline.PendingOperationEntity
import com.nexora.android.data.sat.SatApi
import com.nexora.android.data.sat.SyncRequest
import com.nexora.android.data.transaction.CreateTransactionRequest
import com.nexora.android.data.transaction.CreateTransferRequest
import com.nexora.android.data.transaction.TransactionApi
import kotlinx.serialization.json.Json

/**
 * Reconstruye la llamada real de una [PendingOperationEntity] a partir de su
 * [OperationType] y manda la Idempotency-Key. Aparte de [SyncWorker] a
 * propósito: no depende de Context/WorkerParameters, así que se prueba con
 * JVM puro (fakes de cada Api), sin Robolectric.
 */
class OperationDispatcher(
    private val json: Json,
    private val accountApi: AccountApi,
    private val creditCardApi: CreditCardApi,
    private val transactionApi: TransactionApi,
    private val installmentApi: InstallmentApi,
    private val satApi: SatApi,
) {
    suspend fun apply(operation: PendingOperationEntity) {
        val key = operation.idempotencyKey
        when (OperationType.valueOf(operation.type)) {
            OperationType.CREATE_ACCOUNT ->
                accountApi.createAccount(key, json.decodeFromString<CreateAccountRequest>(operation.payloadJson))

            OperationType.CREATE_CREDIT_CARD ->
                creditCardApi.createCreditCard(key, json.decodeFromString<CreateCreditCardRequest>(operation.payloadJson))

            OperationType.CREATE_TRANSACTION ->
                transactionApi.createTransaction(key, json.decodeFromString<CreateTransactionRequest>(operation.payloadJson))

            OperationType.CREATE_TRANSFER ->
                transactionApi.createTransfer(key, json.decodeFromString<CreateTransferRequest>(operation.payloadJson))

            OperationType.CREDIT_CARD_PURCHASE -> {
                val cardId = requireNotNull(operation.pathParams)
                creditCardApi.purchase(cardId, key, json.decodeFromString<CreditCardPurchaseRequest>(operation.payloadJson))
            }

            OperationType.CREDIT_CARD_PAYMENT -> {
                val cardId = requireNotNull(operation.pathParams)
                creditCardApi.pay(cardId, key, json.decodeFromString<CreditCardPaymentRequest>(operation.payloadJson))
            }

            OperationType.CREATE_INSTALLMENT_PLAN -> {
                val cardId = requireNotNull(operation.pathParams)
                installmentApi.createPlan(cardId, key, json.decodeFromString<CreateInstallmentPlanRequest>(operation.payloadJson))
            }

            OperationType.PAY_INSTALLMENT -> {
                val (planId, installmentId) = requireNotNull(operation.pathParams).split("|")
                installmentApi.payInstallment(planId, installmentId, key)
            }

            OperationType.SAT_SYNC ->
                satApi.sync(json.decodeFromString<SyncRequest>(operation.payloadJson))
        }
    }
}
