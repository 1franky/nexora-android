package com.nexora.android.ui.cards

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nexora.android.R
import com.nexora.android.data.creditcard.CreditCardRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCreditCardSheet(
    creditCardRepository: CreditCardRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    // remember (no viewModel()): misma razón que en NewAccountSheet / NewTransactionSheet.
    val viewModel = remember { NewCreditCardViewModel(creditCardRepository) }
    val uiState = viewModel.uiState
    val fallbackError = stringResource(R.string.cards_load_error)
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
            Text(stringResource(R.string.cards_dialog_title), style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.cards_dialog_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.bank,
                onValueChange = viewModel::onBankChange,
                label = { Text(stringResource(R.string.cards_dialog_bank)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.last4,
                onValueChange = viewModel::onLast4Change,
                label = { Text(stringResource(R.string.cards_dialog_last4)) },
                singleLine = true,
                isError = uiState.last4.isNotEmpty() && !uiState.last4Valid,
                supportingText = {
                    Text(
                        if (uiState.last4.isNotEmpty() && !uiState.last4Valid) {
                            stringResource(R.string.cards_dialog_last4_invalid)
                        } else {
                            stringResource(R.string.cards_dialog_last4_hint)
                        },
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.creditLimit,
                onValueChange = viewModel::onCreditLimitChange,
                label = { Text(stringResource(R.string.cards_dialog_credit_limit)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.closingDay,
                    onValueChange = viewModel::onClosingDayChange,
                    label = { Text(stringResource(R.string.cards_dialog_closing_day)) },
                    supportingText = { Text(stringResource(R.string.cards_dialog_day_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = uiState.paymentDueDay,
                    onValueChange = viewModel::onPaymentDueDayChange,
                    label = { Text(stringResource(R.string.cards_dialog_payment_due_day)) },
                    supportingText = { Text(stringResource(R.string.cards_dialog_day_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = uiState.currency,
                onValueChange = viewModel::onCurrencyChange,
                label = { Text(stringResource(R.string.cards_dialog_currency)) },
                singleLine = true,
                isError = uiState.currency.isNotEmpty() && !uiState.currencyValid,
                supportingText = {
                    Text(
                        if (uiState.currency.isNotEmpty() && !uiState.currencyValid) {
                            stringResource(R.string.cards_dialog_currency_invalid)
                        } else {
                            stringResource(R.string.cards_dialog_currency_hint)
                        },
                    )
                },
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
                    Text(stringResource(R.string.cards_dialog_create))
                }
            }
        }
    }
}
