package com.nexora.android.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nexora.android.R
import com.nexora.android.data.account.AccountRepository
import com.nexora.android.data.account.AccountType

private val ACCOUNT_TYPES = listOf(
    AccountType.DEBIT,
    AccountType.SAVINGS,
    AccountType.CREDIT_CARD,
    AccountType.AFORE,
    AccountType.PPR,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAccountSheet(
    accountRepository: AccountRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    // remember (no viewModel()) por el mismo motivo que en NewTransactionSheet: esta hoja
    // entra y sale de la composición en cada apertura y debe nacer limpia cada vez.
    val viewModel = remember { NewAccountViewModel(accountRepository) }
    val uiState = viewModel.uiState
    val fallbackError = stringResource(R.string.accounts_load_error)
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onSaved()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.accounts_dialog_title), style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.accounts_dialog_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            AccountTypeDropdown(selected = uiState.type, onSelect = viewModel::onTypeChange)

            OutlinedTextField(
                value = uiState.currency,
                onValueChange = viewModel::onCurrencyChange,
                label = { Text(stringResource(R.string.accounts_dialog_currency)) },
                singleLine = true,
                isError = uiState.currency.isNotEmpty() && !uiState.currencyValid,
                supportingText = {
                    Text(
                        if (uiState.currency.isNotEmpty() && !uiState.currencyValid) {
                            stringResource(R.string.accounts_dialog_currency_invalid)
                        } else {
                            stringResource(R.string.accounts_dialog_currency_hint)
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.openingBalance,
                onValueChange = viewModel::onOpeningBalanceChange,
                label = { Text(stringResource(R.string.accounts_dialog_opening_balance)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            SwitchRow(
                label = stringResource(R.string.accounts_dialog_include_available_balance),
                checked = uiState.includeInAvailableBalance,
                onCheckedChange = viewModel::onIncludeInAvailableBalanceChange,
            )
            SwitchRow(
                label = stringResource(R.string.accounts_dialog_include_net_worth),
                checked = uiState.includeInNetWorth,
                onCheckedChange = viewModel::onIncludeInNetWorthChange,
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
                    Text(stringResource(R.string.accounts_dialog_create))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountTypeDropdown(selected: AccountType, onSelect: (AccountType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = accountTypeLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.accounts_dialog_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize(),
        ) {
            ACCOUNT_TYPES.forEach { type ->
                DropdownMenuItem(
                    text = { Text(accountTypeLabel(type)) },
                    onClick = { onSelect(type); expanded = false },
                )
            }
        }
    }
}

/** No es private: la reutiliza EditAccountSheet. */
@Composable
fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
