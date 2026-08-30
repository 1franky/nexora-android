package com.nexora.android.ui.common

import java.text.NumberFormat
import java.util.Locale

// Mismo criterio que formatCurrency en nexora-web/dataviz/format.ts: es-MX, sin decimales.
private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX")).apply {
    maximumFractionDigits = 0
}

fun formatCurrency(value: Double): String = currencyFormatter.format(value)
