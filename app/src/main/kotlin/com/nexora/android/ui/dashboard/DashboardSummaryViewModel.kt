package com.nexora.android.ui.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.common.ApiException
import com.nexora.android.data.dashboard.DashboardRepository
import com.nexora.android.data.dashboard.DashboardResponse
import kotlinx.coroutines.launch

sealed interface DashboardSummaryUiState {
    data object Loading : DashboardSummaryUiState
    data class Error(val message: String) : DashboardSummaryUiState
    data class Success(val dashboard: DashboardResponse) : DashboardSummaryUiState
}

/**
 * ViewModel liviano compartido por las pantallas de resumen que cuelgan del
 * dashboard (Próximo pago, Quincena): ambas solo necesitan releer el mismo
 * DashboardResponse ya cacheado (ver DashboardRepository/OfflineCache), sin
 * el join con UserRepository que sí necesita el propio Dashboard.
 */
class DashboardSummaryViewModel(private val dashboardRepository: DashboardRepository) : ViewModel() {

    var uiState by mutableStateOf<DashboardSummaryUiState>(DashboardSummaryUiState.Loading)
        private set

    fun load(fallbackError: String) {
        viewModelScope.launch {
            uiState = DashboardSummaryUiState.Loading
            uiState = try {
                DashboardSummaryUiState.Success(dashboardRepository.getDashboard(fallbackError))
            } catch (e: ApiException) {
                DashboardSummaryUiState.Error(e.message ?: fallbackError)
            }
        }
    }
}
