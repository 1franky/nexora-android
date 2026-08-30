package com.nexora.android.ui.cards

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
import com.nexora.android.data.creditcard.CreditCard
import com.nexora.android.data.creditcard.CreditCardRepository
import com.nexora.android.data.transaction.Transaction
import com.nexora.android.data.transaction.TransactionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

sealed interface CardDetailUiState {
    data object Loading : CardDetailUiState
    data class Error(val message: String) : CardDetailUiState
    data class Success(
        val card: CreditCard,
        val transactions: List<Transaction>,
        val categories: List<Category>,
        val accounts: List<Account>,
    ) : CardDetailUiState
}

/**
 * Detalle de una tarjeta: la tarjeta misma + sus movimientos (mismo criterio
 * que CreditCardDetailPage en nexora-web — los movimientos de la tarjeta son
 * los de su cuenta asociada, card.accountId) + catálogos para las hojas de
 * compra/pago (categorías de gasto, cuentas elegibles para pagar).
 */
class CardDetailViewModel(
    private val cardId: String,
    private val creditCardRepository: CreditCardRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    var uiState by mutableStateOf<CardDetailUiState>(CardDetailUiState.Loading)
        private set

    fun load(fallbackError: String) {
        viewModelScope.launch {
            uiState = CardDetailUiState.Loading
            uiState = try {
                coroutineScope {
                    val cardDeferred = async { creditCardRepository.getCreditCard(cardId, fallbackError) }
                    val categoriesDeferred = async { categoryRepository.listCategories(fallbackError) }
                    val accountsDeferred = async { accountRepository.listAccounts(fallbackError) }
                    val card = cardDeferred.await()
                    val transactions = transactionRepository.listTransactions(card.accountId, fallbackError)
                    CardDetailUiState.Success(card, transactions, categoriesDeferred.await(), accountsDeferred.await())
                }
            } catch (e: ApiException) {
                CardDetailUiState.Error(e.message ?: fallbackError)
            }
        }
    }

    /** Se llama tras registrar una compra o un pago, para refrescar deuda/disponible y la lista. */
    fun refresh(fallbackError: String) = load(fallbackError)
}
