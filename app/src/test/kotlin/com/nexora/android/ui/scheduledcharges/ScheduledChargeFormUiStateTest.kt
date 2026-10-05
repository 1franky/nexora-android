package com.nexora.android.ui.scheduledcharges

import com.nexora.android.data.account.AccountType
import com.nexora.android.data.scheduledcharge.ScheduledCharge
import com.nexora.android.data.scheduledcharge.ScheduledChargeFrequency
import com.nexora.android.data.scheduledcharge.ScheduledChargeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ScheduledChargeFormUiStateTest {

    private val today = LocalDate.parse("2026-10-05")

    private val valid = ScheduledChargeFormUiState.forCreate("acc-1", today).copy(
        name = "Disney+",
        amount = "119",
        dayOfMonth = "15",
    ).withSuggestedStartDate(today)

    private fun charge(lastRunDate: String? = null, status: ScheduledChargeStatus = ScheduledChargeStatus.ACTIVE) = ScheduledCharge(
        id = "sc-1",
        accountId = "acc-1",
        accountName = "BBVA Azul",
        accountType = AccountType.CREDIT_CARD,
        currency = "MXN",
        name = "Disney+",
        amount = 119.0,
        frequency = ScheduledChargeFrequency.YEARLY,
        dayOfMonth = 3,
        monthOfYear = 3,
        startDate = "2025-03-03",
        nextRunDate = "2027-03-03",
        lastRunDate = lastRunDate,
        status = status,
        createdAt = "2025-03-01T10:00:00Z",
    )

    @Test
    fun `formulario completo es enviable`() {
        assertTrue(valid.canSubmit)
    }

    @Test
    fun `sin cuenta, nombre, monto o dia no es enviable`() {
        assertFalse(valid.copy(accountId = null).canSubmit)
        assertFalse(valid.copy(name = "  ").canSubmit)
        assertFalse(valid.copy(amount = "0").canSubmit)
        assertFalse(valid.copy(amount = "abc").canSubmit)
        assertFalse(valid.copy(dayOfMonth = "32").canSubmit)
        assertFalse(valid.copy(dayOfMonth = "").canSubmit)
    }

    @Test
    fun `nombre de mas de 120 caracteres no es enviable`() {
        assertFalse(valid.copy(name = "x".repeat(121)).canSubmit)
    }

    @Test
    fun `anual exige mes`() {
        assertFalse(valid.copy(frequency = ScheduledChargeFrequency.YEARLY, monthOfYear = null).canSubmit)
        assertTrue(valid.copy(frequency = ScheduledChargeFrequency.YEARLY, monthOfYear = 3).canSubmit)
    }

    @Test
    fun `fecha de fin anterior al inicio no es enviable`() {
        assertFalse(valid.copy(endDate = valid.startDate.minusDays(1)).canSubmit)
        assertTrue(valid.copy(endDate = valid.startDate).canSubmit)
    }

    @Test
    fun `la fecha de inicio sugerida es la proxima ocurrencia desde hoy`() {
        assertEquals(LocalDate.parse("2026-10-15"), valid.startDate)
        val dayThree = valid.copy(dayOfMonth = "3").withSuggestedStartDate(today)
        assertEquals(LocalDate.parse("2026-11-03"), dayThree.startDate)
    }

    @Test
    fun `una fecha de inicio elegida a mano no se recalcula`() {
        val manual = valid.copy(startDate = LocalDate.parse("2026-01-01"), startDateTouched = true)
        assertEquals(LocalDate.parse("2026-01-01"), manual.copy(dayOfMonth = "20").withSuggestedStartDate(today).startDate)
    }

    @Test
    fun `inicio pasado calcula los atrasados`() {
        val past = valid.copy(startDate = LocalDate.parse("2026-07-01"), startDateTouched = true)
        // 15-jul, 15-ago, 15-sep.
        assertEquals(3, past.overdueCount(today))
        assertEquals(0, valid.overdueCount(today))
    }

    @Test
    fun `request mensual manda monthOfYear null aunque haya un mes guardado`() {
        val request = valid.copy(monthOfYear = 4, description = "  ").toRequest()
        assertNull(request.monthOfYear)
        assertNull(request.description)
        assertEquals("2026-10-15", request.startDate)
        assertEquals(15, request.dayOfMonth)
        assertEquals(119.0, request.amount, 0.0)
    }

    @Test
    fun `edicion de un cargo que ya corrio bloquea la fecha de inicio y no avisa atrasados`() {
        val state = ScheduledChargeFormUiState.forEdit(charge(lastRunDate = "2026-03-03"))
        assertTrue(state.startDateLocked)
        assertFalse(state.backfillApplies)
        assertEquals(0, state.overdueCount(today))
        assertEquals("119", state.amount)
        assertEquals(3, state.toRequest().monthOfYear)
    }

    @Test
    fun `edicion de un cargo activo que nunca corrio si avisa atrasados`() {
        val state = ScheduledChargeFormUiState.forEdit(charge())
        assertFalse(state.startDateLocked)
        assertTrue(state.backfillApplies)
        // 3-mar-2025 y 3-mar-2026.
        assertEquals(2, state.overdueCount(today))
        // Un cargo pausado no se procesa al editarlo.
        assertFalse(ScheduledChargeFormUiState.forEdit(charge(status = ScheduledChargeStatus.PAUSED)).backfillApplies)
    }

    @Test
    fun `monto para el campo de texto sin ceros sobrantes`() {
        assertEquals("119", ScheduledChargeFormUiState.formatAmountForInput(119.0))
        assertEquals("119.5", ScheduledChargeFormUiState.formatAmountForInput(119.5))
        assertEquals("99.99", ScheduledChargeFormUiState.formatAmountForInput(99.99))
    }
}
