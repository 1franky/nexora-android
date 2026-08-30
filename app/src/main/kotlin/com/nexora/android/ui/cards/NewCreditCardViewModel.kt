package com.nexora.android.ui.cards

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.common.ApiException
import com.nexora.android.data.creditcard.CreateCreditCardRequest
import com.nexora.android.data.creditcard.CreditCardRepository
import kotlinx.coroutines.launch

private val CURRENCY_PATTERN = Regex("^[A-Z]{3}$")
private val LAST4_PATTERN = Regex("^[0-9]{4}$")

data class NewCreditCardUiState(
    val name: String = "",
    val bank: String = "",
    val last4: String = "",
    val creditLimit: String = "",
    val closingDay: String = "1",
    val paymentDueDay: String = "15",
    val currency: String = "MXN",
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
) {
    val last4Valid: Boolean get() = LAST4_PATTERN.matches(last4)
    val currencyValid: Boolean get() = CURRENCY_PATTERN.matches(currency)
    val limitValid: Boolean get() = (creditLimit.toDoubleOrNull() ?: 0.0) > 0.0
    val canSubmit: Boolean get() =
        name.isNotBlank() && bank.isNotBlank() && last4Valid && currencyValid && limitValid && !isSaving
}

/** Formulario de "Nueva tarjeta". Mismo criterio que CreateCreditCardDialog en nexora-web. */
class NewCreditCardViewModel(private val creditCardRepository: CreditCardRepository) : ViewModel() {

    var uiState by mutableStateOf(NewCreditCardUiState())
        private set

    fun onNameChange(value: String) {
        uiState = uiState.copy(name = value, error = null)
    }

    fun onBankChange(value: String) {
        uiState = uiState.copy(bank = value, error = null)
    }

    fun onLast4Change(value: String) {
        uiState = uiState.copy(last4 = value.filter { it.isDigit() }.take(4), error = null)
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

    fun onCurrencyChange(value: String) {
        uiState = uiState.copy(currency = value.uppercase(), error = null)
    }

    fun submit(fallbackError: String) {
        val state = uiState
        if (!state.canSubmit) return

        uiState = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                creditCardRepository.createCreditCard(
                    CreateCreditCardRequest(
                        name = state.name.trim(),
                        bank = state.bank.trim(),
                        last4 = state.last4,
                        creditLimit = state.creditLimit.toDouble(),
                        closingDay = state.closingDay.toIntOrNull() ?: 1,
                        paymentDueDay = state.paymentDueDay.toIntOrNull() ?: 15,
                        currency = state.currency,
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
