package com.nexora.android.ui.notifications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.common.ApiException
import com.nexora.android.data.notification.Notification
import com.nexora.android.data.notification.NotificationRepository
import kotlinx.coroutines.launch

sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState
    data class Error(val message: String) : NotificationsUiState
    data class Success(val notifications: List<Notification>) : NotificationsUiState
}

class NotificationsViewModel(private val notificationRepository: NotificationRepository) : ViewModel() {

    var uiState by mutableStateOf<NotificationsUiState>(NotificationsUiState.Loading)
        private set

    fun load(fallbackError: String) {
        viewModelScope.launch {
            uiState = NotificationsUiState.Loading
            uiState = try {
                NotificationsUiState.Success(notificationRepository.list(fallbackError))
            } catch (e: ApiException) {
                NotificationsUiState.Error(e.message ?: fallbackError)
            }
        }
    }

    /**
     * Marca como leída al tocarla (criterio propio de Android; en nexora-web
     * hay un botón explícito por fila). Actualiza la fila en memoria sin
     * recargar toda la lista; si falla, se ignora — el usuario puede volver
     * a tocarla.
     */
    fun markAsRead(id: String, fallbackError: String) {
        val current = uiState
        if (current !is NotificationsUiState.Success) return
        viewModelScope.launch {
            try {
                val updated = notificationRepository.markAsRead(id, fallbackError)
                uiState = NotificationsUiState.Success(current.notifications.map { if (it.id == updated.id) updated else it })
            } catch (_: ApiException) {
                // Silencioso: no hay superficie de error para acciones secundarias en este patrón (ver CardsViewModel).
            }
        }
    }

    fun markAllAsRead(fallbackError: String) {
        val current = uiState
        if (current !is NotificationsUiState.Success) return
        viewModelScope.launch {
            try {
                notificationRepository.markAllAsRead(fallbackError)
                load(fallbackError)
            } catch (_: ApiException) {
                // Silencioso, mismo criterio que markAsRead.
            }
        }
    }
}
