package com.nexora.android.ui.scheduledcharges

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nexora.android.R
import com.nexora.android.data.account.Account
import com.nexora.android.data.account.AccountStatus
import com.nexora.android.data.account.AccountType
import com.nexora.android.data.category.Category
import com.nexora.android.data.category.CategoryRepository
import com.nexora.android.data.category.CategoryStatus
import com.nexora.android.data.category.CategoryType
import com.nexora.android.data.scheduledcharge.ScheduledCharge
import com.nexora.android.data.scheduledcharge.ScheduledChargeFrequency
import com.nexora.android.data.scheduledcharge.ScheduledChargeRepository
import com.nexora.android.ui.cards.QuickCreateCategoryDialog
import com.nexora.android.ui.common.formatDateMedium
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Cuentas donde se puede programar un cargo: activas y que no sean AFORE/PPR
 * (regla de nexora-api: tarjeta -> compra, débito/ahorro -> gasto). En
 * edición se conserva la cuenta actual aunque ya no sea elegible, para no
 * mostrar el campo vacío — el backend rechazará guardar si sigue inválida.
 */
fun eligibleScheduledChargeAccounts(accounts: List<Account>, currentAccountId: String? = null): List<Account> =
    accounts.filter {
        (it.status == AccountStatus.ACTIVE && it.type != AccountType.AFORE && it.type != AccountType.PPR) ||
            it.id == currentAccountId
    }

