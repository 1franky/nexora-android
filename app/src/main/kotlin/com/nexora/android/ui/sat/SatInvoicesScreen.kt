package com.nexora.android.ui.sat

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nexora.android.R
import com.nexora.android.data.common.ApiException
import com.nexora.android.data.sat.CfdiEstadoSat
import com.nexora.android.data.sat.CfdiInvoiceResponse
import com.nexora.android.data.sat.CfdiTipo
import com.nexora.android.data.sat.SatRepository
import com.nexora.android.ui.common.formatCurrency
import com.nexora.android.ui.common.formatDateTimeShort
import com.nexora.android.ui.theme.NexoraExtendedTheme
import kotlinx.coroutines.launch

/**
 * Listado de facturas SAT (A12): filtros de tipo/fecha + buscador de texto,
 * descarga/comparte el XML de cada factura. Igual que SatConnectionScreen,
 * vive dentro de la sección protegida por el bloqueo con huella (A10).
 */
@Composable
fun SatInvoicesScreen(
    satRepository: SatRepository,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: SatInvoicesViewModel = viewModel(
        factory = viewModelFactory { initializer { SatInvoicesViewModel(satRepository) } },
    )
    val fallbackError = stringResource(R.string.sat_invoices_load_error)
    val downloadError = stringResource(R.string.sat_invoices_download_error)
    LaunchedEffect(Unit) { viewModel.search(fallbackError) }

    var downloadErrorMessage by remember { mutableStateOf<String?>(null) }
    var pendingXmlDownloadInvoice by remember { mutableStateOf<CfdiInvoiceResponse?>(null) }
    var pendingPdfDownloadInvoice by remember { mutableStateOf<CfdiInvoiceResponse?>(null) }

    val createXmlDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/xml")) { uri ->
        val invoice = pendingXmlDownloadInvoice
        pendingXmlDownloadInvoice = null
        if (uri != null && invoice != null) {
            scope.launch {
                try {
                    val bytes = viewModel.downloadXml(invoice.id, downloadError)
                    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                } catch (e: ApiException) {
                    downloadErrorMessage = e.message ?: downloadError
                }
            }
        }
    }

    /** Mismo mecanismo que el XML, apuntando al endpoint de la representación impresa (B13/A14) — la elección de nombre de archivo abre el selector del sistema con mime type PDF. */
    val createPdfDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val invoice = pendingPdfDownloadInvoice
        pendingPdfDownloadInvoice = null
        if (uri != null && invoice != null) {
            scope.launch {
                try {
                    val bytes = viewModel.downloadPdf(invoice.id, downloadError)
                    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                } catch (e: ApiException) {
                    downloadErrorMessage = e.message ?: downloadError
                }
            }
        }
    }

    fun shareInvoiceXml(invoice: CfdiInvoiceResponse) {
        scope.launch {
            try {
                val bytes = viewModel.downloadXml(invoice.id, downloadError)
                val intent = shareXmlIntent(context, "${invoice.uuidFiscal}.xml", bytes)
                context.startActivity(Intent.createChooser(intent, null))
            } catch (e: ApiException) {
                downloadErrorMessage = e.message ?: downloadError
            }
        }
    }

    fun shareInvoicePdf(invoice: CfdiInvoiceResponse) {
        scope.launch {
            try {
                val bytes = viewModel.downloadPdf(invoice.id, downloadError)
                val intent = sharePdfIntent(context, "${invoice.uuidFiscal}.pdf", bytes)
                context.startActivity(Intent.createChooser(intent, null))
            } catch (e: ApiException) {
                downloadErrorMessage = e.message ?: downloadError
            }
        }
    }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.sat_invoices_title), style = MaterialTheme.typography.headlineSmall)
        }

        SatInvoiceFilters(viewModel = viewModel, onApply = { viewModel.search(fallbackError) })

        if (downloadErrorMessage != null) {
            Text(
                downloadErrorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        when (val state = viewModel.uiState) {
            SatInvoicesUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is SatInvoicesUiState.Error -> Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(state.message, style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { viewModel.search(fallbackError) }, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.retry))
                }
            }
            is SatInvoicesUiState.Success -> if (state.invoices.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.sat_invoices_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(state.invoices, key = { it.id }) { invoice ->
                        InvoiceRow(
                            invoice = invoice,
                            onDownloadPdf = {
                                pendingPdfDownloadInvoice = invoice
                                createPdfDocumentLauncher.launch("${invoice.uuidFiscal}.pdf")
                            },
                            onDownloadXml = {
                                pendingXmlDownloadInvoice = invoice
                                createXmlDocumentLauncher.launch("${invoice.uuidFiscal}.xml")
                            },
                            onSharePdf = { shareInvoicePdf(invoice) },
                            onShareXml = { shareInvoiceXml(invoice) },
                        )
                    }
                    if (state.hasMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                if (state.isLoadingMore) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    Button(onClick = { viewModel.loadMore(fallbackError) }) {
                                        Text(stringResource(R.string.sat_invoices_load_more))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SatInvoiceFilters(viewModel: SatInvoicesViewModel, onApply: () -> Unit) {
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var showDesdePicker by remember { mutableStateOf(false) }
    var showHastaPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = viewModel.texto,
            onValueChange = viewModel::onTextoChange,
            label = { Text(stringResource(R.string.sat_invoices_search_label)) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onApply() }),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box {
                val typeLabel = when (viewModel.tipo) {
                    CfdiTipo.EMITIDAS -> stringResource(R.string.sat_invoices_filter_type_emitidas)
                    CfdiTipo.RECIBIDAS -> stringResource(R.string.sat_invoices_filter_type_recibidas)
                    null -> stringResource(R.string.sat_invoices_filter_type_all)
                }
                Button(onClick = { typeMenuExpanded = true }) {
                    Icon(Icons.Filled.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(typeLabel, modifier = Modifier.padding(start = 6.dp))
                }
                DropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sat_invoices_filter_type_all)) },
                        onClick = { typeMenuExpanded = false; viewModel.onTipoChange(null); onApply() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sat_invoices_filter_type_emitidas)) },
                        onClick = { typeMenuExpanded = false; viewModel.onTipoChange(CfdiTipo.EMITIDAS); onApply() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sat_invoices_filter_type_recibidas)) },
                        onClick = { typeMenuExpanded = false; viewModel.onTipoChange(CfdiTipo.RECIBIDAS); onApply() },
                    )
                }
            }

            IconButton(onClick = { showDesdePicker = true }) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = stringResource(R.string.sat_invoices_filter_from))
            }
            IconButton(onClick = { showHastaPicker = true }) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = stringResource(R.string.sat_invoices_filter_to))
            }
        }

        if (viewModel.desde != null || viewModel.hasta != null) {
            Text(
                text = "${stringResource(R.string.sat_invoices_filter_from)}: ${viewModel.desde ?: "—"}   " +
                    "${stringResource(R.string.sat_invoices_filter_to)}: ${viewModel.hasta ?: "—"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showDesdePicker) {
        DateOnlyPickerDialog(
            onDismiss = { showDesdePicker = false },
            onConfirm = { date -> showDesdePicker = false; viewModel.onDesdeChange(date); onApply() },
        )
    }
    if (showHastaPicker) {
        DateOnlyPickerDialog(
            onDismiss = { showHastaPicker = false },
            onConfirm = { date -> showHastaPicker = false; viewModel.onHastaChange(date); onApply() },
        )
    }
}

