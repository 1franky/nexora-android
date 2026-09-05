package com.nexora.android.ui.sat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nexora.android.R
import com.nexora.android.data.sat.SatCertificateResponse
import com.nexora.android.data.sat.SatCertificateStatus
import com.nexora.android.data.sat.SatRepository
import com.nexora.android.ui.common.formatDateShort
import com.nexora.android.ui.common.formatDateTimeShort
import com.nexora.android.ui.login.nexoraFieldColors
import com.nexora.android.ui.theme.NexoraExtendedTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.launch

/**
 * Pantalla "Conexión SAT" (A12, plan-integracion-sat.md sección 9): alta de
 * la e.firma o panel de estado según haya o no una conexión ya guardada.
 * Vive dentro de la sección protegida por el bloqueo con huella (A10) — el
 * gate de re-autenticación se dispara justo antes de navegar aquí, ver
 * SettingsScreen.
 */
@Composable
fun SatConnectionScreen(
    satRepository: SatRepository,
    onNavigateBack: () -> Unit,
    onNavigateToInvoices: () -> Unit,
) {
    val viewModel: SatConnectionViewModel = viewModel(
        factory = viewModelFactory { initializer { SatConnectionViewModel(satRepository) } },
    )
    val fallbackError = stringResource(R.string.sat_connection_load_error)
    LaunchedEffect(Unit) { viewModel.load(fallbackError) }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.sat_connection_title), style = MaterialTheme.typography.headlineSmall)
        }

        when (val state = viewModel.uiState) {
            SatConnectionUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is SatConnectionUiState.Error -> Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(state.message, style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { viewModel.load(fallbackError) }, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.retry))
                }
            }
            SatConnectionUiState.NotConnected -> SatOnboardingForm(viewModel = viewModel)
            is SatConnectionUiState.Connected -> SatStatusPanel(
                certificate = state.certificate,
                viewModel = viewModel,
                onNavigateToInvoices = onNavigateToInvoices,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SatOnboardingForm(viewModel: SatConnectionViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val connectError = stringResource(R.string.sat_connection_connect_error)
    val missingFilesMessage = stringResource(R.string.sat_connection_missing_files)

    var cerUri by remember { mutableStateOf<Uri?>(null) }
    var cerName by remember { mutableStateOf<String?>(null) }
    var keyUri by remember { mutableStateOf<Uri?>(null) }
    var keyName by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var isPreparingFiles by remember { mutableStateOf(false) }

    // Sin filtro de mimeType estricto a propósito: muchos proveedores de documentos
    // reportan .cer/.key como application/octet-stream (o no los reconocen en
    // absoluto), y un filtro específico los ocultaría del selector.
    val cerPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            cerUri = uri
            cerName = queryDisplayName(context, uri)
        }
    }
    val keyPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            keyUri = uri
            keyName = queryDisplayName(context, uri)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.sat_connection_intro), style = MaterialTheme.typography.bodyMedium)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NexoraExtendedTheme.colors.accentContainer, RoundedCornerShape(12.dp))
                .padding(12.dp),
        ) {
            Text(stringResource(R.string.sat_connection_consent), style = MaterialTheme.typography.bodySmall)
        }

        FilePickerRow(
            label = stringResource(R.string.sat_connection_cer_label),
            fileName = cerName,
            onPick = { cerPicker.launch(arrayOf("*/*")) },
        )
        FilePickerRow(
            label = stringResource(R.string.sat_connection_key_label),
            fileName = keyName,
            onPick = { keyPicker.launch(arrayOf("*/*")) },
        )

        OutlinedTextField(
            value = viewModel.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text(stringResource(R.string.sat_connection_password_label)) },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val description = stringResource(
                    if (passwordVisible) R.string.password_toggle_hide else R.string.password_toggle_show,
                )
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = description,
                    )
                }
            },
            colors = nexoraFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )

        val error = localError ?: viewModel.submitError
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                val cer = cerUri
                val key = keyUri
                if (cer == null || key == null) {
                    localError = missingFilesMessage
                    return@Button
                }
                localError = null
                isPreparingFiles = true
                scope.launch {
                    val cerBytes = readBytes(context, cer)
                    val keyBytes = readBytes(context, key)
                    isPreparingFiles = false
                    if (cerBytes == null || keyBytes == null) {
                        localError = missingFilesMessage
                        return@launch
                    }
                    viewModel.connect(cerBytes, cerName ?: "certificado.cer", keyBytes, keyName ?: "llave.key", connectError)
                }
            },
            enabled = !viewModel.isSubmitting && !isPreparingFiles && viewModel.password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (viewModel.isSubmitting || isPreparingFiles) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(R.string.sat_connection_submit))
            }
        }
    }
}

