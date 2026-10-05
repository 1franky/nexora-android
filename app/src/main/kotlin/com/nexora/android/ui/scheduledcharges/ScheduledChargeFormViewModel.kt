package com.nexora.android.ui.scheduledcharges

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.android.data.category.Category
import com.nexora.android.data.category.CategoryRepository
import com.nexora.android.data.category.CategoryType
import com.nexora.android.data.common.ApiException
import com.nexora.android.data.scheduledcharge.ScheduledCharge
import com.nexora.android.data.scheduledcharge.ScheduledChargeFrequency
import com.nexora.android.data.scheduledcharge.ScheduledChargeRepository
import com.nexora.android.data.scheduledcharge.ScheduledChargeRequest
import com.nexora.android.data.scheduledcharge.ScheduledChargeStatus
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Mismos topes que las validaciones de ScheduledChargeRequest en nexora-api. */
const val SCHEDULED_CHARGE_NAME_MAX = 120
const val SCHEDULED_CHARGE_DESCRIPTION_MAX = 500

data class ScheduledChargeFormUiState(
    val accountId: String? = null,
    val name: String = "",
    val amount: String = "",
    val categoryId: String? = null,
    val description: String = "",
    val frequency: ScheduledChargeFrequency = ScheduledChargeFrequency.MONTHLY,
    val dayOfMonth: String = "",
    /** Se conserva aunque la frecuencia sea mensual (por si el usuario vuelve a anual); solo se manda si es YEARLY. */
    val monthOfYear: Int? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    /**
     * El usuario eligió la fecha de inicio a mano: deja de recalcularse sola como «la
     * próxima ocurrencia desde hoy» cada vez que cambia el día/mes/frecuencia.
     */
    val startDateTouched: Boolean = false,
    /** Edición de un cargo que ya generó movimientos: nexora-api rechaza (400) cambiar startDate. */
    val startDateLocked: Boolean = false,
    /**
     * Si al guardar el backend registrará de inmediato las ocurrencias vencidas desde startDate:
     * siempre en un alta, y en la edición de un cargo ACTIVO que nunca ha corrido
     * (ScheduledChargeService.update recalcula desde startDate en ese caso).
     */
    val backfillApplies: Boolean = true,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
) {
    val day: Int? get() = dayOfMonth.toIntOrNull()?.takeIf { it in 1..31 }

    val dayInvalid: Boolean get() = dayOfMonth.isNotEmpty() && day == null

    val effectiveMonth: Int? get() = if (frequency == ScheduledChargeFrequency.YEARLY) monthOfYear else null

    val ruleValid: Boolean get() =
        day != null && (frequency == ScheduledChargeFrequency.MONTHLY || monthOfYear in 1..12)

    val endDateInvalid: Boolean get() = endDate != null && endDate < startDate

    val canSubmit: Boolean get() =
        accountId != null &&
            name.isNotBlank() && name.trim().length <= SCHEDULED_CHARGE_NAME_MAX &&
            (amount.toDoubleOrNull() ?: 0.0) > 0.0 &&
            ruleValid &&
            !endDateInvalid &&
            description.trim().length <= SCHEDULED_CHARGE_DESCRIPTION_MAX &&
            !isSaving

    /** «Se registrarán N cargos atrasados» — 0 si no aplica o la regla aún está incompleta. */
    fun overdueCount(today: LocalDate): Int {
        val validDay = day
        if (!backfillApplies || !ruleValid || validDay == null || endDateInvalid) return 0
        return ScheduleRules.overdueCount(frequency, validDay, effectiveMonth, startDate, endDate, today)
    }

    /** Recalcula la fecha de inicio sugerida (próxima ocurrencia desde [today]) si el usuario no la fijó. */
    fun withSuggestedStartDate(today: LocalDate): ScheduledChargeFormUiState {
        val validDay = day
        if (isEditing || startDateTouched || startDateLocked || !ruleValid || validDay == null) return this
        return copy(startDate = ScheduleRules.firstOnOrAfter(frequency, validDay, effectiveMonth, today))
    }

    /** Solo válido si [canSubmit]. */
    fun toRequest(): ScheduledChargeRequest = ScheduledChargeRequest(
        accountId = requireNotNull(accountId),
        name = name.trim(),
        amount = amount.toDouble(),
        categoryId = categoryId,
        description = description.trim().ifBlank { null },
        frequency = frequency,
        dayOfMonth = requireNotNull(day),
        monthOfYear = effectiveMonth,
        startDate = startDate.toString(),
        endDate = endDate?.toString(),
    )

    companion object {
        fun forCreate(initialAccountId: String?, today: LocalDate) =
            ScheduledChargeFormUiState(accountId = initialAccountId, startDate = today)

        fun forEdit(charge: ScheduledCharge) = ScheduledChargeFormUiState(
            accountId = charge.accountId,
            name = charge.name,
            amount = formatAmountForInput(charge.amount),
            categoryId = charge.categoryId,
            description = charge.description.orEmpty(),
            frequency = charge.frequency,
            dayOfMonth = charge.dayOfMonth.toString(),
            monthOfYear = charge.monthOfYear,
            startDate = LocalDate.parse(charge.startDate),
            endDate = charge.endDate?.let(LocalDate::parse),
            startDateTouched = true,
            startDateLocked = charge.lastRunDate != null,
            backfillApplies = charge.lastRunDate == null && charge.status == ScheduledChargeStatus.ACTIVE,
            isEditing = true,
        )

        /** 119.0 -> "119", 119.5 -> "119.5": sin el ".0" que deja Double.toString. */
        fun formatAmountForInput(amount: Double): String =
            if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toBigDecimal().stripTrailingZeros().toPlainString()
    }
}

