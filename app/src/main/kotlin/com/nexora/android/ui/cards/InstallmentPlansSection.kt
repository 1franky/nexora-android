package com.nexora.android.ui.cards

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nexora.android.R
import com.nexora.android.data.installment.Installment
import com.nexora.android.data.installment.InstallmentPlan
import com.nexora.android.data.installment.InstallmentPlanType
import com.nexora.android.data.installment.InstallmentStatus
import com.nexora.android.ui.common.formatCurrency
import com.nexora.android.ui.common.formatDateShort

@Composable
fun InstallmentPlansSection(
    state: InstallmentPlansUiState,
    payingInstallmentId: String?,
    payError: String?,
    onRetry: () -> Unit,
    onPayInstallment: (planId: String, installmentId: String) -> Unit,
    onEditPlan: (InstallmentPlan) -> Unit,
) {
    when (state) {
        InstallmentPlansUiState.Loading -> Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
        }
        is InstallmentPlansUiState.Error -> Column {
            Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
        is InstallmentPlansUiState.Success -> {
            if (state.plans.isEmpty()) {
                Text(
                    stringResource(R.string.installments_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (payError != null) {
                        Text(payError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    state.plans.forEach { plan ->
                        InstallmentPlanCard(
                            plan = plan,
                            payingInstallmentId = payingInstallmentId,
                            onPayInstallment = { installmentId -> onPayInstallment(plan.id, installmentId) },
                            onEditClick = { onEditPlan(plan) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallmentPlanCard(
    plan: InstallmentPlan,
    payingInstallmentId: String?,
    onPayInstallment: (String) -> Unit,
    onEditClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .animateContentSize()
            .padding(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PlanTypeChip(plan.planType)
            Text(formatCurrency(plan.totalAmount), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.installments_paid_of, plan.installmentsPaid, plan.installmentCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (expanded) {
            Column(Modifier.padding(top = 14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SummaryItem(stringResource(R.string.installments_installment_amount), formatCurrency(plan.installmentAmount), Modifier.weight(1f))
                    SummaryItem(stringResource(R.string.installments_financed_balance), formatCurrency(plan.financedBalance), Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SummaryItem(
                        stringResource(R.string.installments_next_installment),
                        plan.nextInstallment?.let { formatDateShort(it.dueDate) } ?: stringResource(R.string.installments_no_next_installment),
                        Modifier.weight(1f),
                    )
                    SummaryItem(stringResource(R.string.installments_end_date), formatDateShort(plan.endDate), Modifier.weight(1f))
                }

                Column(Modifier.padding(top = 14.dp)) {
                    plan.installments.forEach { installment ->
                        InstallmentRow(
                            installment = installment,
                            isPaying = payingInstallmentId == installment.id,
                            onPay = { onPayInstallment(installment.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanTypeChip(planType: InstallmentPlanType) {
    val background = if (planType == InstallmentPlanType.MCI) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val label = if (planType == InstallmentPlanType.MCI) {
        stringResource(R.string.installments_plan_type_mci)
    } else {
        stringResource(R.string.installments_plan_type_msi)
    }
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .background(background, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun SummaryItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun InstallmentRow(installment: Installment, isPaying: Boolean, onPay: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                "${stringResource(R.string.installments_schedule_number)} ${installment.number}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "${stringResource(R.string.installments_schedule_due_date)} ${formatDateShort(installment.dueDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(formatCurrency(installment.amount), style = MaterialTheme.typography.bodyMedium)
            when {
                installment.status == InstallmentStatus.PAID -> Text(
                    stringResource(R.string.installments_installment_status_paid),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
                isPaying -> CircularProgressIndicator(modifier = Modifier.size(18.dp))
                else -> Button(onClick = onPay, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(stringResource(R.string.installments_mark_as_paid), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
