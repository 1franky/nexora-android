package com.nexora.android.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nexora.android.R
import com.nexora.android.data.auth.AuthRepository
import com.nexora.android.data.dashboard.DashboardRepository
import com.nexora.android.data.user.UserRepository
import com.nexora.android.ui.common.formatCurrency
import com.nexora.android.ui.theme.NexoraExtendedTheme

@Composable
fun DashboardScreen(
    dashboardRepository: DashboardRepository,
    userRepository: UserRepository,
    authRepository: AuthRepository,
) {
    val viewModel: DashboardViewModel = viewModel(
        factory = viewModelFactory { initializer { DashboardViewModel(dashboardRepository, userRepository, authRepository) } },
    )
    val fallbackError = stringResource(R.string.dashboard_load_error)

    LaunchedEffect(Unit) { viewModel.load(fallbackError) }

    when (val state = viewModel.uiState) {
        DashboardUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is DashboardUiState.Error -> Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(state.message, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { viewModel.load(fallbackError) }, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.retry))
            }
        }
        is DashboardUiState.Success -> DashboardContent(state.data, onLogout = viewModel::logout)
    }
}

@Composable
private fun DashboardContent(data: DashboardData, onLogout: () -> Unit) {
    val dashboard = data.dashboard

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text(stringResource(R.string.dashboard_greeting), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(data.displayName, style = MaterialTheme.typography.headlineSmall)
            }
            IconButton(onClick = onLogout) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.logout))
            }
        }

        Spacer(Modifier.height(18.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = stringResource(R.string.dashboard_available),
                value = formatCurrency(dashboard.availableBalance),
                accent = true,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(R.string.dashboard_debt),
                value = formatCurrency(dashboard.creditCardDebt),
                accent = false,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val nextPayment = dashboard.upcomingPayments.firstOrNull()
            StatCard(
                label = stringResource(R.string.dashboard_next_payment),
                value = nextPayment?.let { formatCurrency(it.expectedPayment) } ?: "—",
                caption = nextPayment?.creditCardName,
                accent = false,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(R.string.dashboard_expenses_this_month),
                value = formatCurrency(dashboard.expenseThisMonth),
                accent = false,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.dashboard_quick_actions),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickActionChip(Icons.Filled.ArrowDownward, stringResource(R.string.dashboard_action_income), primary = true)
            QuickActionChip(Icons.Filled.ArrowUpward, stringResource(R.string.dashboard_action_expense), primary = false)
            QuickActionChip(Icons.Filled.SwapHoriz, stringResource(R.string.dashboard_action_transfer), primary = false)
            QuickActionChip(Icons.Filled.ShoppingCart, stringResource(R.string.dashboard_action_purchase), primary = false)
            QuickActionChip(Icons.Filled.CreditCard, stringResource(R.string.dashboard_action_pay), primary = false)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    accent: Boolean,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    val background = if (accent) NexoraExtendedTheme.colors.accentContainer else MaterialTheme.colorScheme.surface
    Column(
        modifier = modifier
            .background(background, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (caption != null) {
            Text(caption, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp))
        } else {
            Spacer(Modifier.height(6.dp))
        }
        Text(value, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun QuickActionChip(icon: ImageVector, label: String, primary: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
