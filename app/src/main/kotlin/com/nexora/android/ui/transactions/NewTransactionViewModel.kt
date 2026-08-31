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
import com.nexora.android.data.transaction.CreateTransactionRequest
import com.nexora.android.data.transaction.CreateTransferRequest
import com.nexora.android.data.transaction.TransactionRepository
import com.nexora.android.data.transaction.TransactionType
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class MovementKind { EXPENSE, INCOME, TRANSFER }

data class NewTransactionUiState(
    val kind: MovementKind = MovementKind.EXPENSE,
    val amount: String = "",
    val date: LocalDate = LocalDate.now(),
    val accountId: String = "",
    val categoryId: String? = null,
    val toAccountId: String = "",
    val description: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

/**
 * Formulario de "Nuevo movimiento" (ingreso, gasto o transferencia). No
 * duplica reglas financieras: solo arma la petición y deja que
 * nexora-api valide (ej. no transferir a la misma cuenta, monto > 0).
 */
class NewTransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    defaultAccountId: String?,
    initialKind: MovementKind = MovementKind.EXPENSE,
) : ViewModel() {

    var uiState by mutableStateOf(NewTransactionUiState(kind = initialKind, accountId = defaultAccountId ?: ""))
        private set

    fun onKindChange(kind: MovementKind) {
        uiState = uiState.copy(kind = kind, categoryId = null, error = null)
    }

    fun onAmountChange(value: String) {
        uiState = uiState.copy(amount = value, error = null)
    }

    fun onDateChange(value: LocalDate) {
        uiState = uiState.copy(date = value)
    }

    fun onAccountChange(value: String) {
        uiState = uiState.copy(
            accountId = value,
            toAccountId = if (uiState.toAccountId == value) "" else uiState.toAccountId,
        )
    }

    fun onCategoryChange(value: String?) {
        uiState = uiState.copy(categoryId = value)
    }

    fun onToAccountChange(value: String) {
        uiState = uiState.copy(toAccountId = value)
    }

    fun onDescriptionChange(value: String) {
        uiState = uiState.copy(description = value)
    }

    fun createCategory(name: String, type: CategoryType, fallbackError: String, onCreated: (Category) -> Unit) {
        viewModelScope.launch {
            try {
                val category = categoryRepository.createCategory(name, type, fallbackError)
                onCreated(category)
            } catch (e: ApiException) {
                uiState = uiState.copy(error = e.message ?: fallbackError)
            }
        }
    }

    fun submit(fallbackError: String) {
        val amount = uiState.amount.toDoubleOrNull()
        if (amount == null || amount <= 0 || uiState.accountId.isBlank() || uiState.isSaving) return
        if (uiState.kind == MovementKind.TRANSFER && uiState.toAccountId.isBlank()) return

        uiState = uiState.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                val dateStr = uiState.date.toString() // LocalDate.toString() ya es ISO yyyy-MM-dd
                val description = uiState.description.trim().ifBlank { null }
                if (uiState.kind == MovementKind.TRANSFER) {
                    transactionRepository.createTransfer(
                        CreateTransferRequest(uiState.accountId, uiState.toAccountId, amount, dateStr, description),
                        fallbackError,
                    )
                } else {
                    transactionRepository.createTransaction(
                        CreateTransactionRequest(
                            type = if (uiState.kind == MovementKind.INCOME) TransactionType.INCOME else TransactionType.EXPENSE,
                            accountId = uiState.accountId,
                            amount = amount,
                            date = dateStr,
                            categoryId = uiState.categoryId,
                            description = description,
                        ),
                        fallbackError,
                    )
                }
                uiState = uiState.copy(isSaving = false, saved = true)
            } catch (e: ApiException) {
                uiState = uiState.copy(isSaving = false, error = e.message ?: fallbackError)
            }
        }
    }
}
