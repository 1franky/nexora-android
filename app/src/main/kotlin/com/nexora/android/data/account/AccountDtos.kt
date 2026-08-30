package com.nexora.android.data.account

import kotlinx.serialization.Serializable

/** Espejo de com.nexora.api.account.web (nexora-api). Solo lectura por ahora — el alta de cuentas es A4. */

@Serializable
enum class AccountType { DEBIT, SAVINGS, CREDIT_CARD, AFORE, PPR }

@Serializable
enum class AccountStatus { ACTIVE, ARCHIVED }

@Serializable
data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val currency: String,
    val balance: Double,
    val includeInAvailableBalance: Boolean,
    val includeInNetWorth: Boolean,
    val status: AccountStatus,
)
