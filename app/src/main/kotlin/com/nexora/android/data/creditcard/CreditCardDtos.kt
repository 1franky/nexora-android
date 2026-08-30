package com.nexora.android.data.creditcard

import kotlinx.serialization.Serializable

/** Espejo de com.nexora.api.creditcard.web (nexora-api). */

@Serializable
enum class CreditCardStatus { ACTIVE, ARCHIVED }

@Serializable
data class CreditCard(
    val id: String,
    val accountId: String,
    val name: String,
    val bank: String,
    val last4: String,
    val currency: String,
    val creditLimit: Double,
    val currentDebt: Double,
    val availableCredit: Double,
    val closingDay: Int,
    val paymentDueDay: Int,
    val nextClosingDate: String,
    val nextPaymentDueDate: String,
    val status: CreditCardStatus,
)

@Serializable
data class CreateCreditCardRequest(
    val name: String,
    val bank: String,
    val last4: String,
    val creditLimit: Double,
    val closingDay: Int,
    val paymentDueDay: Int,
    val currency: String,
)

@Serializable
data class CreditCardPurchaseRequest(
    val amount: Double,
    val date: String,
    val merchant: String,
    val categoryId: String? = null,
    val description: String? = null,
    val reference: String? = null,
)

@Serializable
data class CreditCardPaymentRequest(
    val fromAccountId: String,
    val amount: Double,
    val date: String,
    val description: String? = null,
    val reference: String? = null,
)
