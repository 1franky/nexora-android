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
import com.nexora.android.data.account.Account
import com.nexora.android.data.creditcard.CreditCardRepository
import com.nexora.android.ui.common.formatDateShort
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardPaymentSheet(
    cardId: String,
    creditCardRepository: CreditCardRepository,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    // remember (no viewModel()): misma razón que en NewTransactionSheet.
    val viewModel = remember { CreditCardPaymentViewModel(cardId, creditCardRepository, accounts.firstOrNull()?.id) }
    val uiState = viewModel.uiState
    val fallbackError = stringResource(R.string.cards_detail_not_found)
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onSaved()
    }

    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.payment_dialog_title), style = MaterialTheme.typography.titleLarge)

            if (accounts.isEmpty()) {
                Text(
                    stringResource(R.string.payment_dialog_no_accounts_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                AccountDropdown(accounts = accounts, selectedId = uiState.fromAccountId, onSelect = viewModel::onFromAccountChange)
            }

            OutlinedTextField(
                value = uiState.amount,
                onValueChange = viewModel::onAmountChange,
                label = { Text(stringResource(R.string.payment_dialog_amount)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = formatDateShort(uiState.date.toString()),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.payment_dialog_date)) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = stringResource(R.string.payment_dialog_date))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text(stringResource(R.string.payment_dialog_description)) },
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
                    Text(stringResource(R.string.payment_dialog_submit))
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
                }) { Text(stringResource(R.string.payment_dialog_submit)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.back)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdown(accounts: List<Account>, selectedId: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = accounts.firstOrNull { it.id == selectedId }?.let { "${it.name} (${it.currency})" } ?: ""

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.payment_dialog_from_account)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize(),
        ) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text("${account.name} (${account.currency})") },
                    onClick = { onSelect(account.id); expanded = false },
                )
            }
        }
    }
}
