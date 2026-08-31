package com.nexora.android.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.nexora.android.data.category.Category
import com.nexora.android.data.category.CategoryRepository
import com.nexora.android.data.creditcard.CreditCardRepository
import com.nexora.android.data.installment.InstallmentPlan
import com.nexora.android.data.installment.InstallmentRepository
import com.nexora.android.data.transaction.Transaction
import com.nexora.android.data.transaction.TransactionRepository
import com.nexora.android.data.transaction.TransactionType
import com.nexora.android.ui.common.formatCurrency
import com.nexora.android.ui.common.formatDateShort
import com.nexora.android.ui.theme.NexoraExtendedTheme

@Composable
fun CardDetailScreen(
    cardId: String,
    creditCardRepository: CreditCardRepository,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
    installmentRepository: InstallmentRepository,
    onNavigateBack: () -> Unit,
) {
    val viewModel: CardDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer { CardDetailViewModel(cardId, creditCardRepository, transactionRepository, categoryRepository, accountRepository) }
        },
    )
    val installmentPlansViewModel: InstallmentPlansViewModel = viewModel(
        factory = viewModelFactory { initializer { InstallmentPlansViewModel(cardId, installmentRepository) } },
    )
    val fallbackError = stringResource(R.string.cards_detail_not_found)
    val installmentsFallbackError = stringResource(R.string.installments_load_error)
    LaunchedEffect(Unit) {
        viewModel.load(fallbackError)
        installmentPlansViewModel.load(installmentsFallbackError)
    }

    var showPurchaseSheet by remember { mutableStateOf(false) }
    var showPaymentSheet by remember { mutableStateOf(false) }
    var showInstallmentPlanSheet by remember { mutableStateOf(false) }
    var editingPurchase by remember { mutableStateOf<Transaction?>(null) }
    var editingPlan by remember { mutableStateOf<InstallmentPlan?>(null) }
    val state = viewModel.uiState

    // Las compras de un plan MSI/MCI se editan desde su propio plan (montos/cuotas), no desde aquí.
    val planTransactionIds = (installmentPlansViewModel.uiState as? InstallmentPlansUiState.Success)
        ?.plans?.map { it.transactionId }?.toSet() ?: emptySet()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }

            when (state) {
                CardDetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is CardDetailUiState.Error -> Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(state.message, style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { viewModel.load(fallbackError) }, modifier = Modifier.padding(top = 16.dp)) {
                        Text(stringResource(R.string.retry))
                    }
                }
                is CardDetailUiState.Success -> CardDetailContent(
                    state = state,
                    onRecordPurchase = { showPurchaseSheet = true },
                    onPayCard = { showPaymentSheet = true },
                    onNewInstallmentPlan = { showInstallmentPlanSheet = true },
                    installmentPlansState = installmentPlansViewModel.uiState,
                    payingInstallmentId = installmentPlansViewModel.payingInstallmentId,
                    payError = installmentPlansViewModel.payError,
                    onRetryInstallmentPlans = { installmentPlansViewModel.load(installmentsFallbackError) },
                    onPayInstallment = { planId, installmentId ->
                        installmentPlansViewModel.payInstallment(planId, installmentId, installmentsFallbackError)
                    },
                    onEditPlan = { plan -> editingPlan = plan },
                    editablePurchaseIds = state.transactions
                        .filter { it.type == TransactionType.CREDIT_CARD_PURCHASE && it.id !in planTransactionIds }
                        .map { it.id }
                        .toSet(),
                    onEditPurchase = { transaction -> editingPurchase = transaction },
                )
            }
        }

        if (showInstallmentPlanSheet && state is CardDetailUiState.Success) {
            CreateInstallmentPlanSheet(
                cardId = cardId,
                installmentRepository = installmentRepository,
                categoryRepository = categoryRepository,
                categories = state.categories,
                onDismiss = { showInstallmentPlanSheet = false },
                onSaved = {
                    showInstallmentPlanSheet = false
                    viewModel.refresh(fallbackError)
                    installmentPlansViewModel.refresh(installmentsFallbackError)
                },
            )
        }

        if (showPurchaseSheet && state is CardDetailUiState.Success) {
            CreditCardPurchaseSheet(
                cardId = cardId,
                creditCardRepository = creditCardRepository,
                categoryRepository = categoryRepository,
                categories = state.categories,
                onDismiss = { showPurchaseSheet = false },
                onSaved = {
                    showPurchaseSheet = false
                    viewModel.refresh(fallbackError)
                },
            )
        }

        if (showPaymentSheet && state is CardDetailUiState.Success) {
            CreditCardPaymentSheet(
                cardId = cardId,
                creditCardRepository = creditCardRepository,
                accounts = state.accounts.filter { it.status == AccountStatus.ACTIVE && it.type != AccountType.CREDIT_CARD },
                onDismiss = { showPaymentSheet = false },
                onSaved = {
                    showPaymentSheet = false
                    viewModel.refresh(fallbackError)
                    // Un pago puede marcar cuotas MSI/MCI del mes corriente como pagadas (nexora-api).
                    installmentPlansViewModel.refresh(installmentsFallbackError)
                },
            )
        }

        val purchaseToEdit = editingPurchase
        if (purchaseToEdit != null && state is CardDetailUiState.Success) {
            EditCreditCardPurchaseSheet(
                cardId = cardId,
                purchase = purchaseToEdit,
                creditCardRepository = creditCardRepository,
                categoryRepository = categoryRepository,
                categories = state.categories,
                onDismiss = { editingPurchase = null },
                onSaved = {
                    editingPurchase = null
                    viewModel.refresh(fallbackError)
                },
            )
        }

        val planToEdit = editingPlan
        if (planToEdit != null && state is CardDetailUiState.Success) {
            EditInstallmentPlanSheet(
                plan = planToEdit,
                installmentRepository = installmentRepository,
                categoryRepository = categoryRepository,
                categories = state.categories,
                onDismiss = { editingPlan = null },
                onSaved = {
                    editingPlan = null
                    viewModel.refresh(fallbackError)
                    installmentPlansViewModel.refresh(installmentsFallbackError)
                },
            )
        }
    }
}

