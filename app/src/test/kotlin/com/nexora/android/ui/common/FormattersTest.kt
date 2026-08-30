package com.nexora.android.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattersTest {

    // Sin igualdad exacta contra un string fijo: el símbolo/orden exacto de
    // NumberFormat.getCurrencyInstance depende de los datos ICU del JDK que
    // corra el test — se verifican las propiedades que sí nos importan.
    @Test
    fun `formatCurrency redondea a enteros y no muestra decimales`() {
        val result = formatCurrency(1234.56)
        assertTrue("debería contener el entero redondeado: $result", result.contains("1,235") || result.contains("1235"))
        assertFalse("no debería mostrar decimales: $result", result.contains("."))
    }

    @Test
    fun `formatCurrency maneja cero y negativos`() {
        assertFalse(formatCurrency(0.0).contains("."))
        val negative = formatCurrency(-500.0)
        assertTrue("debería indicar que es negativo: $negative", negative.contains("-"))
    }

    @Test
    fun `formatDateShort convierte ISO a dia y mes abreviado sin punto`() {
        val result = formatDateShort("2026-08-29")
        assertTrue("debería incluir el día: $result", result.contains("29"))
        assertFalse("no debería tener punto (ver replace en Formatters)", result.contains("."))
    }

    @Test
    fun `formatDateShort con fecha invalida devuelve el string original`() {
        assertEquals("no-es-una-fecha", formatDateShort("no-es-una-fecha"))
    }
}
