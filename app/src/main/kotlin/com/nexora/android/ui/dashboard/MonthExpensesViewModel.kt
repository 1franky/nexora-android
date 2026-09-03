package com.nexora.android.ui.dashboard

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
import com.nexora.android.data.transaction.TransactionType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** Igual criterio que EXPENSE_TYPES en DashboardService (nexora-api): gasto = EXPENSE + CREDIT_CARD_PURCHASE. */
private val EXPENSE_TYPES = setOf(TransactionType.EXPENSE, TransactionType.CREDIT_CARD_PURCHASE)

data class MonthExpensesData(
    val transactions: List<Transaction>,
    val accountNameById: Map<String, String>,
    val categoryNameById: Map<String, String>,
)

sealed interface MonthExpensesUiState {
    data object Loading : MonthExpensesUiState
    data class Error(val message: String) : MonthExpensesUiState
    data class Success(val data: MonthExpensesData) : MonthExpensesUiState
}

/**
 * Resumen de "gastos del mes" del dashboard: las transacciones EXPENSE/
 * CREDIT_CARD_PURCHASE del mes en curso, de todas las cuentas — mismo
 * criterio que expenseThisMonth en el backend, para que el total coincida
 * con lo que ya se ve en el tile.
 */
class MonthExpensesViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    var uiState by mutableStateOf<MonthExpensesUiState>(MonthExpensesUiState.Loading)
        private set

    fun load(fallbackError: String) {
        viewModelScope.launch {
            uiState = MonthExpensesUiState.Loading
            uiState = try {
                coroutineScope {
                    val transactionsDeferred = async { transactionRepository.listTransactions(null, fallbackError) }
                    val accountsDeferred = async { accountRepository.listAccounts(fallbackError) }
                    val categoriesDeferred = async { categoryRepository.listCategories(fallbackError) }

                    val thisMonth = YearMonth.now()
                    val monthTransactions = transactionsDeferred.await()
                        .filter { it.type in EXPENSE_TYPES && it.date.inMonth(thisMonth) }
                        .sortedByDescending { it.date }
                    val accounts: List<Account> = accountsDeferred.await()
                    val categories: List<Category> = categoriesDeferred.await()

                    MonthExpensesUiState.Success(
                        MonthExpensesData(
                            transactions = monthTransactions,
                            accountNameById = accounts.associate { it.id to it.name },
                            categoryNameById = categories.associate { it.id to it.name },
                        ),
                    )
                }
            } catch (e: ApiException) {
                MonthExpensesUiState.Error(e.message ?: fallbackError)
            }
        }
    }
}

private fun String.inMonth(month: YearMonth): Boolean = try {
    YearMonth.from(LocalDate.parse(this)) == month
} catch (_: Exception) {
    false
}
