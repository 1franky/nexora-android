package com.nexora.android.ui.transactions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.category.Category
import com.nexora.android.data.category.CategoryRepository
import com.nexora.android.data.category.CategoryType
import com.nexora.android.data.common.ApiException
import com.nexora.android.data.transaction.Transaction
import com.nexora.android.data.transaction.TransactionRepository
import com.nexora.android.data.transaction.TransactionType
import com.nexora.android.data.transaction.UpdateTransactionRequest
import kotlinx.coroutines.launch
import java.time.LocalDate

data class EditTransactionUiState(
    val amount: String,
    val date: LocalDate,
    val categoryId: String?,
    val description: String,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
) {
    val canSubmit: Boolean get() = (amount.toDoubleOrNull() ?: 0.0) > 0.0 && !isSaving
}

/** Solo para INCOME/EXPENSE — ver TransactionsScreen, mismo criterio que EditTransactionDialog en nexora-web. */
class EditTransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val transaction: Transaction,
) : ViewModel() {

    var uiState by mutableStateOf(
        EditTransactionUiState(
            amount = transaction.amount.toString(),
            date = LocalDate.parse(transaction.date),
            categoryId = transaction.categoryId,
            description = transaction.description ?: "",
        ),
    )
        private set

    fun onAmountChange(value: String) {
        uiState = uiState.copy(amount = value, error = null)
    }

    fun onDateChange(value: LocalDate) {
        uiState = uiState.copy(date = value)
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
                val type = if (transaction.type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
                val category = categoryRepository.createCategory(name, type, fallbackError)
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
                transactionRepository.updateTransaction(
                    id = transaction.id,
                    request = UpdateTransactionRequest(
                        amount = state.amount.toDouble(),
                        date = state.date.toString(),
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