@Composable
private fun FilePickerRow(label: String, fileName: String?, onPick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onPick) {
                Text(stringResource(R.string.sat_connection_select_file))
            }
            if (fileName != null) {
                Text(
                    text = stringResource(R.string.sat_connection_file_selected, fileName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SatStatusPanel(
    certificate: SatCertificateResponse,
    viewModel: SatConnectionViewModel,
    onNavigateToInvoices: () -> Unit,
) {
    val syncQueued = stringResource(R.string.sat_status_sync_queued)
    val syncStarted = stringResource(R.string.sat_status_sync_started)
    val syncError = stringResource(R.string.sat_status_sync_error)

    var rangeFrom by remember { mutableStateOf<LocalDate?>(null) }
    var rangeTo by remember { mutableStateOf<LocalDate?>(null) }
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatusRow(stringResource(R.string.sat_status_rfc), certificate.rfc)
        StatusRow(stringResource(R.string.sat_status_valid_until), formatDateTimeShort(certificate.validUntil))
        StatusRow(
            stringResource(R.string.sat_status_last_sync),
            certificate.lastSyncAt?.let { formatDateTimeShort(it) } ?: stringResource(R.string.sat_status_last_sync_never),
        )
        StatusBadge(certificate.status)

        if (certificate.status == SatCertificateStatus.ERROR_AUTENTICACION) {
            Text(
                stringResource(R.string.sat_status_auth_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        HorizontalDivider()

        Button(
            onClick = { viewModel.sync(null, null, syncQueued, syncStarted, syncError) },
            enabled = !viewModel.isSyncing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.sat_status_sync_now))
        }

        Text(stringResource(R.string.sat_status_sync_range_title), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = rangeFrom?.let { formatDateShort(it.toString()) } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.sat_status_sync_range_from)) },
                trailingIcon = {
                    IconButton(onClick = { showFromPicker = true }) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = stringResource(R.string.sat_status_sync_range_from))
                    }
                },
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = rangeTo?.let { formatDateShort(it.toString()) } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.sat_status_sync_range_to)) },
                trailingIcon = {
                    IconButton(onClick = { showToPicker = true }) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = stringResource(R.string.sat_status_sync_range_to))
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedButton(
            onClick = {
                val from = rangeFrom
                val to = rangeTo
                if (from != null && to != null) {
                    viewModel.sync(isoStartOfDay(from), isoEndOfDay(to), syncQueued, syncStarted, syncError)
                }
            },
            enabled = !viewModel.isSyncing && rangeFrom != null && rangeTo != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.sat_status_sync_range_submit))
        }

        if (viewModel.syncMessage != null) {
            Text(viewModel.syncMessage!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        HorizontalDivider()

        ContraparteSection(viewModel = viewModel)

        HorizontalDivider()

        Button(onClick = onNavigateToInvoices, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sat_status_view_invoices))
        }

        OutlinedButton(
            onClick = viewModel::requestDelete,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.sat_status_delete_connection))
        }
    }

    if (showFromPicker) {
        DateOnlyPickerDialog(onDismiss = { showFromPicker = false }, onConfirm = { rangeFrom = it; showFromPicker = false })
    }
    if (showToPicker) {
        DateOnlyPickerDialog(onDismiss = { showToPicker = false }, onConfirm = { rangeTo = it; showToPicker = false })
    }

    if (viewModel.pendingDeleteConfirmation) {
        val deleteError = stringResource(R.string.sat_status_delete_error)
        AlertDialog(
            onDismissRequest = { if (!viewModel.isDeleting) viewModel.cancelDelete() },
            title = { Text(stringResource(R.string.sat_status_delete_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (viewModel.deleteError != null) {
                        Text(viewModel.deleteError!!, color = MaterialTheme.colorScheme.error)
                    }
                    Text(stringResource(R.string.sat_status_delete_confirm_message))
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete(deleteError) }, enabled = !viewModel.isDeleting) {
                    Text(stringResource(R.string.sat_status_delete_connection))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }, enabled = !viewModel.isDeleting) {
                    Text(stringResource(R.string.back))
                }
            },
        )
    }
}

/**
 * RFC de terceros que le facturan al usuario (A13): el SAT exige el RFC
 * específico del emisor para descargar CFDI RECIBIDAS — no existe forma de
 * pedir "todo lo que me han facturado" en una sola solicitud, así que el
 * usuario tiene que registrar aquí a su empleador, sus proveedores, etc.
 */
@Composable
private fun ContraparteSection(viewModel: SatConnectionViewModel) {
    val loadError = stringResource(R.string.sat_contraparte_load_error)
    val addError = stringResource(R.string.sat_contraparte_add_error)
    val deleteError = stringResource(R.string.sat_contraparte_delete_error)

    LaunchedEffect(Unit) { viewModel.loadContrapartes(loadError) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.sat_contraparte_section_title), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.sat_contraparte_section_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (viewModel.contrapartesError != null) {
            Text(viewModel.contrapartesError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        viewModel.contrapartes.forEach { contraparte ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(contraparte.alias ?: contraparte.rfc, style = MaterialTheme.typography.bodyMedium)
                    if (contraparte.alias != null) {
                        Text(contraparte.rfc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (viewModel.deletingContraparteId == contraparte.id) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    IconButton(onClick = { viewModel.deleteContraparte(contraparte.id, deleteError) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.sat_contraparte_delete))
                    }
                }
            }
        }

        OutlinedTextField(
            value = viewModel.newContraparteRfc,
            onValueChange = viewModel::onNewContraparteRfcChange,
            label = { Text(stringResource(R.string.sat_contraparte_rfc_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = viewModel.newContraparteAlias,
            onValueChange = viewModel::onNewContraparteAliasChange,
            label = { Text(stringResource(R.string.sat_contraparte_alias_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (viewModel.addContraparteError != null) {
            Text(viewModel.addContraparteError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        OutlinedButton(
            onClick = { viewModel.addContraparte(addError) },
            enabled = !viewModel.isAddingContraparte && viewModel.newContraparteRfc.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (viewModel.isAddingContraparte) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text(stringResource(R.string.sat_contraparte_add))
            }
        }
    }
}

/** No es private: SatInvoicesScreen también la usa para sus filtros de fecha. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateOnlyPickerDialog(onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                } ?: onDismiss()
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.back)) } },
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StatusBadge(status: SatCertificateStatus) {
    val (label, color) = when (status) {
        SatCertificateStatus.ACTIVO -> stringResource(R.string.sat_status_active) to NexoraExtendedTheme.colors.income
        SatCertificateStatus.ERROR_AUTENTICACION -> stringResource(R.string.sat_status_error_short) to MaterialTheme.colorScheme.error
        SatCertificateStatus.REVOCADO -> stringResource(R.string.sat_status_revoked) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = color)
    }
}