@Composable
private fun CardDetailContent(
    state: CardDetailUiState.Success,
    onRecordPurchase: () -> Unit,
    onPayCard: () -> Unit,
    onNewInstallmentPlan: () -> Unit,
    installmentPlansState: InstallmentPlansUiState,
    payingInstallmentId: String?,
    payError: String?,
    onRetryInstallmentPlans: () -> Unit,
    onPayInstallment: (planId: String, installmentId: String) -> Unit,
    onEditPlan: (InstallmentPlan) -> Unit,
    editablePurchaseIds: Set<String>,
    onEditPurchase: (Transaction) -> Unit,
) {
    val card = state.card
    val usage = if (card.creditLimit > 0) (card.currentDebt / card.creditLimit).coerceIn(0.0, 1.0).toFloat() else 0f
    val categoryNameById = remember(state.categories) { state.categories.associate { it.id to it.name } }

    LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
        item {
            Text(card.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                "${card.bank} · •••• ${card.last4}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LinearProgressIndicator(
                progress = { usage },
                color = if (usage >= 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(stringResource(R.string.cards_debt), formatCurrency(card.currentDebt), Modifier.weight(1f))
                StatTile(stringResource(R.string.cards_available), formatCurrency(card.availableCredit), Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(stringResource(R.string.cards_limit), formatCurrency(card.creditLimit), Modifier.weight(1f))
                StatTile(stringResource(R.string.cards_next_payment), formatDateShort(card.nextPaymentDueDate), Modifier.weight(1f))
            }

            Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onRecordPurchase, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.AddShoppingCart, contentDescription = null, modifier = Modifier.height(18.dp))
                    Text(stringResource(R.string.cards_detail_record_purchase), modifier = Modifier.padding(start = 6.dp))
                }
                Button(onClick = onPayCard, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.height(18.dp))
                    Text(stringResource(R.string.cards_detail_pay_card), modifier = Modifier.padding(start = 6.dp))
                }
            }

            OutlinedButton(onClick = onNewInstallmentPlan, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Icon(Icons.Filled.EventRepeat, contentDescription = null, modifier = Modifier.height(18.dp))
                Text(stringResource(R.string.installments_new_plan), modifier = Modifier.padding(start = 6.dp))
            }

            Text(
                stringResource(R.string.installments_heading),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 28.dp, bottom = 8.dp),
            )
            InstallmentPlansSection(
                state = installmentPlansState,
                payingInstallmentId = payingInstallmentId,
                payError = payError,
                onRetry = onRetryInstallmentPlans,
                onPayInstallment = onPayInstallment,
                onEditPlan = onEditPlan,
            )

            Text(
                stringResource(R.string.cards_detail_movements),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 28.dp, bottom = 8.dp),
            )

            if (state.transactions.isEmpty()) {
                Text(
                    stringResource(R.string.cards_detail_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }

        items(state.transactions, key = { it.id }) { transaction ->
            CardTransactionRow(
                transaction = transaction,
                categoryNameById = categoryNameById,
                onEditClick = if (transaction.id in editablePurchaseIds) ({ onEditPurchase(transaction) }) else null,
            )
        }

        item { androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun CardTransactionRow(transaction: Transaction, categoryNameById: Map<String, String>, onEditClick: (() -> Unit)?) {
    val typeLabel = when (transaction.type) {
        TransactionType.CREDIT_CARD_PAYMENT -> stringResource(R.string.cards_type_payment)
        else -> stringResource(R.string.cards_type_purchase)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(transaction.merchant ?: typeLabel, style = MaterialTheme.typography.bodyLarge)
            val subtitle = listOfNotNull(
                formatDateShort(transaction.date),
                transaction.categoryId?.let { categoryNameById[it] },
            ).joinToString(" · ")
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val amountColor = when {
            transaction.balanceEffect > 0 -> NexoraExtendedTheme.colors.income
            transaction.balanceEffect < 0 -> NexoraExtendedTheme.colors.expense
            else -> MaterialTheme.colorScheme.onSurface
        }
        val sign = if (transaction.balanceEffect >= 0) "+" else ""
        Text("$sign${formatCurrency(transaction.balanceEffect)}", style = MaterialTheme.typography.titleSmall, color = amountColor)
        if (onEditClick != null) {
            IconButton(onClick = onEditClick) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
            }
        }
    }
}
