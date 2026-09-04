package com.nexora.android.ui.common

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Mismo criterio que formatCurrency en nexora-web/dataviz/format.ts: es-MX, sin decimales.
private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX")).apply {
    maximumFractionDigits = 0
}

fun formatCurrency(value: Double): String = currencyFormatter.format(value)

private val dateShortFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-MX"))

/** "2026-08-29" -> "29 ago" — mismo criterio que formatDateShort en nexora-web. */
fun formatDateShort(isoDate: String): String = try {
    LocalDate.parse(isoDate).format(dateShortFormatter).replace(".", "")
} catch (_: Exception) {
    isoDate
}

private val dateTimeShortFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("es-MX"))

/**
 * "2026-08-29T10:15:00Z" -> "29 ago 2026, 10:15" (A12, respuestas del SAT:
 * validUntil/lastSyncAt). Tolera tanto un instante con zona/offset como un
 * date-time "naive" sin zona (formato distinto que puede mandar el backend
 * según el campo) — se muestra en la hora local del dispositivo.
 */
fun formatDateTimeShort(isoDateTime: String): String = try {
    val instant = try {
        Instant.parse(isoDateTime)
    } catch (_: Exception) {
        LocalDateTime.parse(isoDateTime).atZone(ZoneId.systemDefault()).toInstant()
    }
    instant.atZone(ZoneId.systemDefault()).format(dateTimeShortFormatter).replace(".", "")
} catch (_: Exception) {
    isoDateTime
}
