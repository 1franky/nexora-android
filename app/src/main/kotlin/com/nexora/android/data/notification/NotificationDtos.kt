package com.nexora.android.data.notification

import kotlinx.serialization.Serializable

/** Espejo de com.nexora.api.notification.web (nexora-api). */

@Serializable
enum class NotificationType { PAYMENT_DUE, PAYMENT_DUE_SOON, PAYMENT_OVERDUE, INSTALLMENT_DUE, BUDGET_EXCEEDED, UNUSUAL_EXPENSE }

@Serializable
enum class NotificationStatus { UNREAD, READ }

@Serializable
data class Notification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val relatedEntityId: String? = null,
    val status: NotificationStatus,
    val createdAt: String,
    val readAt: String? = null,
)
