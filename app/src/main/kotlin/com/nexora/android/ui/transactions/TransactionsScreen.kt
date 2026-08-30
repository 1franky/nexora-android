package com.nexora.android.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nexora.android.R
import com.nexora.android.data.account.Account
import com.nexora.android.data.account.AccountRepository
import com.nexora.android.data.account.AccountStatus
import com.nexora.android.data.category.Category
import com.nexora.android.data.category.CategoryRepository
import com.nexora.android.data.transaction.Transaction
import com.nexora.android.data.transaction.TransactionRepository
import com.nexora.android.data.transaction.TransactionType
import com.nexora.android.ui.common.formatCurrency
import com.nexora.android.ui.common.formatDateShort
import com.nexora.android.ui.theme.NexoraExtendedTheme

@Composable
fun TransactionsScreen(
    transactionRepository: TransactionRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
) {
    val viewModel: TransactionsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { TransactionsViewModel(transactionRepository, accountRepository, categoryRepository) }
        },
    )
    val fallbackError = stringResource(R.string.transactions_load_error)
    LaunchedEffect(Unit) { viewModel.load(fallbackError) }

    var showNewTransactionSheet by remember { mutableStateOf(false) }
    val state = viewModel.uiState

    Scaffold(
        floatingActionButton = {
            if (state is TransactionsUiState.Success) {
                FloatingActionButton(onClick = { showNewTransactionSheet = true }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.transactions_new))
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (state) {
                TransactionsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is TransactionsUiState.Error -> Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(state.message, style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { viewModel.load(fallbackError) }, modifier = Modifier.padding(top = 16.dp)) {
                        Text(stringResource(R.string.retry))
                    }
                }
                is TransactionsUiState.Success -> TransactionsContent(
                    state = state,
                    onSelectAccount = { viewModel.selectAccount(it, fallbackError) },
                )
            }

            if (showNewTransactionSheet && state is TransactionsUiState.Success) {
                NewTransactionSheet(
                    accounts = state.accounts.filter { it.status == AccountStatus.ACTIVE },
                    categories = state.categories,
                    defaultAccountId = state.selectedAccountId,
                    transactionRepository = transactionRepository,
                    categoryRepository = categoryRepository,
                    onDismiss = { showNewTransactionSheet = false },
                    onSaved = {
                        showNewTransactionSheet = false
                        viewModel.refresh(fallbackError)
                    },
                )
            }
        }
    }
}

@Composable
private fun TransactionsContent(
    state: TransactionsUiState.Success,
    onSelectAccount: (String?) -> Unit,
) {
    var filterExpanded by remember { mutableStateOf(false) }
    val accountNameById = remember(state.accounts) { state.accounts.associate { it.id to it.name } }
    val categoryNameById = remember(state.categories) { state.categories.associate { it.id to it.name } }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.transactions_title), style = MaterialTheme.typography.headlineSmall)
            Box {
                IconButton(onClick = { filterExpanded = true }) {
                    Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.transactions_filter_content_description))
                }
                DropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.transactions_all_accounts)) },
                        onClick = { filterExpanded = false; onSelectAccount(null) },
                    )
                    state.accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = { filterExpanded = false; onSelectAccount(account.id) },
                        )
                    }
                }
            }
        }

        if (state.transactions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.transactions_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                items(state.transactions, key = { it.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        accountName = accountNameById[transaction.accountId],
                        relatedLabel = resolveRelatedLabel(transaction, accountNameById, categoryNameById),
                        showAccount = state.selectedAccountId == null,
                    )
                }
            }
        }
    }
}

private fun resolveRelatedLabel(
    transaction: Transaction,
    accountNameById: Map<String, String>,
    categoryNameById: Map<String, String>,
): String? = transaction.categoryId?.let { categoryNameById[it] }
    ?: transaction.counterAccountId?.let { accountNameById[it] }
    ?: transaction.merchant

@Composable
private fun TransactionRow(transaction: Transaction, accountName: String?, relatedLabel: String?, showAccount: Boolean) {
    val (icon, tint) = iconAndTintFor(transaction)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(tint.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(relatedLabel ?: formatDateShort(transaction.date), style = MaterialTheme.typography.bodyLarge)
            val subtitle = if (showAccount) accountName else formatDateShort(transaction.date)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val amountColor = when {
            transaction.balanceEffect > 0 -> NexoraExtendedTheme.colors.income
            transaction.balanceEffect < 0 -> NexoraExtendedTheme.colors.expense
            else -> MaterialTheme.colorScheme.onSurface
        }
        val sign = if (transaction.balanceEffect >= 0) "+" else ""
        Text(
            "$sign${formatCurrency(transaction.balanceEffect)}",
            style = MaterialTheme.typography.titleSmall,
            color = amountColor,
        )
    }
}

@Composable
private fun iconAndTintFor(transaction: Transaction): Pair<ImageVector, androidx.compose.ui.graphics.Color> = when (transaction.type) {
    TransactionType.INCOME -> Icons.Filled.ArrowDownward to NexoraExtendedTheme.colors.income
    TransactionType.TRANSFER -> Icons.Filled.SwapHoriz to MaterialTheme.colorScheme.primary
    else -> Icons.Filled.ArrowUpward to NexoraExtendedTheme.colors.expense
}
