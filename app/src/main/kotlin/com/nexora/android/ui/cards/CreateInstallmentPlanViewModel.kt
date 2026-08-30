package com.nexora.android.ui.cards

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.category.Category
import com.nexora.android.data.category.CategoryRepository
import com.nexora.android.data.category.CategoryType
import com.nexora.android.data.common.ApiException
import com.nexora.android.data.installment.CreateInstallmentPlanRequest
import com.nexora.android.data.installment.InstallmentRepository
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CreateInstallmentPlanUiState(
    val amount: String = "",
    val date: LocalDate = LocalDate.now(),
    val merchant: String = "",
    val installmentCount: String = "3",
    val interestRate: String = "0",
    val categoryId: String? = null,
    val description: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
) {
    val canSubmit: Boolean get() {
        val count = installmentCount.toIntOrNull()
        val rate = interestRate.toDoubleOrNull()
        return (amount.toDoubleOrNull() ?: 0.0) > 0.0 &&
            merchant.isNotBlank() &&
            count != null && count in 2..60 &&
            rate != null && rate >= 0.0 &&
            !isSaving
    }
}

/** Formulario de "Comprar a MSI/MCI". Mismo criterio que CreateInstallmentPlanDialog en nexora-web. */
class CreateInstallmentPlanViewModel(
    private val cardId: String,
    private val installmentRepository: InstallmentRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    var uiState by mutableStateOf(CreateInstallmentPlanUiState())
        private set

    fun onAmountChange(value: String) {
        uiState = uiState.copy(amount = value, error = null)
    }

    fun onDateChange(value: LocalDate) {
        uiState = uiState.copy(date = value)
    }

    fun onMerchantChange(value: String) {
        uiState = uiState.copy(merchant = value, error = null)
    }

    fun onInstallmentCountChange(value: String) {
        uiState = uiState.copy(installmentCount = value, error = null)
    }

    fun onInterestRateChange(value: String) {
        uiState = uiState.copy(interestRate = value, error = null)
    }

    fun onCategoryChange(value: String?) {
        uiState = uiState.copy(categoryId = value)
    }

    fun onDescriptionChange(value: String) {
        uiState = uiState.copy(description = value)
    }

    fun createCategory(name: String, fallbackError: String, onCreated: (Category) -> Unit) {
        viewModelScope.launch {
            try {
                val category = categoryRepository.createCategory(name, CategoryType.EXPENSE, fallbackError)
                onCreated(category)
            } catch (e: ApiException) {
                uiState = uiState.copy(error = e.message ?: fallbackError)
            }
        }
    }

    fun submit(fallbackError: String) {
        val state = uiState
        if (!state.canSubmit) return

        uiState = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                installmentRepository.createPlan(
                    cardId,
                    CreateInstallmentPlanRequest(
                        amount = state.amount.toDouble(),
                        date = state.date.toString(),
                        merchant = state.merchant.trim(),
                        installmentCount = state.installmentCount.toInt(),
                        interestRate = state.interestRate.toDouble(),
                        categoryId = state.categoryId,
                        description = state.description.trim().ifBlank { null },
                    ),
                    fallbackError,
                )
                uiState = uiState.copy(isSaving = false, saved = true)
            } catch (e: ApiException) {
                uiState = uiState.copy(isSaving = false, error = e.message ?: fallbackError)
            }
        }
    }
}
