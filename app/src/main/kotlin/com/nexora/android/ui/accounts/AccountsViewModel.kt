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

sealed interface AccountsUiState {
    data object Loading : AccountsUiState
    data class Error(val message: String) : AccountsUiState
    data class Success(val accounts: List<Account>) : AccountsUiState
}

class AccountsViewModel(private val accountRepository: AccountRepository) : ViewModel() {

    var uiState by mutableStateOf<AccountsUiState>(AccountsUiState.Loading)
        private set

    fun load(fallbackError: String) {
        viewModelScope.launch {
            uiState = AccountsUiState.Loading
            uiState = try {
                AccountsUiState.Success(accountRepository.listAccounts(fallbackError))
            } catch (e: ApiException) {
                AccountsUiState.Error(e.message ?: fallbackError)
            }
        }
    }

    /** Se llama tras crear una cuenta nueva desde la hoja de alta, para refrescar la lista. */
    fun refresh(fallbackError: String) = load(fallbackError)
}
