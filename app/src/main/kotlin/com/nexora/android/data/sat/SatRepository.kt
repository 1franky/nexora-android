package com.nexora.android.data.sat

import com.nexora.android.data.common.ApiException
import com.nexora.android.data.common.apiCall
import com.nexora.android.data.common.extractMessage
import com.nexora.android.data.offline.OfflineCache
import com.nexora.android.data.offline.OperationType
import com.nexora.android.data.offline.PendingOperationDao
import com.nexora.android.data.offline.WriteOutcome
import com.nexora.android.data.offline.cachedApiCall
import com.nexora.android.data.offline.writeCall
import com.nexora.android.sync.SyncScheduler
import java.io.IOException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

/** Misma clave para cualquier filtro/página: el listado paginado de facturas se cachea completo por combinación de query (A8). */
private fun invoicesCacheKey(tipo: CfdiTipo?, desde: String?, hasta: String?, texto: String?, page: Int, size: Int): String =
    "sat-invoices:tipo=${tipo?.name.orEmpty()}&desde=${desde.orEmpty()}&hasta=${hasta.orEmpty()}&texto=${texto.orEmpty()}&page=$page&size=$size"

class SatRepository(
    private val satApi: SatApi,
    private val offlineCache: OfflineCache,
    private val pendingOperationDao: PendingOperationDao,
    private val syncScheduler: SyncScheduler,
    private val json: Json,
) {

    /**
     * null = el usuario no tiene ninguna e.firma conectada (404 del backend, ver
     * SatApi.getCertificateStatus) — no es un error, es un estado legítimo de
     * "sin conectar" que la pantalla usa para mostrar el formulario de alta.
     */
    suspend fun getCertificateStatus(fallbackError: String): SatCertificateResponse? = try {
        satApi.getCertificateStatus()
    } catch (e: HttpException) {
        if (e.code() == 404) null else throw ApiException(extractMessage(e.response()?.errorBody()) ?: fallbackError)
    } catch (e: IOException) {
        throw ApiException(fallbackError)
    }

    /**
     * Sin caché ni cola offline a propósito (igual que updateCreditCard): el
     * usuario espera ver el resultado (o el error de autenticación contra el
     * SAT) de inmediato, y subir archivos encolados sin conexión no tiene
     * sentido para una operación que ya de por sí tarda varios segundos.
     */
    suspend fun connectCertificate(
        cerBytes: ByteArray,
        cerFileName: String,
        keyBytes: ByteArray,
        keyFileName: String,
        password: String,
        fallbackError: String,
    ): SatCertificateResponse = apiCall(fallbackError) {
        val cerPart = MultipartBody.Part.createFormData(
            "cer", cerFileName, cerBytes.toRequestBody("application/x-x509-ca-cert".toMediaType()),
        )
        val keyPart = MultipartBody.Part.createFormData(
            "key", keyFileName, keyBytes.toRequestBody("application/octet-stream".toMediaType()),
        )
        satApi.connectCertificate(cerPart, keyPart, password)
    }

    /** Borrado real del lado del backend (no soft-delete) — conserva las facturas ya descargadas. */
    suspend fun deleteCertificate(fallbackError: String) = apiCall(fallbackError) { satApi.deleteCertificate() }

    suspend fun listInvoices(
        tipo: CfdiTipo?,
        desde: String?,
        hasta: String?,
        texto: String?,
        page: Int,
        size: Int,
        fallbackError: String,
    ): PageCfdiInvoiceResponse = cachedApiCall(
        offlineCache,
        invoicesCacheKey(tipo, desde, hasta, texto, page, size),
        fallbackError,
    ) {
        satApi.listInvoices(tipo?.name, desde, hasta, texto, page, size)
    }

    suspend fun downloadXml(invoiceId: String, fallbackError: String): ByteArray =
        apiCall(fallbackError) { satApi.downloadXml(invoiceId).bytes() }

    /**
     * Encola vía WorkManager si no hay conexión (mismo patrón writeCall/SyncWorker
     * de A8) — el propio POST /sat/sync responde 202 y corre en background del
     * lado del servidor, así que no hace falta esperar nada más que el disparo
     * llegue; si el dispositivo está offline, se reintenta solo en cuanto vuelva
     * la señal en vez de fallar sin más.
     */
    suspend fun requestSync(desde: String?, hasta: String?, fallbackError: String): WriteOutcome<Unit> {
        val outcome = writeCall(
            fallbackMessage = fallbackError,
            pendingOperationDao = pendingOperationDao,
            json = json,
            type = OperationType.SAT_SYNC,
            summary = if (desde != null && hasta != null) "Sincronizar SAT: $desde a $hasta" else "Sincronizar SAT (incremental)",
            payload = SyncRequest(desde, hasta),
        ) { satApi.sync(SyncRequest(desde, hasta)) }
        if (outcome is WriteOutcome.Queued) syncScheduler.requestSync()
        return outcome
    }
}
