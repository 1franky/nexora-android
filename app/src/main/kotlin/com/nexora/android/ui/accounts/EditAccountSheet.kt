package com.nexora.android.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.nexora.android.R
import com.nexora.android.data.account.Account
import com.nexora.android.data.account.AccountRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountSheet(
    account: Account,
    accountRepository: AccountRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    // remember (no viewModel()) por el mismo motivo que en NewAccountSheet: esta hoja
    // entra y sale de la composición en cada apertura y debe nacer limpia cada vez.
    val viewModel = remember { EditAccountViewModel(accountRepository, account) }
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
            Text(stringResource(R.string.accounts_edit_title), style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.accounts_dialog_name)) },
                singleLine = true,
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
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}