/**
 * Alta/edición de un cargo programado (A15). Mismo criterio que el diálogo de
 * W13 en nexora-web. Requiere conexión (ver ScheduledChargeRepository).
 */
class ScheduledChargeFormViewModel(
    private val scheduledChargeRepository: ScheduledChargeRepository,
    private val categoryRepository: CategoryRepository,
    existing: ScheduledCharge?,
    initialAccountId: String?,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val chargeId = existing?.id

    var uiState by mutableStateOf(
        existing?.let { ScheduledChargeFormUiState.forEdit(it) }
            ?: ScheduledChargeFormUiState.forCreate(initialAccountId, today()),
    )
        private set

    val currentDate: LocalDate get() = today()

    private fun update(transform: (ScheduledChargeFormUiState) -> ScheduledChargeFormUiState) {
        uiState = transform(uiState).copy(error = null).withSuggestedStartDate(today())
    }

    fun onAccountChange(value: String) = update { it.copy(accountId = value) }

    fun onNameChange(value: String) = update { it.copy(name = value) }

    fun onAmountChange(value: String) = update { it.copy(amount = value) }

    fun onCategoryChange(value: String?) = update { it.copy(categoryId = value) }

    fun onDescriptionChange(value: String) = update { it.copy(description = value) }

    fun onFrequencyChange(value: ScheduledChargeFrequency) = update {
        // Al pasar a anual sin mes elegido, se sugiere el mes en curso.
        val month = if (value == ScheduledChargeFrequency.YEARLY && it.monthOfYear == null) today().monthValue else it.monthOfYear
        it.copy(frequency = value, monthOfYear = month)
    }

    fun onDayChange(value: String) = update { it.copy(dayOfMonth = value.filter(Char::isDigit).take(2)) }

    fun onMonthChange(value: Int) = update { it.copy(monthOfYear = value) }

    fun onStartDateChange(value: LocalDate) = update {
        if (it.startDateLocked) it else it.copy(startDate = value, startDateTouched = true)
    }

    fun onEndDateChange(value: LocalDate?) = update { it.copy(endDate = value) }

    fun createCategory(name: String, fallbackError: String, onCreated: (Category) -> Unit) {
        viewModelScope.launch {
            try {
                onCreated(categoryRepository.createCategory(name, CategoryType.EXPENSE, fallbackError))
            } catch (e: ApiException) {
                uiState = uiState.copy(error = e.message ?: fallbackError)
            }
        }
    }

    fun submit(fallbackError: String) {
        val state = uiState
        if (!state.canSubmit) return

        uiState = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                val request = state.toRequest()
                if (chargeId == null) {
                    scheduledChargeRepository.create(request, fallbackError)
                } else {
                    scheduledChargeRepository.update(chargeId, request, fallbackError)
                }
                uiState = uiState.copy(isSaving = false, saved = true)
            } catch (e: ApiException) {
                uiState = uiState.copy(isSaving = false, error = e.message ?: fallbackError)
            }
        }
    }
}
