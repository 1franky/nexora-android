package com.nexora.android.ui.common

import java.text.NumberFormat
import java.time.LocalDate
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
