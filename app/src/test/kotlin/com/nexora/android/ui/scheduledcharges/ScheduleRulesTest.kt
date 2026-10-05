package com.nexora.android.ui.scheduledcharges

import com.nexora.android.data.scheduledcharge.ScheduledChargeFrequency.MONTHLY
import com.nexora.android.data.scheduledcharge.ScheduledChargeFrequency.YEARLY
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Mismos casos que ScheduleCalculatorTests en nexora-api (B14): esta réplica debe dar lo mismo. */
class ScheduleRulesTest {

    private fun d(iso: String) = LocalDate.parse(iso)

    @Test
    fun `mensual el mismo dia cuenta como primera ocurrencia`() {
        assertEquals(d("2026-10-15"), ScheduleRules.firstOnOrAfter(MONTHLY, 15, null, d("2026-10-15")))
    }

    @Test
    fun `mensual pasado el dia salta al mes siguiente`() {
        assertEquals(d("2026-11-15"), ScheduleRules.firstOnOrAfter(MONTHLY, 15, null, d("2026-10-16")))
    }

    @Test
    fun `dia 31 cae en el ultimo dia de meses cortos y vuelve al 31 despues`() {
        assertEquals(d("2026-04-30"), ScheduleRules.firstOnOrAfter(MONTHLY, 31, null, d("2026-04-01")))
        assertEquals(d("2026-05-31"), ScheduleRules.nextAfter(MONTHLY, 31, null, d("2026-04-30")))
    }

    @Test
    fun `dia 31 en febrero bisiesto y no bisiesto`() {
        assertEquals(d("2027-02-28"), ScheduleRules.firstOnOrAfter(MONTHLY, 31, null, d("2027-02-01")))
        assertEquals(d("2028-02-29"), ScheduleRules.firstOnOrAfter(MONTHLY, 31, null, d("2028-02-01")))
    }

    @Test
    fun `mensual cruza de diciembre a enero`() {
        assertEquals(d("2027-01-05"), ScheduleRules.nextAfter(MONTHLY, 5, null, d("2026-12-05")))
    }

    @Test
    fun `anual 29 de febrero cae el 28 en anos no bisiestos`() {
        assertEquals(d("2027-02-28"), ScheduleRules.firstOnOrAfter(YEARLY, 29, 2, d("2026-03-01")))
        assertEquals(d("2028-02-29"), ScheduleRules.nextAfter(YEARLY, 29, 2, d("2027-02-28")))
    }

    @Test
    fun `anual pasado el mes salta al ano siguiente`() {
        assertEquals(d("2027-03-03"), ScheduleRules.firstOnOrAfter(YEARLY, 3, 3, d("2026-03-04")))
        assertEquals(d("2026-03-03"), ScheduleRules.firstOnOrAfter(YEARLY, 3, 3, d("2026-01-10")))
    }

    @Test
    fun `atrasados con inicio pasado cuentan hasta hoy inclusive`() {
        // 15-jul, 15-ago, 15-sep (hoy es 15-sep): 3 cargos.
        assertEquals(3, ScheduleRules.overdueCount(MONTHLY, 15, null, d("2026-07-01"), null, d("2026-09-15")))
        // Un día antes del tercer cargo: 2.
        assertEquals(2, ScheduleRules.overdueCount(MONTHLY, 15, null, d("2026-07-01"), null, d("2026-09-14")))
    }

    @Test
    fun `atrasados respetan la fecha de fin inclusive`() {
        assertEquals(2, ScheduleRules.overdueCount(MONTHLY, 15, null, d("2026-07-01"), d("2026-08-15"), d("2026-10-05")))
        assertEquals(1, ScheduleRules.overdueCount(MONTHLY, 15, null, d("2026-07-01"), d("2026-08-14"), d("2026-10-05")))
    }

    @Test
    fun `inicio futuro no genera atrasados`() {
        assertEquals(0, ScheduleRules.overdueCount(MONTHLY, 15, null, d("2026-10-15"), null, d("2026-10-05")))
    }

    @Test
    fun `inicio hoy con el dia de hoy cuenta uno`() {
        assertEquals(1, ScheduleRules.overdueCount(MONTHLY, 5, null, d("2026-10-05"), null, d("2026-10-05")))
    }

    @Test
    fun `atrasados anuales`() {
        // 3-mar-2024, 2025 y 2026.
        assertEquals(3, ScheduleRules.overdueCount(YEARLY, 3, 3, d("2024-01-01"), null, d("2026-10-05")))
    }
}
