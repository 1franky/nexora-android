package com.nexora.android.ui.transactions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.account.Account
import com.nexora.android.data.account.AccountRepository
import com.nexora.android.data.category.Category
import com.nexora.android.data.category.CategoryRepository
import com.nexora.android.data.common.ApiException
import com.nexora.android.data.transaction.Transaction
import com.nexora.android.data.transaction.TransactionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

sealed interface TransactionsUiState {
    data object Loading : TransactionsUiState
    data class Error(val message: String) : TransactionsUiState
    data class Success(
        val transactions: List<Transaction>,
        val accounts: List<Account>,
        val categories: List<Category>,
        val selectedAccountId: String?,
    ) : TransactionsUiState
}

/**
 * Selector de cuenta ("todas" por defecto) sobre el mismo endpoint que ahora
 * acepta accountId opcional — igual criterio que TransactionsPage en
 * nexora-web tras el issue #6.
 */
class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private var accounts: List<Account> = emptyList()
    private var categories: List<Category> = emptyList()
    var selectedAccountId: String? = null
        private set

    var uiState by mutableStateOf<TransactionsUiState>(TransactionsUiState.Loading)
        private set

    /** Movimiento pendiente de confirmar borrado (null = el diálogo de confirmación está cerrado). */
    var pendingDelete by mutableStateOf<Transaction?>(null)
        private set
    var isDeleting by mutableStateOf(false)
        private set
    var deleteError by mutableStateOf<String?>(null)
        private set

    fun requestDelete(transaction: Transaction) {
        pendingDelete = transaction
        deleteError = null
    }

    fun cancelDelete() {
        pendingDelete = null
        deleteError = null
    }

    fun confirmDelete(fallbackError: String) {
        val transaction = pendingDelete ?: return
        isDeleting = true
        deleteError = null
        viewModelScope.launch {
            try {
                transactionRepository.deleteTransaction(transaction.id, fallbackError)
                isDeleting = false
                pendingDelete = null
                refresh(fallbackError)
            } catch (e: ApiException) {
                isDeleting = false
                deleteError = e.message ?: fallbackError
            }
        }
    }

    fun load(fallbackError: String) {
        viewModelScope.launch {
            uiState = TransactionsUiState.Loading
            uiState = try {
                coroutineScope {
                    val accountsDeferred = async { accountRepository.listAccounts(fallbackError) }
                    val categoriesDeferred = async { categoryRepository.listCategories(fallbackError) }
                    val transactionsDeferred = async { transactionRepository.listTransactions(selectedAccountId, fallbackError) }
                    accounts = accountsDeferred.await()
                    categories = categoriesDeferred.await()
                    TransactionsUiState.Success(transactionsDeferred.await(), accounts, categories, selectedAccountId)
                }
            } catch (e: ApiException) {
                TransactionsUiState.Error(e.message ?: fallbackError)
            }
        }
    }

    fun selectAccount(accountId: String?, fallbackError: String) {
        selectedAccountId = accountId
        viewModelScope.launch {
            uiState = try {
                val transactions = transactionRepository.listTransactions(accountId, fallbackError)
                TransactionsUiState.Success(transactions, accounts, categories, accountId)
            } catch (e: ApiException) {
                TransactionsUiState.Error(e.message ?: fallbackError)
            }
        }
    }

    /** Se llama tras guardar un movimiento nuevo desde la hoja de alta, para refrescar la lista con el filtro actual. */
    fun refresh(fallbackError: String) = selectAccount(selectedAccountId, fallbackError)
}
