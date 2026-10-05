package com.nexora.android.data.scheduledcharge

import com.nexora.android.data.account.AccountType
import kotlinx.serialization.Serializable

/**
 * Espejo de com.nexora.api.scheduledcharge.web (nexora-api, B14): cargos
 * programados — suscripciones/domiciliaciones que el backend convierte solas
 * en un movimiento en su fecha (CREDIT_CARD_PURCHASE en tarjeta, EXPENSE en
 * débito/ahorro). Ver plan-cargos-programados.md.
 */

@Serializable
enum class ScheduledChargeFrequency { MONTHLY, YEARLY }

@Serializable
enum class ScheduledChargeStatus { ACTIVE, PAUSED, FINISHED, CANCELLED }

@Serializable
data class ScheduledCharge(
    val id: String,
    val accountId: String,
    val accountName: String,
    val accountType: AccountType,
    val currency: String,
    val name: String,
    val amount: Double,
    val categoryId: String? = null,
    val description: String? = null,
    val frequency: ScheduledChargeFrequency,
    val dayOfMonth: Int,
    val monthOfYear: Int? = null,
    val startDate: String,
    val endDate: String? = null,
    /** null cuando está FINISHED o CANCELLED. */
    val nextRunDate: String? = null,
    /** != null en cuanto generó al menos un movimiento: a partir de ahí startDate ya no se puede editar. */
    val lastRunDate: String? = null,
    val status: ScheduledChargeStatus,
    /** Motivo de una pausa automática (p. ej. categoría archivada). Se limpia al reanudar. */
    val lastError: String? = null,
    val createdAt: String,
)

/** Mismo cuerpo para alta (POST) y edición (PUT). */
@Serializable
data class ScheduledChargeRequest(
    val accountId: String,
    val name: String,
    val amount: Double,
    val categoryId: String? = null,
    val description: String? = null,
    val frequency: ScheduledChargeFrequency,
    val dayOfMonth: Int,
    /** Obligatorio si [frequency] = YEARLY; null si MONTHLY (el backend rechaza lo contrario). */
    val monthOfYear: Int? = null,
    val startDate: String,
    val endDate: String? = null,
)
