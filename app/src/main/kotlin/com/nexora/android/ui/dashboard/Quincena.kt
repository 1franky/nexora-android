package com.nexora.android.ui.dashboard

import com.nexora.android.data.dashboard.UpcomingCardPayment
import java.time.LocalDate

/**
 * La quincena en curso, hoy: del 1 al 15 del mes si [today] cae en esa mitad,
 * o del 16 al último día del mes si no — no depende de días de corte/pago de
 * ninguna tarjeta en particular, solo de la fecha de hoy.
 */
fun currentQuincena(today: LocalDate): ClosedRange<LocalDate> =
    if (today.dayOfMonth <= 15) {
        today.withDayOfMonth(1)..today.withDayOfMonth(15)
    } else {
        today.withDayOfMonth(16)..today.withDayOfMonth(today.lengthOfMonth())
    }

/**
 * De "próximo pago" de cada tarjeta (dashboard.upcomingPayments), los que
 * vencen dentro de la quincena en curso — p.ej. con corte el 5 y el 14 hoy
 * ambos entran; uno con corte el 25 no entra hasta que la quincena en curso
 * sea la segunda mitad del mes.
 */
fun UpcomingCardPayment.dueDateOrNull(): LocalDate? = try {
    LocalDate.parse(dueDate)
} catch (_: Exception) {
    null
}

fun List<UpcomingCardPayment>.inQuincena(quincena: ClosedRange<LocalDate>): List<UpcomingCardPayment> =
    filter { payment -> payment.dueDateOrNull()?.let { it in quincena } ?: false }
