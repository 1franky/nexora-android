package com.nexora.android.data.transaction

import kotlinx.serialization.Serializable

/** Espejo de com.nexora.api.transaction (nexora-api). */

@Serializable
enum class TransactionType {
    INCOME, EXPENSE, TRANSFER, CREDIT_CARD_PURCHASE, CREDIT_CARD_PAYMENT, REFUND, ADJUSTMENT
}

@Serializable
data class Transaction(
    val id: String,
    val accountId: String,
    val type: TransactionType,
    val amount: Double,
    /**
     * Igual a [amount] pero con signo (positivo si aumenta el saldo de
     * [accountId], negativo si lo disminuye) — lo único que distingue la
     * pierna de salida de la de entrada en una transferencia, que comparte
     * el mismo [type] en ambas filas (ver TransactionService en nexora-api).
     */
    val balanceEffect: Double,
    val date: String,
    val description: String? = null,
    val categoryId: String? = null,
    val counterAccountId: String? = null,
    val merchant: String? = null,
)

@Serializable
data class CreateTransactionRequest(
    val type: TransactionType,
    val accountId: String,
    val amount: Double,
    val date: String,
    val categoryId: String? = null,
    val description: String? = null,
)

@Serializable
data class CreateTransferRequest(
    val fromAccountId: String,
    val toAccountId: String,
    val amount: Double,
    val date: String,
    val description: String? = null,
)

@Serializable
data class TransferResult(val outgoing: Transaction, val incoming: Transaction)
