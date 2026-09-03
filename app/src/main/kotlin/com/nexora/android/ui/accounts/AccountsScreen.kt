package com.nexora.android.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nexora.android.R
import com.nexora.android.data.account.Account
import com.nexora.android.data.account.AccountRepository
import com.nexora.android.data.account.AccountStatus
import com.nexora.android.data.account.AccountType
import com.nexora.android.ui.common.formatCurrency

@Composable
fun AccountsScreen(
    accountRepository: AccountRepository,
    onNavigateBack: () -> Unit,
    /** Si no es null, solo se listan cuentas de este tipo — usado por el acceso "Disponible" del dashboard (solo débito). */
    filterType: AccountType? = null,
) {
    val viewModel: AccountsViewModel = viewModel(
        factory = viewModelFactory { initializer { AccountsViewModel(accountRepository) } },
    )
    val fallbackError = stringResource(R.string.accounts_load_error)
    LaunchedEffect(Unit) { viewModel.load(fallbackError) }

    var showNewAccountSheet by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var search by remember { mutableStateOf("") }
    val state = viewModel.uiState

    Scaffold(
        floatingActionButton = {
            if (state is AccountsUiState.Success) {
                FloatingActionButton(onClick = { showNewAccountSheet = true }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.accounts_new))
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                    Text(
                        if (filterType != null) accountTypeLabel(filterType) else stringResource(R.string.accounts_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }

                when (state) {
                    AccountsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is AccountsUiState.Error -> Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(state.message, style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { viewModel.load(fallbackError) }, modifier = Modifier.padding(top = 16.dp)) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                    is AccountsUiState.Success -> {
                        val typedAccounts = remember(state.accounts, filterType) {
                            if (filterType != null) state.accounts.filter { it.type == filterType } else state.accounts
                        }
                        if (typedAccounts.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    stringResource(R.string.accounts_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 32.dp),
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = search,
                                onValueChange = { search = it },
                                label = { Text(stringResource(R.string.accounts_search_label)) },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                            )
                            val filteredAccounts = remember(typedAccounts, search) {
                                typedAccounts.filter { it.name.contains(search, ignoreCase = true) }
                            }
                            if (filteredAccounts.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        stringResource(R.string.accounts_search_empty),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 32.dp),
                                    )
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(filteredAccounts, key = { it.id }) { account ->
                                        AccountCard(account, onEditClick = { editingAccount = account })
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showNewAccountSheet && state is AccountsUiState.Success) {
                NewAccountSheet(
                    accountRepository = accountRepository,
                    onDismiss = { showNewAccountSheet = false },
                    onSaved = {
                        showNewAccountSheet = false
                        viewModel.refresh(fallbackError)
                    },
                )
            }

            editingAccount?.let { account ->
                EditAccountSheet(
                    account = account,
                    accountRepository = accountRepository,
                    onDismiss = { editingAccount = null },
                    onSaved = {
                        editingAccount = null
                        viewModel.refresh(fallbackError)
                    },
                )
            }
        }
    }
}

@Composable
private fun AccountCard(account: Account, onEditClick: () -> Unit) {
    val archived = account.status == AccountStatus.ARCHIVED
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                account.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (archived) {
                    Text(
                        stringResource(R.string.accounts_archived),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                }
            }
        }
        Text(
            "${accountTypeLabel(account.type)} · ${account.currency}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
        )
        Text(formatCurrency(account.balance), style = MaterialTheme.typography.headlineSmall)

        val chips = buildList {
            if (account.includeInAvailableBalance) add(stringResource(R.string.accounts_included_available_balance))
            if (account.includeInNetWorth) add(stringResource(R.string.accounts_included_net_worth))
        }
        if (chips.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chips.forEach { chip ->
                    Text(
                        chip,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun accountTypeLabel(type: AccountType): String = when (type) {
    AccountType.DEBIT -> stringResource(R.string.accounts_type_debit)
    AccountType.SAVINGS -> stringResource(R.string.accounts_type_savings)
    AccountType.CREDIT_CARD -> stringResource(R.string.accounts_type_credit_card)
    AccountType.AFORE -> stringResource(R.string.accounts_type_afore)
    AccountType.PPR -> stringResource(R.string.accounts_type_ppr)
}
