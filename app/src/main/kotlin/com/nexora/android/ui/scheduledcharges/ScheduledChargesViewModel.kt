package com.nexora.android.ui.scheduledcharges

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
import com.nexora.android.data.scheduledcharge.ScheduledCharge
import com.nexora.android.data.scheduledcharge.ScheduledChargeRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

sealed interface ScheduledChargesUiState {
    data object Loading : ScheduledChargesUiState
    data class Error(val message: String) : ScheduledChargesUiState
    data class Success(
        val charges: List<ScheduledCharge>,
        /** Catálogos para el formulario de alta/edición (cuentas elegibles y categorías de gasto). */
        val accounts: List<Account>,
        val categories: List<Category>,
    ) : ScheduledChargesUiState
}

/**
 * Lista de cargos programados — de todo el usuario o de una cuenta (detalle de
 * tarjeta/cuenta). Pausar/reanudar/cancelar no se encolan sin conexión (ver
 * ScheduledChargeRepository): un error se muestra como banner sin perder la
 * lista ya cargada, igual que el pago de cuotas en InstallmentPlansViewModel.
 */
class ScheduledChargesViewModel(
    private val scheduledChargeRepository: ScheduledChargeRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    var uiState by mutableStateOf<ScheduledChargesUiState>(ScheduledChargesUiState.Loading)
        private set

    /** id del cargo con una acción (pausar/reanudar/cancelar) en curso, para su spinner. */
    var busyChargeId by mutableStateOf<String?>(null)
        private set

    var actionError by mutableStateOf<String?>(null)
        private set

    private var accountId: String? = null

    fun load(accountId: String?, fallbackError: String) {
        this.accountId = accountId
        viewModelScope.launch {
            uiState = ScheduledChargesUiState.Loading
            uiState = fetch(fallbackError)
        }
    }

    /** Recarga sin pasar por Loading si ya había datos (tras guardar o tras una acción). */
    fun refresh(fallbackError: String) {
        viewModelScope.launch {
            val result = fetch(fallbackError)
            if (result is ScheduledChargesUiState.Success || uiState !is ScheduledChargesUiState.Success) uiState = result
        }
    }

    private suspend fun fetch(fallbackError: String): ScheduledChargesUiState = try {
        coroutineScope {
            val charges = async { scheduledChargeRepository.list(accountId, fallbackError) }
            val accounts = async { accountRepository.listAccounts(fallbackError) }
            val categories = async { categoryRepository.listCategories(fallbackError) }
            ScheduledChargesUiState.Success(charges.await(), accounts.await(), categories.await())
        }
    } catch (e: ApiException) {
        ScheduledChargesUiState.Error(e.message ?: fallbackError)
    }

    /** [onDone] se llama solo si la acción tuvo éxito (p. ej. para refrescar los movimientos de la tarjeta). */
    fun pause(id: String, fallbackError: String, listFallbackError: String, onDone: () -> Unit = {}) =
        runAction(id, fallbackError, listFallbackError, onDone) { scheduledChargeRepository.pause(id, fallbackError) }

    /** Reanudar puede registrar de inmediato el cargo de hoy si toca hoy. */
    fun resume(id: String, fallbackError: String, listFallbackError: String, onDone: () -> Unit = {}) =
        runAction(id, fallbackError, listFallbackError, onDone) { scheduledChargeRepository.resume(id, fallbackError) }

    fun cancel(id: String, fallbackError: String, listFallbackError: String, onDone: () -> Unit = {}) =
        runAction(id, fallbackError, listFallbackError, onDone) { scheduledChargeRepository.cancel(id, fallbackError) }

    fun dismissActionError() {
        actionError = null
    }

    private fun runAction(
        id: String,
        fallbackError: String,
        listFallbackError: String,
        onDone: () -> Unit,
        action: suspend () -> Unit,
    ) {
        if (busyChargeId != null) return
        busyChargeId = id
        actionError = null
        viewModelScope.launch {
            try {
                action()
                onDone()
                // Se recarga la lista entera en vez de reemplazar el elemento: el orden (por
                // próximo cargo, terminados/cancelados al final) lo decide el backend.
                val result = fetch(listFallbackError)
                if (result is ScheduledChargesUiState.Success) uiState = result
            } catch (e: ApiException) {
                actionError = e.message ?: fallbackError
            } finally {
                busyChargeId = null
            }
        }
    }
}
