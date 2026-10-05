package com.nexora.android.ui.scheduledcharges

import com.nexora.android.data.scheduledcharge.ScheduledChargeFrequency
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Réplica en cliente de ScheduleCalculator (nexora-api, B14) — solo para
 * mostrar al usuario, antes de guardar, la fecha de inicio sugerida y cuántos
 * cargos atrasados se registrarán. El cálculo real (y el que cuenta) sigue
 * siendo el del backend; si cambia allá, hay que cambiarlo aquí.
 *
 * Si el mes no tiene el día configurado (31 en abril, 29-31 en febrero), el
 * cargo cae en el último día de ese mes — el día configurado no cambia: en
 * mayo vuelve al 31.
 */
object ScheduleRules {

    /** Tope de iteraciones para [overdueCount], por si el usuario elige una fecha absurda (año 1900). */
    private const val MAX_OVERDUE = 10_000

    /** Primera ocurrencia de la regla que cae en [date] o después. */
    fun firstOnOrAfter(
        frequency: ScheduledChargeFrequency,
        dayOfMonth: Int,
        monthOfYear: Int?,
        date: LocalDate,
    ): LocalDate = when (frequency) {
        ScheduledChargeFrequency.MONTHLY -> {
            val candidate = clampedDate(YearMonth.from(date), dayOfMonth)
            if (candidate >= date) candidate else clampedDate(YearMonth.from(date).plusMonths(1), dayOfMonth)
        }
        ScheduledChargeFrequency.YEARLY -> {
            val month = requireNotNull(monthOfYear) { "monthOfYear es obligatorio para un cargo anual." }
            val candidate = clampedDate(YearMonth.of(date.year, month), dayOfMonth)
            if (candidate >= date) candidate else clampedDate(YearMonth.of(date.year + 1, month), dayOfMonth)
        }
    }

    /** Siguiente ocurrencia estrictamente posterior a [date]. */
    fun nextAfter(
        frequency: ScheduledChargeFrequency,
        dayOfMonth: Int,
        monthOfYear: Int?,
        date: LocalDate,
    ): LocalDate = firstOnOrAfter(frequency, dayOfMonth, monthOfYear, date.plusDays(1))

    /**
     * Cuántos cargos registraría el backend de inmediato al dar de alta un cargo con
     * [startDate] (ScheduledChargeService.postDueOccurrences): las ocurrencias desde la primera
     * ≥ [startDate] hasta [today] inclusive, sin pasar de [endDate] (inclusive).
     */
    fun overdueCount(
        frequency: ScheduledChargeFrequency,
        dayOfMonth: Int,
        monthOfYear: Int?,
        startDate: LocalDate,
        endDate: LocalDate?,
        today: LocalDate,
    ): Int {
        var count = 0
        var date = firstOnOrAfter(frequency, dayOfMonth, monthOfYear, startDate)
        while (date <= today && (endDate == null || date <= endDate) && count < MAX_OVERDUE) {
            count++
            date = nextAfter(frequency, dayOfMonth, monthOfYear, date)
        }
        return count
    }

    private fun clampedDate(yearMonth: YearMonth, dayOfMonth: Int): LocalDate =
        yearMonth.atDay(minOf(dayOfMonth, yearMonth.lengthOfMonth()))
}

private val SPANISH_MX: Locale = Locale.forLanguageTag("es-MX")

/** 3 -> "marzo". Mismo locale fijo que Formatters (la app solo está en español por ahora). */
fun monthName(monthOfYear: Int, locale: Locale = SPANISH_MX): String =
    Month.of(monthOfYear).getDisplayName(TextStyle.FULL_STANDALONE, locale).lowercase(locale)

/**
 * «Cada día 15» / «Cada 3 de marzo». Las plantillas llegan ya resueltas de strings.xml
 * (scheduled_charges_frequency_monthly_desc / _yearly_desc) para poder probar esto sin Context.
 */
fun frequencyDescription(
    frequency: ScheduledChargeFrequency,
    dayOfMonth: Int,
    monthOfYear: Int?,
    monthlyTemplate: String,
    yearlyTemplate: String,
): String = when {
    frequency == ScheduledChargeFrequency.YEARLY && monthOfYear != null ->
        String.format(SPANISH_MX, yearlyTemplate, dayOfMonth, monthName(monthOfYear))
    else -> String.format(SPANISH_MX, monthlyTemplate, dayOfMonth)
}
