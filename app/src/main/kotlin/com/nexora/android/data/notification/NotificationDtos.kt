package com.nexora.android.data.notification

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Espejo de com.nexora.api.notification.web (nexora-api). */

@Serializable(with = NotificationTypeSerializer::class)
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

    /** B14 (nexora-api): cargo programado registrado / pausado por error. relatedEntityId = id del cargo. */
    SCHEDULED_CHARGE_POSTED, SCHEDULED_CHARGE_FAILED,

    /**
     * Cualquier tipo que el backend agregue en el futuro y esta versión de la app no conozca
     * (ver [NotificationTypeSerializer]) — para que el crash de 1.5.2 no se repita con cada
     * tipo nuevo: la notificación se muestra igual (título y mensaje vienen del backend) con
     * un ícono genérico.
     */
    UNKNOWN,
}

/** Deserializa un tipo desconocido como [NotificationType.UNKNOWN] en vez de lanzar SerializationException. */
object NotificationTypeSerializer : KSerializer<NotificationType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.nexora.android.data.notification.NotificationType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: NotificationType) = encoder.encodeString(value.name)

    override fun deserialize(decoder: Decoder): NotificationType {
        val raw = decoder.decodeString()
        return NotificationType.entries.firstOrNull { it.name == raw } ?: NotificationType.UNKNOWN
    }
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
