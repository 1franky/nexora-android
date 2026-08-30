package com.nexora.android.sync

import com.nexora.android.data.account.AccountType
import com.nexora.android.data.account.CreateAccountRequest
import com.nexora.android.data.creditcard.CreateCreditCardRequest
import com.nexora.android.data.creditcard.CreditCardPaymentRequest
import com.nexora.android.data.creditcard.CreditCardPurchaseRequest
import com.nexora.android.data.installment.CreateInstallmentPlanRequest
import com.nexora.android.data.offline.OperationType
import com.nexora.android.data.offline.PendingOperationEntity
import com.nexora.android.data.offline.PendingOperationStatus
import com.nexora.android.data.transaction.CreateTransactionRequest
import com.nexora.android.data.transaction.CreateTransferRequest
import com.nexora.android.data.transaction.TransactionType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Un [OperationDispatcher.apply] por [OperationType]: verifica que
 * reconstruye la llamada correcta (Idempotency-Key, path params, payload
 * deserializado) a partir de lo que [com.nexora.android.data.offline.writeCall]
 * guardó — sin esto, un bug aquí solo se vería reintentando de verdad contra
 * el backend.
 */
class OperationDispatcherTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val accountApi = FakeAccountApi()
    private val creditCardApi = FakeCreditCardApi()
    private val transactionApi = FakeTransactionApi()
    private val installmentApi = FakeInstallmentApi()
    private val dispatcher = OperationDispatcher(json, accountApi, creditCardApi, transactionApi, installmentApi)

    private fun operation(type: OperationType, payload: String, pathParams: String? = null, key: String = "key-1") =
        PendingOperationEntity(
            idempotencyKey = key,
            type = type.name,
            summary = "test",
            payloadJson = payload,
            pathParams = pathParams,
            createdAt = 0L,
            status = PendingOperationStatus.PENDING.name,
        )

    @Test
    fun `CREATE_ACCOUNT llama a accountApi con la key y el payload`() = runTest {
        val request = CreateAccountRequest(name = "Débito", type = AccountType.DEBIT, currency = "MXN", openingBalance = 100.0)
        dispatcher.apply(operation(OperationType.CREATE_ACCOUNT, json.encodeToString(request), key = "key-account"))

        assertEquals("key-account", accountApi.lastKey)
        assertEquals(request, accountApi.lastRequest)
    }

    @Test
    fun `CREATE_CREDIT_CARD llama a creditCardApi con la key y el payload`() = runTest {
        val request = CreateCreditCardRequest(
            name = "BBVA Azul", bank = "BBVA", last4 = "1234", creditLimit = 50000.0,
            closingDay = 15, paymentDueDay = 5, currency = "MXN",
        )
        dispatcher.apply(operation(OperationType.CREATE_CREDIT_CARD, json.encodeToString(request), key = "key-card"))

        assertEquals("key-card", creditCardApi.lastKey)
        assertEquals(request, creditCardApi.lastCreateRequest)
    }

    @Test
    fun `CREATE_TRANSACTION llama a transactionApi con la key y el payload`() = runTest {
        val request = CreateTransactionRequest(type = TransactionType.EXPENSE, accountId = "acc-1", amount = 500.0, date = "2026-01-01")
        dispatcher.apply(operation(OperationType.CREATE_TRANSACTION, json.encodeToString(request), key = "key-txn"))

        assertEquals("key-txn", transactionApi.lastKey)
        assertEquals(request, transactionApi.lastTransactionRequest)
    }

    @Test
    fun `CREATE_TRANSFER llama a transactionApi con la key y el payload`() = runTest {
        val request = CreateTransferRequest(fromAccountId = "acc-1", toAccountId = "acc-2", amount = 500.0, date = "2026-01-01")
        dispatcher.apply(operation(OperationType.CREATE_TRANSFER, json.encodeToString(request), key = "key-transfer"))

        assertEquals("key-transfer", transactionApi.lastKey)
        assertEquals(request, transactionApi.lastTransferRequest)
    }

    @Test
    fun `CREDIT_CARD_PURCHASE usa el cardId de pathParams`() = runTest {
        val request = CreditCardPurchaseRequest(amount = 250.0, date = "2026-01-01", merchant = "Amazon")
        dispatcher.apply(operation(OperationType.CREDIT_CARD_PURCHASE, json.encodeToString(request), pathParams = "card-42", key = "key-purchase"))

        assertEquals("card-42", creditCardApi.lastCardId)
        assertEquals("key-purchase", creditCardApi.lastKey)
        assertEquals(request, creditCardApi.lastPurchaseRequest)
    }

    @Test
    fun `CREDIT_CARD_PAYMENT usa el cardId de pathParams`() = runTest {
        val request = CreditCardPaymentRequest(fromAccountId = "acc-1", amount = 1000.0, date = "2026-01-01")
        dispatcher.apply(operation(OperationType.CREDIT_CARD_PAYMENT, json.encodeToString(request), pathParams = "card-7", key = "key-payment"))

        assertEquals("card-7", creditCardApi.lastCardId)
        assertEquals("key-payment", creditCardApi.lastKey)
        assertEquals(request, creditCardApi.lastPaymentRequest)
    }

    @Test
    fun `CREATE_INSTALLMENT_PLAN usa el cardId de pathParams`() = runTest {
        val request = CreateInstallmentPlanRequest(amount = 1200.0, date = "2026-01-01", merchant = "Liverpool", installmentCount = 12)
        dispatcher.apply(operation(OperationType.CREATE_INSTALLMENT_PLAN, json.encodeToString(request), pathParams = "card-9", key = "key-plan"))

        assertEquals("card-9", installmentApi.lastCardId)
        assertEquals("key-plan", installmentApi.lastKey)
        assertEquals(request, installmentApi.lastPlanRequest)
    }

    @Test
    fun `PAY_INSTALLMENT separa planId e installmentId de pathParams`() = runTest {
        dispatcher.apply(operation(OperationType.PAY_INSTALLMENT, payload = "{}", pathParams = "plan-1|inst-3", key = "key-pay-installment"))

        assertEquals("plan-1", installmentApi.lastPlanId)
        assertEquals("inst-3", installmentApi.lastInstallmentId)
        assertEquals("key-pay-installment", installmentApi.lastKey)
    }
}
