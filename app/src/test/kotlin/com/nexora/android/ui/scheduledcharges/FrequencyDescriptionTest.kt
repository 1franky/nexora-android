package com.nexora.android.ui.scheduledcharges

import com.nexora.android.data.scheduledcharge.ScheduledChargeFrequency
import org.junit.Assert.assertEquals
import org.junit.Test

/** Plantillas copiadas de strings.xml (scheduled_charges_frequency_*_desc). */
class FrequencyDescriptionTest {

    private val monthly = "Cada día %1\$d"
    private val yearly = "Cada %1\$d de %2\$s"

    @Test
    fun `mensual muestra el dia`() {
        assertEquals("Cada día 15", frequencyDescription(ScheduledChargeFrequency.MONTHLY, 15, null, monthly, yearly))
    }

    @Test
    fun `anual muestra dia y mes en minusculas`() {
        assertEquals("Cada 3 de marzo", frequencyDescription(ScheduledChargeFrequency.YEARLY, 3, 3, monthly, yearly))
        assertEquals("Cada 31 de diciembre", frequencyDescription(ScheduledChargeFrequency.YEARLY, 31, 12, monthly, yearly))
    }

    @Test
    fun `mensual ignora un mes sobrante`() {
        assertEquals("Cada día 1", frequencyDescription(ScheduledChargeFrequency.MONTHLY, 1, 5, monthly, yearly))
    }

    @Test
    fun `nombres de mes en espanol`() {
        assertEquals("enero", monthName(1))
        assertEquals("septiembre", monthName(9))
    }
}
