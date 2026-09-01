package com.nexora.android.sync

import com.nexora.android.data.account.Account
import com.nexora.android.data.account.AccountApi
import com.nexora.android.data.account.AccountStatus
import com.nexora.android.data.account.CreateAccountRequest
import com.nexora.android.data.account.UpdateAccountRequest
import com.nexora.android.data.creditcard.CreateCreditCardRequest
import com.nexora.android.data.creditcard.CreditCard
import com.nexora.android.data.creditcard.CreditCardApi
import com.nexora.android.data.creditcard.CreditCardPaymentRequest
import com.nexora.android.data.creditcard.CreditCardPurchaseRequest
import com.nexora.android.data.creditcard.CreditCardStatus
import com.nexora.android.data.creditcard.UpdateCreditCardPurchaseRequest
import com.nexora.android.data.creditcard.UpdateCreditCardRequest
import com.nexora.android.data.installment.CreateInstallmentPlanRequest
import com.nexora.android.data.installment.InstallmentApi
import com.nexora.android.data.installment.InstallmentPlan
import com.nexora.android.data.installment.InstallmentPlanStatus
import com.nexora.android.data.installment.InstallmentPlanType
import com.nexora.android.data.installment.UpdateInstallmentPlanRequest
import com.nexora.android.data.transaction.CreateTransactionRequest
import com.nexora.android.data.transaction.CreateTransferRequest
import com.nexora.android.data.transaction.Transaction
import com.nexora.android.data.transaction.TransactionApi
import com.nexora.android.data.transaction.TransactionType
import com.nexora.android.data.transaction.TransferResult
import com.nexora.android.data.transaction.UpdateTransactionRequest

/** Fakes que solo registran con qué se les llamó — JVM puro, sin mocking library (ver AppContainer sobre el criterio de evitar dependencias). */

private fun dummyTransaction(accountId: String) = Transaction(
    id = "txn-1", accountId = accountId, type = TransactionType.EXPENSE, amount = 1.0,
    balanceEffect = -1.0, date = "2026-01-01",
)

class FakeAccountApi : AccountApi {
    var lastKey: String? = null
    var lastRequest: CreateAccountRequest? = null

    override suspend fun listAccounts(): List<Account> = emptyList()

    override suspend fun createAccount(idempotencyKey: String, request: CreateAccountRequest): Account {
        lastKey = idempotencyKey
        lastRequest = request
        return Account(
            id = "acc-1", name = request.name, type = request.type, currency = request.currency,
            balance = request.openingBalance, includeInAvailableBalance = request.includeInAvailableBalance,
            includeInNetWorth = request.includeInNetWorth, status = AccountStatus.ACTIVE,
        )
    }

    override suspend fun updateAccount(id: String, request: UpdateAccountRequest): Account = error("no usado en estos tests")
}

class FakeCreditCardApi : CreditCardApi {
    var lastKey: String? = null
    var lastCardId: String? = null
    var lastCreateRequest: CreateCreditCardRequest? = null
    var lastPurchaseRequest: CreditCardPurchaseRequest? = null
    var lastPaymentRequest: CreditCardPaymentRequest? = null

    override suspend fun listCreditCards(): List<CreditCard> = emptyList()

    override suspend fun createCreditCard(idempotencyKey: String, request: CreateCreditCardRequest): CreditCard {
        lastKey = idempotencyKey
        lastCreateRequest = request
        return CreditCard(
            id = "card-1", accountId = "acc-1", name = request.name, bank = request.bank, last4 = request.last4,
            currency = request.currency, creditLimit = request.creditLimit, currentDebt = 0.0,
            availableCredit = request.creditLimit, closingDay = request.closingDay, paymentDueDay = request.paymentDueDay,
            nextClosingDate = "2026-01-15", nextPaymentDueDate = "2026-01-25", status = CreditCardStatus.ACTIVE,
        )
    }

    override suspend fun getCreditCard(id: String): CreditCard = error("no usado en estos tests")

    override suspend fun updateCreditCard(id: String, request: UpdateCreditCardRequest): CreditCard = error("no usado en estos tests")

    override suspend fun purchase(id: String, idempotencyKey: String, request: CreditCardPurchaseRequest): Transaction {
        lastCardId = id
        lastKey = idempotencyKey
        lastPurchaseRequest = request
        return dummyTransaction(id)
    }

    override suspend fun pay(id: String, idempotencyKey: String, request: CreditCardPaymentRequest): TransferResult {
        lastCardId = id
        lastKey = idempotencyKey
        lastPaymentRequest = request
        return TransferResult(dummyTransaction(request.fromAccountId), dummyTransaction(id))
    }

    override suspend fun updatePurchase(id: String, transactionId: String, request: UpdateCreditCardPurchaseRequest): Transaction =
        error("no usado en estos tests")
}

class FakeTransactionApi : TransactionApi {
    var lastKey: String? = null
    var lastTransactionRequest: CreateTransactionRequest? = null
    var lastTransferRequest: CreateTransferRequest? = null

    override suspend fun listTransactions(accountId: String?): List<Transaction> = emptyList()

    override suspend fun createTransaction(idempotencyKey: String, request: CreateTransactionRequest): Transaction {
        lastKey = idempotencyKey
        lastTransactionRequest = request
        return dummyTransaction(request.accountId)
    }

    override suspend fun createTransfer(idempotencyKey: String, request: CreateTransferRequest): TransferResult {
        lastKey = idempotencyKey
        lastTransferRequest = request
        return TransferResult(dummyTransaction(request.fromAccountId), dummyTransaction(request.toAccountId))
    }

    override suspend fun updateTransaction(id: String, request: UpdateTransactionRequest): Transaction = error("no usado en estos tests")

    override suspend fun deleteTransaction(id: String) = error("no usado en estos tests")
}

class FakeInstallmentApi : InstallmentApi {
    var lastKey: String? = null
    var lastCardId: String? = null
    var lastPlanRequest: CreateInstallmentPlanRequest? = null
    var lastPlanId: String? = null
    var lastInstallmentId: String? = null

    private fun dummyPlan(cardId: String) = InstallmentPlan(
        id = "plan-1", creditCardId = cardId, transactionId = "txn-1", planType = InstallmentPlanType.MSI,
        originalAmount = 100.0, installmentCount = 3, interestRate = 0.0, interestAmount = 0.0, totalAmount = 100.0,
        installmentAmount = 33.33, startDate = "2026-01-01", endDate = "2026-04-01", status = InstallmentPlanStatus.ACTIVE,
        installmentsPaid = 0, installmentsPending = 3, financedBalance = 100.0, nextInstallment = null, installments = emptyList(),
    )

    override suspend fun createPlan(cardId: String, idempotencyKey: String, request: CreateInstallmentPlanRequest): InstallmentPlan {
        lastCardId = cardId
        lastKey = idempotencyKey
        lastPlanRequest = request
        return dummyPlan(cardId)
    }

    override suspend fun listForCard(cardId: String): List<InstallmentPlan> = emptyList()

    override suspend fun payInstallment(id: String, installmentId: String, idempotencyKey: String): InstallmentPlan {
        lastPlanId = id
        lastInstallmentId = installmentId
        lastKey = idempotencyKey
        return dummyPlan("card-1")
    }

    override suspend fun updatePlan(id: String, request: UpdateInstallmentPlanRequest): InstallmentPlan = error("no usado en estos tests")
}
