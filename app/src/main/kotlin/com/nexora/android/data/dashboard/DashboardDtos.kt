package com.nexora.android.data.dashboard

import kotlinx.serialization.Serializable

/** Espejo de com.nexora.api.dashboard.web.DashboardResponse (nexora-api). Solo los campos que usa el MVP (A2). */

@Serializable
data class CategoryAmount(val categoryId: String, val categoryName: String, val amount: Double)

@Serializable
data class UpcomingCardPayment(
    val creditCardId: String,
    val creditCardName: String,
    val dueDate: String,
    val expectedPayment: Double,
)

@Serializable
data class MonthlyPoint(val month: String, val amount: Double)

@Serializable
data class TransactionSummary(
    val id: String,
    val accountId: String,
    val type: String,
    val amount: Double,
    val date: String,
    val description: String? = null,
    val merchant: String? = null,
)

@Serializable
data class DashboardResponse(
    val month: String,
    val availableBalance: Double,
    val netWorth: Double,
    val incomeThisMonth: Double,
    val expenseThisMonth: Double,
    val monthlyBalance: Double,
    val expensesByCategory: List<CategoryAmount>,
    val incomeByCategory: List<CategoryAmount>,
    val creditCardDebt: Double,
    val availableCredit: Double,
    val upcomingPayments: List<UpcomingCardPayment>,
    val activeMsiPlansCount: Int,
    val monthlyInstallmentCommitment: Double,
    val netWorthEvolution: List<MonthlyPoint>,
    val expenseEvolution: List<MonthlyPoint>,
    val recentTransactions: List<TransactionSummary>,
)
