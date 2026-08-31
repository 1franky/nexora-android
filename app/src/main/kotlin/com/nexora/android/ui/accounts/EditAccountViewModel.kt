package com.nexora.android.ui.accounts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.account.Account
import com.nexora.android.data.account.AccountRepository
import com.nexora.android.data.common.ApiException
import kotlinx.coroutines.launch

data class EditAccountUiState(
    val name: String,
    val includeInAvailableBalance: Boolean,
    val includeInNetWorth: Boolean,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
) {
    val canSubmit: Boolean get() = name.isNotBlank() && !isSaving
}

/** Tipo, moneda y saldo no se editan aquí a propósito (ver nexora-api: cambiarlos rompería el significado de lo ya registrado). */
class EditAccountViewModel(private val accountRepository: AccountRepository, account: Account) : ViewModel() {

    private val accountId = account.id

    var uiState by mutableStateOf(
        EditAccountUiState(
            name = account.name,
            includeInAvailableBalance = account.includeInAvailableBalance,
            includeInNetWorth = account.includeInNetWorth,
        ),
    )
        private set

    fun onNameChange(value: String) {
        uiState = uiState.copy(name = value, error = null)
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
                accountRepository.updateAccount(
                    id = accountId,
                    name = state.name.trim(),
                    includeInAvailableBalance = state.includeInAvailableBalance,
                    includeInNetWorth = state.includeInNetWorth,
                    fallbackError = fallbackError,
                )
                uiState = uiState.copy(isSaving = false, saved = true)
            } catch (e: ApiException) {
                uiState = uiState.copy(isSaving = false, error = e.message ?: fallbackError)
            }
        }
    }
}
