package com.nexora.android.data.installment

import kotlinx.serialization.Serializable

/** Espejo de com.nexora.api.installment.web (nexora-api). Compras a MSI/MCI. */

@Serializable
enum class InstallmentPlanType { MSI, MCI }

@Serializable
enum class InstallmentPlanStatus { ACTIVE, COMPLETED, CANCELLED }

@Serializable
enum class InstallmentStatus { PENDING, PAID }

@Serializable
data class Installment(
    val id: String,
    val number: Int,
    val dueDate: String,
    val amount: Double,
    val status: InstallmentStatus,
    val paidAt: String? = null,
)

@Serializable
data class InstallmentPlan(
    val id: String,
    val creditCardId: String,
    val transactionId: String,
    val planType: InstallmentPlanType,
    val originalAmount: Double,
    val installmentCount: Int,
    val interestRate: Double,
    val interestAmount: Double,
    val totalAmount: Double,
    val installmentAmount: Double,
    val startDate: String,
    val endDate: String,
    val status: InstallmentPlanStatus,
    val installmentsPaid: Int,
    val installmentsPending: Int,
    val financedBalance: Double,
    val nextInstallment: Installment? = null,
    val installments: List<Installment>,
)

@Serializable
data class CreateInstallmentPlanRequest(
    val amount: Double,
    val date: String,
    val merchant: String,
    val installmentCount: Int,
    val interestRate: Double = 0.0,
    val categoryId: String? = null,
    val description: String? = null,
)
