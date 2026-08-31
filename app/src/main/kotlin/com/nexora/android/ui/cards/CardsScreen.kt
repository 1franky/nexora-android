package com.nexora.android.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nexora.android.R
import com.nexora.android.data.creditcard.CreditCard
import com.nexora.android.data.creditcard.CreditCardRepository
import com.nexora.android.data.creditcard.CreditCardStatus
import com.nexora.android.ui.common.formatCurrency

@Composable
fun CardsScreen(
    creditCardRepository: CreditCardRepository,
    onCardClick: (String) -> Unit,
) {
    val viewModel: CardsViewModel = viewModel(
        factory = viewModelFactory { initializer { CardsViewModel(creditCardRepository) } },
    )
    val fallbackError = stringResource(R.string.cards_load_error)
    LaunchedEffect(Unit) { viewModel.load(fallbackError) }

    var showNewCardSheet by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<CreditCard?>(null) }
    var search by remember { mutableStateOf("") }
    val state = viewModel.uiState

    Scaffold(
        floatingActionButton = {
            if (state is CardsUiState.Success) {
                FloatingActionButton(onClick = { showNewCardSheet = true }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cards_new))
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            Column(Modifier.fillMaxSize()) {
                Text(
                    stringResource(R.string.cards_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                )

                when (state) {
                    CardsUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is CardsUiState.Error -> Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(state.message, style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { viewModel.load(fallbackError) }, modifier = Modifier.padding(top = 16.dp)) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                    is CardsUiState.Success -> {
                        if (state.cards.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    stringResource(R.string.cards_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 32.dp),
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = search,
                                onValueChange = { search = it },
                                label = { Text(stringResource(R.string.cards_search_label)) },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                            )
                            val filteredCards = remember(state.cards, search) {
                                state.cards.filter {
                                    it.name.contains(search, ignoreCase = true) ||
                                        it.bank.contains(search, ignoreCase = true) ||
                                        it.last4.contains(search, ignoreCase = true)
                                }
                            }
                            if (filteredCards.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        stringResource(R.string.cards_search_empty),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 32.dp),
                                    )
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 96.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(filteredCards, key = { it.id }) { card ->
                                        CreditCardRow(card, onClick = { onCardClick(card.id) }, onEditClick = { editingCard = card })
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showNewCardSheet && state is CardsUiState.Success) {
                NewCreditCardSheet(
                    creditCardRepository = creditCardRepository,
                    onDismiss = { showNewCardSheet = false },
                    onSaved = {
                        showNewCardSheet = false
                        viewModel.refresh(fallbackError)
                    },
                )
            }

            editingCard?.let { card ->
                EditCreditCardSheet(
                    card = card,
                    creditCardRepository = creditCardRepository,
                    onDismiss = { editingCard = null },
                    onSaved = {
                        editingCard = null
                        viewModel.refresh(fallbackError)
                    },
                )
            }
        }
    }
}

@Composable
private fun CreditCardRow(card: CreditCard, onClick: () -> Unit, onEditClick: () -> Unit) {
    val archived = card.status == CreditCardStatus.ARCHIVED
    val usage = if (card.creditLimit > 0) (card.currentDebt / card.creditLimit).coerceIn(0.0, 1.0).toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                card.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (archived) {
                    Text(
                        stringResource(R.string.cards_archived),
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
            "${card.bank} · •••• ${card.last4}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
        )
        Text(formatCurrency(card.currentDebt), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.cards_debt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LinearProgressIndicator(
            progress = { usage },
            color = if (usage >= 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${stringResource(R.string.cards_available)} ${formatCurrency(card.availableCredit)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${stringResource(R.string.cards_limit)} ${formatCurrency(card.creditLimit)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
