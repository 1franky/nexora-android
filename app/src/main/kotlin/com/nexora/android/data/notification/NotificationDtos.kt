package com.nexora.android.data.notification

import kotlinx.serialization.Serializable

/** Espejo de com.nexora.api.notification.web (nexora-api). */

@Serializable
enum class NotificationType {
    PAYMENT_DUE, PAYMENT_DUE_SOON, PAYMENT_OVERDUE, INSTALLMENT_DUE, BUDGET_EXCEEDED, UNUSUAL_EXPENSE,
    /**
     * B11 (nexora-api): [com.nexora.api.notification.domain.NotificationService] no las
     * genera, las crea SatSyncService al terminar una sincronización con el SAT. Faltaban
     * aquí: kotlinx.serialization no tolera un valor de enum desconocido (a diferencia de
     * nexora-web, que solo compara strings en runtime) y GET /notifications tumbaba la app
     * en cuanto el usuario tenía una de estas en la lista — p.ej. justo después de vincular
     * el SAT.
     */
    SAT_SYNC_COMPLETED, SAT_SYNC_FAILED,
}

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
