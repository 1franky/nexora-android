package com.nexora.android.ui.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nexora.android.R
import com.nexora.android.data.category.Category
import com.nexora.android.data.category.CategoryRepository
import com.nexora.android.data.category.CategoryStatus
import com.nexora.android.data.category.CategoryType
import com.nexora.android.data.creditcard.CreditCardRepository
import com.nexora.android.ui.common.formatDateShort
import java.time.Instant
import java.time.ZoneOffset

private const val NEW_CATEGORY_OPTION = "__new__"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardPurchaseSheet(
    cardId: String,
    creditCardRepository: CreditCardRepository,
    categoryRepository: CategoryRepository,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    // remember (no viewModel()): misma razón que en NewTransactionSheet.
    val viewModel = remember { CreditCardPurchaseViewModel(cardId, creditCardRepository, categoryRepository) }
    val uiState = viewModel.uiState
    val fallbackError = stringResource(R.string.cards_detail_not_found)
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onSaved()
    }

    var quickCreateCategory by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.purchase_dialog_title), style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = uiState.amount,
                onValueChange = viewModel::onAmountChange,
                label = { Text(stringResource(R.string.purchase_dialog_amount)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = formatDateShort(uiState.date.toString()),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.purchase_dialog_date)) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = stringResource(R.string.purchase_dialog_date))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
            )

            OutlinedTextField(
                value = uiState.merchant,
                onValueChange = viewModel::onMerchantChange,
                label = { Text(stringResource(R.string.purchase_dialog_merchant)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            CategoryDropdown(
                categories = categories.filter { it.type == CategoryType.EXPENSE && it.status == CategoryStatus.ACTIVE },
                selectedId = uiState.categoryId,
                onSelect = { if (it == NEW_CATEGORY_OPTION) quickCreateCategory = true else viewModel.onCategoryChange(it) },
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text(stringResource(R.string.purchase_dialog_description)) },
                modifier = Modifier.fillMaxWidth(),
            )

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
                    Text(stringResource(R.string.purchase_dialog_submit))
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.onDateChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.purchase_dialog_submit)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.back)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (quickCreateCategory) {
        QuickCreateCategoryDialog(
            onDismiss = { quickCreateCategory = false },
            onCreate = { name ->
                viewModel.createCategory(name, fallbackError) { category ->
                    viewModel.onCategoryChange(category.id)
                    quickCreateCategory = false
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(categories: List<Category>, selectedId: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val noCategoryLabel = stringResource(R.string.transactions_category_none)
    val newCategoryLabel = stringResource(R.string.transactions_category_new)
    val selectedName = categories.firstOrNull { it.id == selectedId }?.name ?: noCategoryLabel

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.purchase_dialog_category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize(),
        ) {
            DropdownMenuItem(text = { Text(noCategoryLabel) }, onClick = { expanded = false })
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = { onSelect(category.id); expanded = false },
                )
            }
            DropdownMenuItem(
                text = { Text(newCategoryLabel) },
                onClick = { expanded = false; onSelect(NEW_CATEGORY_OPTION) },
            )
        }
    }
}

@Composable
private fun QuickCreateCategoryDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_category_dialog_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.new_category_dialog_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name.trim()) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.new_category_dialog_create))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.back)) } },
    )
}
