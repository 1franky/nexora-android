package com.nexora.android.ui.dashboard

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nexora.android.R
import com.nexora.android.data.account.AccountRepository
import com.nexora.android.data.category.CategoryRepository
import com.nexora.android.data.transaction.TransactionRepository
import com.nexora.android.ui.common.formatCurrency
import com.nexora.android.ui.transactions.TransactionRow
import com.nexora.android.ui.transactions.resolveRelatedLabel

/** Resumen de "gastos del mes" del dashboard: gastos y compras de tarjeta del mes en curso. */
@Composable
fun MonthExpensesScreen(
    transactionRepository: TransactionRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    onNavigateBack: () -> Unit,
) {
    val viewModel: MonthExpensesViewModel = viewModel(
        factory = viewModelFactory {
            initializer { MonthExpensesViewModel(transactionRepository, accountRepository, categoryRepository) }
        },
    )
    val fallbackError = stringResource(R.string.month_expenses_load_error)
    LaunchedEffect(Unit) { viewModel.load(fallbackError) }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.month_expenses_title), style = MaterialTheme.typography.headlineSmall)
        }

        when (val state = viewModel.uiState) {
            MonthExpensesUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is MonthExpensesUiState.Error -> Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(state.message, style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { viewModel.load(fallbackError) }, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.retry))
                }
            }
            is MonthExpensesUiState.Success -> {
                val data = state.data
                if (data.transactions.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.month_expenses_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                    }
                } else {
                    val total = data.transactions.sumOf { it.amount }
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Text(
                            stringResource(R.string.dashboard_expenses_this_month),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(formatCurrency(total), style = MaterialTheme.typography.headlineMedium)
                    }
                    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                        items(data.transactions, key = { it.id }) { transaction ->
                            TransactionRow(
                                transaction = transaction,
                                accountName = data.accountNameById[transaction.accountId],
                                relatedLabel = resolveRelatedLabel(transaction, data.categoryNameById),
                                // EXPENSE_TYPES (arriba) nunca incluye TRANSFER/CREDIT_CARD_PAYMENT, así que esta lista no tiene contraparte que mostrar.
                                counterAccountName = null,
                                showAccount = true,
                                onEdit = null,
                                onDelete = null,
                            )
                        }
                    }
                }
            }
        }
    }
}