/** Solo categorías de gasto activas (nexora-api rechaza las de ingreso o archivadas); se conserva la actual en edición. */
fun eligibleScheduledChargeCategories(categories: List<Category>, currentCategoryId: String? = null): List<Category> =
    categories.filter { (it.type == CategoryType.EXPENSE && it.status == CategoryStatus.ACTIVE) || it.id == currentCategoryId }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledChargeFormSheet(
    /** null = alta. */
    existing: ScheduledCharge?,
    /** Cuenta preseleccionada en un alta (desde el detalle de tarjeta/cuenta). */
    initialAccountId: String?,
    accounts: List<Account>,
    categories: List<Category>,
    scheduledChargeRepository: ScheduledChargeRepository,
    categoryRepository: CategoryRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val accountOptions = remember(accounts, existing) { eligibleScheduledChargeAccounts(accounts, existing?.accountId) }
    // remember (no viewModel()): misma razón que en las demás hojas de la app — vive lo que vive la hoja.
    // La cuenta preseleccionada solo se respeta si es elegible (p. ej. no una cuenta archivada).
    val viewModel = remember(existing?.id) {
        val preselected = initialAccountId?.takeIf { id -> accountOptions.any { it.id == id } }
        ScheduledChargeFormViewModel(scheduledChargeRepository, categoryRepository, existing, preselected)
    }
    val uiState = viewModel.uiState
    val fallbackError = stringResource(R.string.scheduled_charges_save_error)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onSaved()
    }

    var quickCreateCategory by remember { mutableStateOf(false) }
    var pickingStartDate by remember { mutableStateOf(false) }
    var pickingEndDate by remember { mutableStateOf(false) }
    // Categorías creadas al vuelo desde esta hoja: aún no están en el catálogo que llegó por parámetro.
    var createdCategories by remember { mutableStateOf(emptyList<Category>()) }

    val categoryOptions = remember(categories, createdCategories, existing) {
        eligibleScheduledChargeCategories(categories + createdCategories, existing?.categoryId)
    }
    val selectedAccount = accountOptions.firstOrNull { it.id == uiState.accountId }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(if (existing == null) R.string.scheduled_charges_dialog_title else R.string.scheduled_charges_edit_title),
                style = MaterialTheme.typography.titleLarge,
            )
            if (existing != null) {
                Text(
                    stringResource(R.string.scheduled_charges_edit_forward_only_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (accountOptions.isEmpty()) {
                Text(
                    stringResource(R.string.scheduled_charges_dialog_no_accounts),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                AccountDropdown(accounts = accountOptions, selectedId = uiState.accountId, onSelect = viewModel::onAccountChange)
            }

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.scheduled_charges_dialog_name)) },
                supportingText = { Text(stringResource(R.string.scheduled_charges_dialog_name_hint)) },
                isError = uiState.name.trim().length > SCHEDULED_CHARGE_NAME_MAX,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.amount,
                onValueChange = viewModel::onAmountChange,
                label = { Text(stringResource(R.string.scheduled_charges_dialog_amount)) },
                suffix = selectedAccount?.let { { Text(it.currency) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            ScheduledChargeCategoryDropdown(
                categories = categoryOptions,
                selectedId = uiState.categoryId,
                onSelect = viewModel::onCategoryChange,
                onCreateNew = { quickCreateCategory = true },
            )

            Text(stringResource(R.string.scheduled_charges_dialog_frequency), style = MaterialTheme.typography.labelLarge)
            val frequencies = listOf(ScheduledChargeFrequency.MONTHLY, ScheduledChargeFrequency.YEARLY)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                frequencies.forEachIndexed { index, frequency ->
                    SegmentedButton(
                        selected = uiState.frequency == frequency,
                        onClick = { viewModel.onFrequencyChange(frequency) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = frequencies.size),
                    ) {
                        Text(
                            stringResource(
                                if (frequency == ScheduledChargeFrequency.MONTHLY) {
                                    R.string.scheduled_charges_frequency_monthly
                                } else {
                                    R.string.scheduled_charges_frequency_yearly
                                },
                            ),
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.dayOfMonth,
                    onValueChange = viewModel::onDayChange,
                    label = { Text(stringResource(R.string.scheduled_charges_dialog_day)) },
                    isError = uiState.dayInvalid,
                    supportingText = {
                        Text(
                            stringResource(
                                if (uiState.dayInvalid) R.string.scheduled_charges_dialog_day_invalid else R.string.scheduled_charges_dialog_day_hint,
                            ),
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                if (uiState.frequency == ScheduledChargeFrequency.YEARLY) {
                    MonthDropdown(selected = uiState.monthOfYear, onSelect = viewModel::onMonthChange, modifier = Modifier.weight(1f))
                }
            }
            Text(
                stringResource(R.string.scheduled_charges_dialog_last_day_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            DateField(
                label = stringResource(R.string.scheduled_charges_dialog_start_date),
                value = formatDateMedium(uiState.startDate.toString()),
                enabled = !uiState.startDateLocked,
                supportingText = if (uiState.startDateLocked) stringResource(R.string.scheduled_charges_dialog_start_date_locked) else null,
                onPick = { pickingStartDate = true },
            )

            DateField(
                label = stringResource(R.string.scheduled_charges_dialog_end_date),
                value = uiState.endDate?.let { formatDateMedium(it.toString()) } ?: stringResource(R.string.scheduled_charges_dialog_no_end_date),
                enabled = true,
                isError = uiState.endDateInvalid,
                supportingText = if (uiState.endDateInvalid) stringResource(R.string.scheduled_charges_dialog_end_date_invalid) else null,
                onPick = { pickingEndDate = true },
                onClear = if (uiState.endDate != null) ({ viewModel.onEndDateChange(null) }) else null,
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text(stringResource(R.string.scheduled_charges_dialog_description)) },
                isError = uiState.description.trim().length > SCHEDULED_CHARGE_DESCRIPTION_MAX,
                modifier = Modifier.fillMaxWidth(),
            )

            val overdue = uiState.overdueCount(viewModel.currentDate)
            if (overdue > 0) {
                Text(
                    pluralStringResource(R.plurals.scheduled_charges_overdue_warning, overdue, overdue),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                )
            }

            if (uiState.error != null) {
                Text(uiState.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { viewModel.submit(fallbackError) },
                enabled = uiState.canSubmit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(if (existing == null) R.string.scheduled_charges_dialog_create else R.string.action_save))
                }
            }
        }
    }

    if (pickingStartDate) {
        DatePickerModal(
            initial = uiState.startDate,
            onDismiss = { pickingStartDate = false },
            onConfirm = { viewModel.onStartDateChange(it); pickingStartDate = false },
        )
    }

    if (pickingEndDate) {
        DatePickerModal(
            initial = uiState.endDate ?: uiState.startDate,
            onDismiss = { pickingEndDate = false },
            onConfirm = { viewModel.onEndDateChange(it); pickingEndDate = false },
        )
    }

    if (quickCreateCategory) {
        QuickCreateCategoryDialog(
            onDismiss = { quickCreateCategory = false },
            onCreate = { name ->
                viewModel.createCategory(name, fallbackError) { category ->
                    createdCategories = createdCategories + category
                    viewModel.onCategoryChange(category.id)
                    quickCreateCategory = false
                }
            },
        )
    }
}

@Composable
private fun DateField(
    label: String,
    value: String,
    enabled: Boolean,
    onPick: () -> Unit,
    isError: Boolean = false,
    supportingText: String? = null,
    onClear: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        enabled = enabled,
        isError = isError,
        label = { Text(label) },
        supportingText = supportingText?.let { { Text(it) } },
        trailingIcon = {
            Row {
                if (onClear != null) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.scheduled_charges_dialog_clear_end_date))
                    }
                }
                IconButton(onClick = onPick, enabled = enabled) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = label)
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onPick),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(initial: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                } ?: onDismiss()
            }) { Text(stringResource(R.string.scheduled_charges_dialog_pick_date)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.back)) } },
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdown(accounts: List<Account>, selectedId: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = accounts.firstOrNull { it.id == selectedId }?.let { "${it.name} (${it.currency})" } ?: ""

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.scheduled_charges_dialog_account)) },
            supportingText = { Text(stringResource(R.string.scheduled_charges_dialog_account_hint)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.exposedDropdownSize()) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text("${account.name} (${account.currency})") },
                    onClick = { onSelect(account.id); expanded = false },
                )
            }
        }
    }
}

/**
 * Como CategoryDropdown (ui.cards), pero «Sin categoría» sí limpia la
 * selección: aquí la categoría es opcional y editable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduledChargeCategoryDropdown(
    categories: List<Category>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onCreateNew: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val noCategoryLabel = stringResource(R.string.transactions_category_none)
    val selectedName = categories.firstOrNull { it.id == selectedId }?.name ?: noCategoryLabel

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.scheduled_charges_dialog_category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.exposedDropdownSize()) {
            DropdownMenuItem(text = { Text(noCategoryLabel) }, onClick = { onSelect(null); expanded = false })
            categories.forEach { category ->
                DropdownMenuItem(text = { Text(category.name) }, onClick = { onSelect(category.id); expanded = false })
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.transactions_category_new)) },
                onClick = { expanded = false; onCreateNew() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthDropdown(selected: Int?, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected?.let { monthName(it) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.scheduled_charges_dialog_month)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.exposedDropdownSize()) {
            (1..12).forEach { month ->
                DropdownMenuItem(text = { Text(monthName(month)) }, onClick = { onSelect(month); expanded = false })
            }
        }
    }
}
