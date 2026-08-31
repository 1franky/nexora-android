package com.nexora.android.ui.cards

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.common.ApiException
import com.nexora.android.data.creditcard.CreditCard
import com.nexora.android.data.creditcard.CreditCardRepository
import kotlinx.coroutines.launch

data class EditCreditCardUiState(
    val name: String,
    val bank: String,
    val creditLimit: String,
    val closingDay: String,
    val paymentDueDay: String,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
) {
    val limitValid: Boolean get() = creditLimit.toDoubleOrNull()?.let { it > 0 } == true
    val canSubmit: Boolean get() = name.isNotBlank() && bank.isNotBlank() && limitValid && !isSaving
}

/** Últimos 4 dígitos y moneda no se editan aquí a propósito (ver nexora-api). */
class EditCreditCardViewModel(private val creditCardRepository: CreditCardRepository, card: CreditCard) : ViewModel() {

    private val cardId = card.id

    var uiState by mutableStateOf(
        EditCreditCardUiState(
            name = card.name,
            bank = card.bank,
            creditLimit = card.creditLimit.toString(),
            closingDay = card.closingDay.toString(),
            paymentDueDay = card.paymentDueDay.toString(),
        ),
    )
        private set

    fun onNameChange(value: String) {
        uiState = uiState.copy(name = value, error = null)
    }

    fun onBankChange(value: String) {
        uiState = uiState.copy(bank = value, error = null)
    }

    fun onCreditLimitChange(value: String) {
        uiState = uiState.copy(creditLimit = value, error = null)
    }

    fun onClosingDayChange(value: String) {
        uiState = uiState.copy(closingDay = value)
    }

    fun onPaymentDueDayChange(value: String) {
        uiState = uiState.copy(paymentDueDay = value)
    }

    fun submit(fallbackError: String) {
        val state = uiState
        if (!state.canSubmit) return

        uiState = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                creditCardRepository.updateCreditCard(
                    id = cardId,
                    name = state.name.trim(),
                    bank = state.bank.trim(),
                    creditLimit = state.creditLimit.toDouble(),
                    closingDay = state.closingDay.toIntOrNull() ?: 1,
                    paymentDueDay = state.paymentDueDay.toIntOrNull() ?: 15,
                    fallbackError = fallbackError,
                )
                uiState = uiState.copy(isSaving = false, saved = true)
            } catch (e: ApiException) {
                uiState = uiState.copy(isSaving = false, error = e.message ?: fallbackError)
            }
        }
    }
}
