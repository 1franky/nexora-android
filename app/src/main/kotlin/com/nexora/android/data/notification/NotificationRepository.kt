package com.nexora.android.data.notification

import com.nexora.android.data.common.apiCall

class NotificationRepository(private val notificationApi: NotificationApi) {
    suspend fun list(fallbackError: String): List<Notification> =
        apiCall(fallbackError) { notificationApi.list() }

    suspend fun markAsRead(id: String, fallbackError: String): Notification =
        apiCall(fallbackError) { notificationApi.markAsRead(id) }

    suspend fun markAllAsRead(fallbackError: String) =
        apiCall(fallbackError) { notificationApi.markAllAsRead() }
}
