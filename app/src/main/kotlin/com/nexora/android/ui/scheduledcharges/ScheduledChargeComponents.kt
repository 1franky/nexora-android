package com.nexora.android.ui.scheduledcharges

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nexora.android.R
import com.nexora.android.data.scheduledcharge.ScheduledCharge
import com.nexora.android.data.scheduledcharge.ScheduledChargeFrequency
import com.nexora.android.data.scheduledcharge.ScheduledChargeStatus
import com.nexora.android.ui.common.formatCurrency
import com.nexora.android.ui.common.formatDateMedium

/** «Cada día 15» / «Cada 3 de marzo». */
@Composable
fun frequencyLabel(frequency: ScheduledChargeFrequency, dayOfMonth: Int, monthOfYear: Int?): String =
    frequencyDescription(
        frequency = frequency,
        dayOfMonth = dayOfMonth,
        monthOfYear = monthOfYear,
        monthlyTemplate = stringResource(R.string.scheduled_charges_frequency_monthly_desc),
        yearlyTemplate = stringResource(R.string.scheduled_charges_frequency_yearly_desc),
    )

/** "$119 MXN": formatCurrency (es-MX) + el código de la moneda de la cuenta, que puede no ser MXN. */
fun formatAmountWithCurrency(amount: Double, currency: String): String = "${formatCurrency(amount)} $currency"

@Composable
fun ScheduledChargeStatusChip(status: ScheduledChargeStatus) {
    val (label, background, content) = when (status) {
        ScheduledChargeStatus.ACTIVE -> Triple(
            stringResource(R.string.scheduled_charges_status_active),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        ScheduledChargeStatus.PAUSED -> Triple(
            stringResource(R.string.scheduled_charges_status_paused),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        ScheduledChargeStatus.FINISHED -> Triple(
            stringResource(R.string.scheduled_charges_status_finished),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ScheduledChargeStatus.CANCELLED -> Triple(
            stringResource(R.string.scheduled_charges_status_cancelled),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = content,
        modifier = Modifier
            .background(background, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/** Distintivo «Programado» para movimientos generados por un cargo programado (scheduledChargeId != null). */
@Composable
fun ScheduledBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            Icons.Filled.EventRepeat,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(11.dp),
        )
        Text(
            stringResource(R.string.scheduled_charges_badge),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
fun ScheduledChargeCard(
    charge: ScheduledCharge,
    showAccount: Boolean,
    busy: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
) {
    val inactive = charge.status == ScheduledChargeStatus.FINISHED || charge.status == ScheduledChargeStatus.CANCELLED
    val primaryText = if (inactive) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(charge.name, style = MaterialTheme.typography.titleMedium, color = primaryText, modifier = Modifier.weight(1f))
            ScheduledChargeStatusChip(charge.status)
        }
        Text(
            formatAmountWithCurrency(charge.amount, charge.currency),
            style = MaterialTheme.typography.titleSmall,
            color = primaryText,
            modifier = Modifier.padding(top = 4.dp),
        )
        val subtitle = listOfNotNull(
            frequencyLabel(charge.frequency, charge.dayOfMonth, charge.monthOfYear),
            if (showAccount) charge.accountName else null,
        ).joinToString(" · ")
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        val nextRun = charge.nextRunDate
        val scheduleLine = when {
            charge.status == ScheduledChargeStatus.ACTIVE && nextRun != null ->
                stringResource(R.string.scheduled_charges_next_run, formatDateMedium(nextRun))
            charge.endDate != null && charge.status != ScheduledChargeStatus.CANCELLED ->
                stringResource(R.string.scheduled_charges_ends_on, formatDateMedium(charge.endDate))
            else -> null
        }
        if (scheduleLine != null) {
            Text(scheduleLine, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
        charge.lastRunDate?.let {
            Text(
                stringResource(R.string.scheduled_charges_last_run, formatDateMedium(it)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (charge.status == ScheduledChargeStatus.PAUSED && !charge.lastError.isNullOrBlank()) {
            Text(
                charge.lastError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }

        if (!inactive) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (busy) {
                    Box(Modifier.padding(end = 12.dp)) { CircularProgressIndicator(modifier = Modifier.size(20.dp)) }
                }
                if (charge.status == ScheduledChargeStatus.ACTIVE) {
                    TextButton(onClick = onPause, enabled = !busy) { Text(stringResource(R.string.scheduled_charges_action_pause)) }
                } else {
                    TextButton(onClick = onResume, enabled = !busy) { Text(stringResource(R.string.scheduled_charges_action_resume)) }
                }
                TextButton(onClick = onEdit, enabled = !busy) { Text(stringResource(R.string.action_edit)) }
                TextButton(onClick = onCancel, enabled = !busy) {
                    Text(stringResource(R.string.scheduled_charges_action_cancel), color = if (busy) Color.Unspecified else MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/**
 * Contenido de la sección «Cargos programados» dentro del detalle de una
 * tarjeta (carga/error/vacío/lista) — misma forma que InstallmentPlansSection.
 * Las hojas y diálogos viven en la pantalla que la contiene.
 */
@Composable
fun ScheduledChargesSectionContent(
    state: ScheduledChargesUiState,
    busyChargeId: String?,
    actionError: String?,
    onRetry: () -> Unit,
    onPause: (ScheduledCharge) -> Unit,
    onResume: (ScheduledCharge) -> Unit,
    onEdit: (ScheduledCharge) -> Unit,
    onCancel: (ScheduledCharge) -> Unit,
) {
    when (state) {
        ScheduledChargesUiState.Loading -> Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
        }
        is ScheduledChargesUiState.Error -> Column {
            Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
        is ScheduledChargesUiState.Success -> {
            if (state.charges.isEmpty()) {
                Text(
                    stringResource(R.string.scheduled_charges_account_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (actionError != null) {
                        Text(actionError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    state.charges.forEach { charge ->
                        ScheduledChargeCard(
                            charge = charge,
                            showAccount = false,
                            busy = busyChargeId == charge.id,
                            onPause = { onPause(charge) },
                            onResume = { onResume(charge) },
                            onEdit = { onEdit(charge) },
                            onCancel = { onCancel(charge) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CancelScheduledChargeDialog(charge: ScheduledCharge, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.scheduled_charges_cancel_title)) },
        text = { Text(stringResource(R.string.scheduled_charges_cancel_message, charge.name)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.scheduled_charges_cancel_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.back)) } },
    )
}
