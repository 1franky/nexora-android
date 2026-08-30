package com.nexora.android.ui.cards

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.common.ApiException
import com.nexora.android.data.installment.InstallmentPlan
import com.nexora.android.data.installment.InstallmentRepository
import com.nexora.android.data.offline.WriteOutcome
import kotlinx.coroutines.launch

sealed interface InstallmentPlansUiState {
    data object Loading : InstallmentPlansUiState
    data class Error(val message: String) : InstallmentPlansUiState
    data class Success(val plans: List<InstallmentPlan>) : InstallmentPlansUiState
}

/**
 * Planes MSI/MCI de una tarjeta (plan.md, sección 6). Mismo criterio que
 * InstallmentPlansSection en nexora-web: se cargan aparte de la tarjeta y
 * sus movimientos, con su propio estado de carga/error.
 */
class InstallmentPlansViewModel(
    private val cardId: String,
    private val installmentRepository: InstallmentRepository,
) : ViewModel() {

    var uiState by mutableStateOf<InstallmentPlansUiState>(InstallmentPlansUiState.Loading)
        private set

    /** id de la cuota que se está marcando como pagada ahora mismo, para mostrar su spinner. */
    var payingInstallmentId by mutableStateOf<String?>(null)
        private set

    /** Error de payInstallment, mostrado como banner sin perder la lista ya cargada (igual criterio que nexora-web). */
    var payError by mutableStateOf<String?>(null)
        private set

    fun load(fallbackError: String) {
        viewModelScope.launch {
            uiState = InstallmentPlansUiState.Loading
            uiState = try {
                InstallmentPlansUiState.Success(installmentRepository.listForCard(cardId, fallbackError))
            } catch (e: ApiException) {
                InstallmentPlansUiState.Error(e.message ?: fallbackError)
            }
        }
    }

    fun refresh(fallbackError: String) = load(fallbackError)

    fun payInstallment(planId: String, installmentId: String, fallbackError: String) {
        val current = uiState
        if (current !is InstallmentPlansUiState.Success || payingInstallmentId != null) return

        payingInstallmentId = installmentId
        payError = null
        viewModelScope.launch {
            try {
                when (val outcome = installmentRepository.payInstallment(planId, installmentId, fallbackError)) {
                    is WriteOutcome.Applied -> {
                        val updatedPlan = outcome.value
                        uiState = InstallmentPlansUiState.Success(
                            current.plans.map { if (it.id == updatedPlan.id) updatedPlan else it },
                        )
                    }
                    // Sin conexión: queda pendiente de sincronizar (banner global). No se sabe el
                    // saldo/estado real hasta que el backend la procese — no se toca la lista.
                    WriteOutcome.Queued -> Unit
                }
            } catch (e: ApiException) {
                payError = e.message ?: fallbackError
            } finally {
                payingInstallmentId = null
            }
        }
    }
}
