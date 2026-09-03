package com.nexora.android.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.nexora.android.data.dashboard.DashboardRepository
import com.nexora.android.data.dashboard.UpcomingCardPayment
import com.nexora.android.ui.common.formatCurrency
import com.nexora.android.ui.common.formatDateShort

/** Resumen de "próximo pago" (mismo listado que el widget equivalente en nexora-web). */
@Composable
fun UpcomingPaymentsScreen(
    dashboardRepository: DashboardRepository,
    onNavigateBack: () -> Unit,
    onCardClick: (String) -> Unit,
) {
    val viewModel: DashboardSummaryViewModel = viewModel(
        factory = viewModelFactory { initializer { DashboardSummaryViewModel(dashboardRepository) } },
    )
    val fallbackError = stringResource(R.string.dashboard_load_error)
    LaunchedEffect(Unit) { viewModel.load(fallbackError) }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.upcoming_payments_title), style = MaterialTheme.typography.headlineSmall)
        }

        when (val state = viewModel.uiState) {
            DashboardSummaryUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is DashboardSummaryUiState.Error -> Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(state.message, style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { viewModel.load(fallbackError) }, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.retry))
                }
            }
            is DashboardSummaryUiState.Success -> {
                val payments = state.dashboard.upcomingPayments
                if (payments.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.upcoming_payments_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(payments, key = { it.creditCardId }) { payment ->
                            UpcomingPaymentCard(payment, onClick = { onCardClick(payment.creditCardId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun UpcomingPaymentCard(payment: UpcomingCardPayment, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(payment.creditCardName, style = MaterialTheme.typography.titleMedium)
            Text(
                "${stringResource(R.string.upcoming_payments_due_date)}: ${formatDateShort(payment.dueDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(formatCurrency(payment.expectedPayment), style = MaterialTheme.typography.titleMedium)
    }
}
