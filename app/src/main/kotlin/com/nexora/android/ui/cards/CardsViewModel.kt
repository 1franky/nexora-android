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

sealed interface CardsUiState {
    data object Loading : CardsUiState
    data class Error(val message: String) : CardsUiState
    data class Success(val cards: List<CreditCard>) : CardsUiState
}

class CardsViewModel(private val creditCardRepository: CreditCardRepository) : ViewModel() {

    var uiState by mutableStateOf<CardsUiState>(CardsUiState.Loading)
        private set

    fun load(fallbackError: String) {
        viewModelScope.launch {
            uiState = CardsUiState.Loading
            uiState = try {
                CardsUiState.Success(creditCardRepository.listCreditCards(fallbackError))
            } catch (e: ApiException) {
                CardsUiState.Error(e.message ?: fallbackError)
            }
        }
    }

    /** Se llama tras crear una tarjeta nueva desde la hoja de alta, para refrescar la lista. */
    fun refresh(fallbackError: String) = load(fallbackError)
}