/**
 * Cada factura ofrece PDF (representación impresa, B13/A14) y XML (documento
 * fiscal original) tanto para descargar como para compartir — ambos formatos
 * se conservan, ninguno reemplaza al otro. Con la fila ya apretada (avatar +
 * contraparte + monto), se usa un DropdownMenu por acción en vez de sumar más
 * íconos; el PDF va primero en cada menú por ser la opción más usada.
 */
@Composable
private fun InvoiceRow(
    invoice: CfdiInvoiceResponse,
    onDownloadPdf: () -> Unit,
    onDownloadXml: () -> Unit,
    onSharePdf: () -> Unit,
    onShareXml: () -> Unit,
) {
    val isEmitida = invoice.tipo == CfdiTipo.EMITIDAS
    val counterpartyName = (if (isEmitida) invoice.nombreReceptor else invoice.nombreEmisor)
        ?: (if (isEmitida) invoice.rfcReceptor else invoice.rfcEmisor)
    val tint = if (isEmitida) NexoraExtendedTheme.colors.income else NexoraExtendedTheme.colors.expense

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(tint.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isEmitida) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(counterpartyName, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(
                formatDateTimeShort(invoice.fechaEmision),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (invoice.estadoSat == CfdiEstadoSat.CANCELADO) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        stringResource(R.string.sat_invoices_cancelled),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        Text(formatCurrency(invoice.total), style = MaterialTheme.typography.titleSmall)

        var downloadMenuExpanded by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { downloadMenuExpanded = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = stringResource(R.string.sat_invoices_download),
                    modifier = Modifier.size(18.dp),
                )
            }
            DropdownMenu(expanded = downloadMenuExpanded, onDismissRequest = { downloadMenuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sat_invoices_download_pdf)) },
                    leadingIcon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null) },
                    onClick = { downloadMenuExpanded = false; onDownloadPdf() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sat_invoices_download_xml)) },
                    leadingIcon = { Icon(Icons.Filled.Description, contentDescription = null) },
                    onClick = { downloadMenuExpanded = false; onDownloadXml() },
                )
            }
        }

        var shareMenuExpanded by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { shareMenuExpanded = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = stringResource(R.string.sat_invoices_share),
                    modifier = Modifier.size(18.dp),
                )
            }
            DropdownMenu(expanded = shareMenuExpanded, onDismissRequest = { shareMenuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sat_invoices_share_pdf)) },
                    leadingIcon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null) },
                    onClick = { shareMenuExpanded = false; onSharePdf() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sat_invoices_share_xml)) },
                    leadingIcon = { Icon(Icons.Filled.Description, contentDescription = null) },
                    onClick = { shareMenuExpanded = false; onShareXml() },
                )
            }
        }
    }
}
