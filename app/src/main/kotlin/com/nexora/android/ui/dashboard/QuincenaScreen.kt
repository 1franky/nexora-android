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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nexora.android.R
import com.nexora.android.data.dashboard.DashboardRepository
import com.nexora.android.ui.common.formatCurrency
import java.time.LocalDate

/**
 * Resumen de la quincena en curso (1-15 o 16-fin de mes, según hoy): suma y
 * desglosa por tarjeta los pagos próximos (dashboard.upcomingPayments) cuya
 * fecha límite cae dentro de esa mitad del mes — ver [currentQuincena].
 */
@Composable
fun QuincenaScreen(
    dashboardRepository: DashboardRepository,
    onNavigateBack: () -> Unit,
    onCardClick: (String) -> Unit,
) {
    val viewModel: DashboardSummaryViewModel = viewModel(
        factory = viewModelFactory { initializer { DashboardSummaryViewModel(dashboardRepository) } },
    )
    val fallbackError = stringResource(R.string.dashboard_load_error)
    LaunchedEffect(Unit) { viewModel.load(fallbackError) }

    val today = remember { LocalDate.now() }
    val quincena = remember(today) { currentQuincena(today) }
    val isFirstHalf = today.dayOfMonth <= 15

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Column {
                Text(stringResource(R.string.quincena_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(
                        if (isFirstHalf) R.string.quincena_subtitle_first_half else R.string.quincena_subtitle_second_half
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
                val payments = remember(state.dashboard.upcomingPayments, quincena) {
                    state.dashboard.upcomingPayments.inQuincena(quincena)
                }
                if (payments.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.quincena_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                    }
                } else {
                    val total = payments.sumOf { it.expectedPayment }
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Text(
                            stringResource(R.string.dashboard_quincena),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(formatCurrency(total), style = MaterialTheme.typography.headlineMedium)
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
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
