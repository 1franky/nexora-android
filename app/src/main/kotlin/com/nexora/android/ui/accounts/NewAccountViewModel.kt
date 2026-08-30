package com.nexora.android.ui.accounts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.account.AccountRepository
import com.nexora.android.data.account.AccountType
import com.nexora.android.data.account.CreateAccountRequest
import com.nexora.android.data.common.ApiException
import kotlinx.coroutines.launch

private val CURRENCY_PATTERN = Regex("^[A-Z]{3}$")

data class NewAccountUiState(
    val name: String = "",
    val type: AccountType = AccountType.DEBIT,
    val currency: String = "MXN",
    val openingBalance: String = "0",
    val includeInAvailableBalance: Boolean = true,
    val includeInNetWorth: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
) {
    val currencyValid: Boolean get() = CURRENCY_PATTERN.matches(currency)
    val canSubmit: Boolean get() = name.isNotBlank() && currencyValid && !isSaving
}

/**
 * Formulario de "Nueva cuenta". Mismo criterio que CreateAccountDialog en
 * nexora-web: nexora-api valida las reglas de negocio, este formulario solo
 * arma la petición.
 */
class NewAccountViewModel(private val accountRepository: AccountRepository) : ViewModel() {

    var uiState by mutableStateOf(NewAccountUiState())
        private set

    fun onNameChange(value: String) {
        uiState = uiState.copy(name = value, error = null)
    }

    fun onTypeChange(value: AccountType) {
        uiState = uiState.copy(type = value)
    }

    fun onCurrencyChange(value: String) {
        uiState = uiState.copy(currency = value.uppercase(), error = null)
    }

    fun onOpeningBalanceChange(value: String) {
        uiState = uiState.copy(openingBalance = value)
    }

    fun onIncludeInAvailableBalanceChange(value: Boolean) {
        uiState = uiState.copy(includeInAvailableBalance = value)
    }

    fun onIncludeInNetWorthChange(value: Boolean) {
        uiState = uiState.copy(includeInNetWorth = value)
    }

    fun submit(fallbackError: String) {
        val state = uiState
        if (!state.canSubmit) return

        uiState = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                accountRepository.createAccount(
                    CreateAccountRequest(
                        name = state.name.trim(),
                        type = state.type,
                        currency = state.currency,
                        openingBalance = state.openingBalance.toDoubleOrNull() ?: 0.0,
                        includeInAvailableBalance = state.includeInAvailableBalance,
                        includeInNetWorth = state.includeInNetWorth,
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
