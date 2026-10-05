package com.nexora.android.ui.scheduledcharges

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nexora.android.R
import com.nexora.android.data.account.AccountRepository
import com.nexora.android.data.category.CategoryRepository
import com.nexora.android.data.scheduledcharge.ScheduledCharge
import com.nexora.android.data.scheduledcharge.ScheduledChargeRepository

/**
 * «Cargos programados» (A15): todos los del usuario, o solo los de una cuenta
 * si llega [accountId] (acceso desde Cuentas). El FAB da de alta uno nuevo,
 * con esa cuenta preseleccionada si la hay.
 */
@Composable
fun ScheduledChargesScreen(
    accountId: String?,
    scheduledChargeRepository: ScheduledChargeRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    onNavigateBack: () -> Unit,
) {
    val viewModel: ScheduledChargesViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ScheduledChargesViewModel(scheduledChargeRepository, accountRepository, categoryRepository) }
        },
    )
    val fallbackError = stringResource(R.string.scheduled_charges_load_error)
    val actionFallbackError = stringResource(R.string.scheduled_charges_action_error)
    LaunchedEffect(accountId) { viewModel.load(accountId, fallbackError) }

    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ScheduledCharge?>(null) }
    var cancelling by remember { mutableStateOf<ScheduledCharge?>(null) }
    val state = viewModel.uiState

    val accountName = (state as? ScheduledChargesUiState.Success)?.let { success ->
        accountId?.let { id -> success.accounts.firstOrNull { it.id == id }?.name }
    }

    Scaffold(
        floatingActionButton = {
            if (state is ScheduledChargesUiState.Success) {
                FloatingActionButton(onClick = { creating = true }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.scheduled_charges_new))
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
                    Column {
                        Text(stringResource(R.string.scheduled_charges_title), style = MaterialTheme.typography.headlineSmall)
                        if (accountName != null) {
                            Text(accountName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                when (state) {
                    ScheduledChargesUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is ScheduledChargesUiState.Error -> Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(state.message, style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { viewModel.load(accountId, fallbackError) }, modifier = Modifier.padding(top = 16.dp)) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                    is ScheduledChargesUiState.Success -> {
                        if (state.charges.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    stringResource(R.string.scheduled_charges_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 32.dp),
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                viewModel.actionError?.let { error ->
                                    item(key = "action-error") {
                                        Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                                items(state.charges, key = { it.id }) { charge ->
                                    ScheduledChargeCard(
                                        charge = charge,
                                        showAccount = accountId == null,
                                        busy = viewModel.busyChargeId == charge.id,
                                        onPause = { viewModel.pause(charge.id, actionFallbackError, fallbackError) },
                                        onResume = { viewModel.resume(charge.id, actionFallbackError, fallbackError) },
                                        onEdit = { editing = charge },
                                        onCancel = { cancelling = charge },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state is ScheduledChargesUiState.Success) {
                if (creating) {
                    ScheduledChargeFormSheet(
                        existing = null,
                        initialAccountId = accountId,
                        accounts = state.accounts,
                        categories = state.categories,
                        scheduledChargeRepository = scheduledChargeRepository,
                        categoryRepository = categoryRepository,
                        onDismiss = { creating = false },
                        onSaved = {
                            creating = false
                            viewModel.refresh(fallbackError)
                        },
                    )
                }
                editing?.let { charge ->
                    ScheduledChargeFormSheet(
                        existing = charge,
                        initialAccountId = null,
                        accounts = state.accounts,
                        categories = state.categories,
                        scheduledChargeRepository = scheduledChargeRepository,
                        categoryRepository = categoryRepository,
                        onDismiss = { editing = null },
                        onSaved = {
                            editing = null
                            viewModel.refresh(fallbackError)
                        },
                    )
                }
            }

            cancelling?.let { charge ->
                CancelScheduledChargeDialog(
                    charge = charge,
                    onConfirm = {
                        cancelling = null
                        viewModel.cancel(charge.id, actionFallbackError, fallbackError)
                    },
                    onDismiss = { cancelling = null },
                )
            }
        }
    }
}
