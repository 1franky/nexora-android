package com.nexora.android.data.sat

import kotlinx.serialization.Serializable

/** Espejo de com.nexora.api.sat.web (nexora-api) — ver plan-integracion-sat.md, sección 7.4. */

@Serializable
enum class SatCertificateStatus { ACTIVO, ERROR_AUTENTICACION, REVOCADO }

@Serializable
data class SatCertificateResponse(
    val rfc: String,
    val status: SatCertificateStatus,
    val validUntil: String,
    val lastSyncAt: String? = null,
)

@Serializable
enum class CfdiTipo { EMITIDAS, RECIBIDAS }

@Serializable
enum class CfdiEstadoSat { VIGENTE, CANCELADO }

@Serializable
data class CfdiInvoiceResponse(
    val id: String,
    val uuidFiscal: String,
    val tipo: CfdiTipo,
    val rfcEmisor: String,
    val nombreEmisor: String? = null,
    val rfcReceptor: String,
    val nombreReceptor: String? = null,
    val fechaEmision: String,
    val subtotal: Double,
    val iva: Double,
    val total: Double,
    val moneda: String,
    val formaPago: String? = null,
    val metodoPago: String? = null,
    val usoCfdi: String? = null,
    val estadoSat: CfdiEstadoSat,
)

/**
 * Espejo parcial de un `Page` estándar de Spring Data: solo los campos que la
 * app usa (contenido + info de paginación para el botón "Cargar más"). El
 * resto de campos que trae un Page real (sort, pageable, etc.) se ignoran
 * gracias a `ignoreUnknownKeys` (ver AppContainer.json).
 */
@Serializable
data class PageCfdiInvoiceResponse(
    val content: List<CfdiInvoiceResponse>,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val number: Int = 0,
    val size: Int = 0,
)

/** Body opcional de POST /sat/sync — sin campos = incremental; con ambos = rango explícito (sección 6.1 del plan). */
@Serializable
data class SyncRequest(val desde: String? = null, val hasta: String? = null)

/**
 * RFC de un tercero que le factura al usuario (empleador, proveedor, etc.).
 * El SAT exige el RFC específico del emisor para descargar CFDI RECIBIDAS —
 * no hay forma de pedir "todo lo que me han facturado" en una sola solicitud
 * (B12/A13, ver plan-integracion-sat.md).
 */
@Serializable
data class SatContraparteResponse(
    val id: String,
    val rfc: String,
    val alias: String? = null,
)

@Serializable
data class CreateSatContraparteRequest(
    val rfc: String,
    val alias: String? = null,
)
