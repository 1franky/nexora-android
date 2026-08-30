package com.nexora.android.ui.cards

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.common.ApiException
import com.nexora.android.data.creditcard.CreditCardPaymentRequest
import com.nexora.android.data.creditcard.CreditCardRepository
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PaymentUiState(
    val fromAccountId: String = "",
    val amount: String = "",
    val date: LocalDate = LocalDate.now(),
    val description: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
) {
    val canSubmit: Boolean get() =
        fromAccountId.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0.0 && !isSaving
}

/** Formulario de "Pagar tarjeta". Mismo criterio que CreditCardPaymentDialog en nexora-web. */
class CreditCardPaymentViewModel(
    private val cardId: String,
    private val creditCardRepository: CreditCardRepository,
    defaultFromAccountId: String?,
) : ViewModel() {

    var uiState by mutableStateOf(PaymentUiState(fromAccountId = defaultFromAccountId ?: ""))
        private set

    fun onFromAccountChange(value: String) {
        uiState = uiState.copy(fromAccountId = value)
    }

    fun onAmountChange(value: String) {
        uiState = uiState.copy(amount = value, error = null)
    }

    fun onDateChange(value: LocalDate) {
        uiState = uiState.copy(date = value)
    }

    fun onDescriptionChange(value: String) {
        uiState = uiState.copy(description = value)
    }

    fun submit(fallbackError: String) {
        val state = uiState
        if (!state.canSubmit) return

        uiState = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                creditCardRepository.pay(
                    cardId,
                    CreditCardPaymentRequest(
                        fromAccountId = state.fromAccountId,
                        amount = state.amount.toDouble(),
                        date = state.date.toString(),
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
