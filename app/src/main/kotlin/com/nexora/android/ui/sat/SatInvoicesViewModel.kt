package com.nexora.android.ui.sat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.common.ApiException
import com.nexora.android.data.sat.CfdiInvoiceResponse
import com.nexora.android.data.sat.CfdiTipo
import com.nexora.android.data.sat.SatRepository
import java.time.LocalDate
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 25

sealed interface SatInvoicesUiState {
    data object Loading : SatInvoicesUiState
    data class Error(val message: String) : SatInvoicesUiState
    data class Success(
        val invoices: List<CfdiInvoiceResponse>,
        val page: Int,
        val totalPages: Int,
        val isLoadingMore: Boolean = false,
    ) : SatInvoicesUiState {
        val hasMore: Boolean get() = page + 1 < totalPages
    }
}

/**
 * Listado de facturas SAT con filtros (A12, plan-integracion-sat.md sección
 * 9): tipo/rango de fechas/texto libre (RFC, nombre o UUID fiscal) + carga
 * incremental por página ("Cargar más"). El caché offline vive en
 * SatRepository.listInvoices (mismo patrón de A8 que el resto de listados).
 */
class SatInvoicesViewModel(private val satRepository: SatRepository) : ViewModel() {

    var uiState by mutableStateOf<SatInvoicesUiState>(SatInvoicesUiState.Loading)
        private set

    var tipo by mutableStateOf<CfdiTipo?>(null)
        private set
    var desde by mutableStateOf<LocalDate?>(null)
        private set
    var hasta by mutableStateOf<LocalDate?>(null)
        private set
    var texto by mutableStateOf("")
        private set

    fun onTipoChange(value: CfdiTipo?) {
        tipo = value
    }

    fun onDesdeChange(value: LocalDate?) {
        desde = value
    }

    fun onHastaChange(value: LocalDate?) {
        hasta = value
    }

    fun onTextoChange(value: String) {
        texto = value
    }

    /** Vuelve a la página 0 con los filtros actuales — se llama al cambiar cualquier filtro o tocar "Buscar". */
    fun search(fallbackError: String) = load(page = 0, fallbackError = fallbackError)

    fun loadMore(fallbackError: String) {
        val current = uiState as? SatInvoicesUiState.Success ?: return
        if (!current.hasMore || current.isLoadingMore) return
        load(page = current.page + 1, fallbackError = fallbackError)
    }

    private fun load(page: Int, fallbackError: String) {
        viewModelScope.launch {
            uiState = when (val current = uiState) {
                is SatInvoicesUiState.Success -> if (page > 0) current.copy(isLoadingMore = true) else SatInvoicesUiState.Loading
                else -> SatInvoicesUiState.Loading
            }
            val previousInvoices = (uiState as? SatInvoicesUiState.Success)?.invoices.orEmpty()
            uiState = try {
                val result = satRepository.listInvoices(
                    tipo = tipo,
                    desde = desde?.let { isoStartOfDay(it) },
                    hasta = hasta?.let { isoEndOfDay(it) },
                    texto = texto.trim().ifBlank { null },
                    page = page,
                    size = PAGE_SIZE,
                    fallbackError = fallbackError,
                )
                val invoices = if (page > 0) previousInvoices + result.content else result.content
                SatInvoicesUiState.Success(invoices, result.number, result.totalPages)
            } catch (e: ApiException) {
                if (page > 0) {
                    SatInvoicesUiState.Success(previousInvoices, page - 1, (uiState as? SatInvoicesUiState.Success)?.totalPages ?: page)
                } else {
                    SatInvoicesUiState.Error(e.message ?: fallbackError)
                }
            }
        }
    }

    suspend fun downloadXml(invoiceId: String, fallbackError: String): ByteArray =
        satRepository.downloadXml(invoiceId, fallbackError)
}
