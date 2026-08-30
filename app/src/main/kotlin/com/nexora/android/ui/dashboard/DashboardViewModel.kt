package com.nexora.android.ui.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.auth.AuthRepository
import com.nexora.android.data.common.ApiException
import com.nexora.android.data.dashboard.DashboardRepository
import com.nexora.android.data.dashboard.DashboardResponse
import com.nexora.android.data.user.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

data class DashboardData(val displayName: String, val dashboard: DashboardResponse)

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Error(val message: String) : DashboardUiState
    data class Success(val data: DashboardData) : DashboardUiState
}

class DashboardViewModel(
    private val dashboardRepository: DashboardRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    var uiState by mutableStateOf<DashboardUiState>(DashboardUiState.Loading)
        private set

    fun load(fallbackError: String) {
        viewModelScope.launch {
            uiState = DashboardUiState.Loading
            uiState = try {
                coroutineScope {
                    val userDeferred = async { userRepository.getCurrentUser(fallbackError) }
                    val dashboardDeferred = async { dashboardRepository.getDashboard(fallbackError) }
                    DashboardUiState.Success(DashboardData(userDeferred.await().displayName, dashboardDeferred.await()))
                }
            } catch (e: ApiException) {
                DashboardUiState.Error(e.message ?: fallbackError)
            }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
