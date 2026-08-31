package com.nexora.android.data.account

import kotlinx.serialization.Serializable

/** Espejo de com.nexora.api.account.web (nexora-api). */

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

@Serializable
data class CreateAccountRequest(
    val name: String,
    val type: AccountType,
    val currency: String,
    val openingBalance: Double = 0.0,
    val includeInAvailableBalance: Boolean = true,
    val includeInNetWorth: Boolean = true,
)

@Serializable
data class UpdateAccountRequest(
    val name: String,
    val includeInAvailableBalance: Boolean,
    val includeInNetWorth: Boolean,
)
