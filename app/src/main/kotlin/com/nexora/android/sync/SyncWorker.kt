package com.nexora.android.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nexora.android.data.account.AccountApi
import com.nexora.android.data.account.CreateAccountRequest
import com.nexora.android.data.creditcard.CreateCreditCardRequest
import com.nexora.android.data.creditcard.CreditCardApi
import com.nexora.android.data.creditcard.CreditCardPaymentRequest
import com.nexora.android.data.creditcard.CreditCardPurchaseRequest
import com.nexora.android.data.installment.CreateInstallmentPlanRequest
import com.nexora.android.data.installment.InstallmentApi
import com.nexora.android.data.offline.OperationType
import com.nexora.android.data.offline.PendingOperationDao
import com.nexora.android.data.offline.PendingOperationEntity
import com.nexora.android.data.offline.PendingOperationStatus
import com.nexora.android.data.transaction.CreateTransactionRequest
import com.nexora.android.data.transaction.CreateTransferRequest
import com.nexora.android.data.transaction.TransactionApi
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Drena [PendingOperationDao] en orden de creación, reintentando cada
 * operación contra la API real con su misma Idempotency-Key (nunca se
 * regenera entre reintentos — así un reintento de una operación que el
 * servidor ya aplicó, pero cuya confirmación no llegó a guardarse
 * localmente, no la duplica: ver IdempotencyFilter en nexora-api).
 *
 * No toca ningún caché de lectura al terminar: la próxima vez que la
 * pantalla correspondiente recargue online, el GET normal ya trae los
 * datos reales y lo refresca solo (ver OfflineCache). Intentarlo aquí
 * requeriría reconstruir, por tipo de operación, cómo insertar la
 * respuesta real dentro de cada lista cacheada — se deja así por ahora.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val pendingOperationDao: PendingOperationDao,
    private val json: Json,
    private val accountApi: AccountApi,
    private val creditCardApi: CreditCardApi,
    private val transactionApi: TransactionApi,
    private val installmentApi: InstallmentApi,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pending = pendingOperationDao.listByStatus(PendingOperationStatus.PENDING.name)
        for (operation in pending) {
            val result = runCatching { apply(operation) }
            when {
                result.isSuccess -> pendingOperationDao.delete(operation.idempotencyKey)
                result.exceptionOrNull() is IOException -> return Result.retry() // sigue sin conexión: se reintenta más tarde, el resto queda pendiente
                else -> pendingOperationDao.updateStatus(
                    key = operation.idempotencyKey,
                    status = PendingOperationStatus.FAILED.name,
                    attempts = operation.attempts + 1,
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
        return Result.success()
    }

    private suspend fun apply(operation: PendingOperationEntity) {
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
        }
    }
}
