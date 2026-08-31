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
import com.nexora.android.data.creditcard.CreditCardRepository
import com.nexora.android.data.creditcard.UpdateCreditCardPurchaseRequest
import com.nexora.android.data.transaction.Transaction
import kotlinx.coroutines.launch
import java.time.LocalDate

data class EditPurchaseUiState(
    val amount: String,
    val date: LocalDate,
    val merchant: String,
    val categoryId: String?,
    val description: String,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
) {
    val canSubmit: Boolean get() = (amount.toDoubleOrNull() ?: 0.0) > 0.0 && merchant.isNotBlank() && !isSaving
}

/** Solo para compras normales (sin plan MSI/MCI) — esas se editan desde EditInstallmentPlanViewModel. */
class EditCreditCardPurchaseViewModel(
    private val cardId: String,
    private val creditCardRepository: CreditCardRepository,
    private val categoryRepository: CategoryRepository,
    purchase: Transaction,
) : ViewModel() {

    private val transactionId = purchase.id

    var uiState by mutableStateOf(
        EditPurchaseUiState(
            amount = purchase.amount.toString(),
            date = LocalDate.parse(purchase.date),
            merchant = purchase.merchant ?: "",
            categoryId = purchase.categoryId,
            description = purchase.description ?: "",
        ),
    )
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
                creditCardRepository.updatePurchase(
                    cardId = cardId,
                    transactionId = transactionId,
                    request = UpdateCreditCardPurchaseRequest(
                        amount = state.amount.toDouble(),
                        date = state.date.toString(),
                        merchant = state.merchant.trim(),
                        categoryId = state.categoryId,
                        description = state.description.trim().ifBlank { null },
                    ),
                    fallbackError = fallbackError,
                )
                uiState = uiState.copy(isSaving = false, saved = true)
            } catch (e: ApiException) {
                uiState = uiState.copy(isSaving = false, error = e.message ?: fallbackError)
            }
        }
    }
}
