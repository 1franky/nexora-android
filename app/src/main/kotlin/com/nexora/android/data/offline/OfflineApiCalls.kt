package com.nexora.android.data.offline

import com.nexora.android.data.common.ApiException
import com.nexora.android.data.common.extractMessage
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

/**
 * Igual que apiCall (data.common.ApiCall), pero para un GET que además debe
 * poder mostrar datos sin conexión: si la llamada tiene éxito, refresca el
 * caché bajo [cacheKey]; si falla por falta de conexión, devuelve lo último
 * guardado ahí en vez de fallar. Un error real del backend (HttpException)
 * sigue propagándose como [ApiException] — no tiene sentido tapar un 404 o
 * un 400 con datos viejos.
 */
suspend inline fun <reified T> cachedApiCall(
    offlineCache: OfflineCache,
    cacheKey: String,
    fallbackMessage: String,
    block: suspend () -> T,
): T {
    try {
        val result = block()
        offlineCache.put(cacheKey, result)
        return result
    } catch (e: HttpException) {
        throw ApiException(extractMessage(e.response()?.errorBody()) ?: fallbackMessage)
    } catch (e: IOException) {
        return offlineCache.get<T>(cacheKey) ?: throw ApiException(fallbackMessage)
    }
}

sealed interface WriteOutcome<out T> {
    data class Applied<T>(val value: T) : WriteOutcome<T>

    /** Sin conexión: la operación quedó guardada en [PendingOperationEntity] para que la sincronice SyncWorker. */
    data object Queued : WriteOutcome<Nothing>
}

/**
 * Igual que apiCall, pero para un POST que además debe poder ejecutarse sin
 * conexión: si falla por [IOException], en vez de propagar el error encola
 * la operación (con una Idempotency-Key nueva, ver IdempotencyFilter en
 * nexora-api) para que la reintente [com.nexora.android.sync.SyncWorker], y
 * devuelve [WriteOutcome.Queued] — no un objeto fabricado que finja el
 * resultado (ver comentario en [PendingOperationEntity] sobre por qué).
 * [block] recibe la Idempotency-Key para mandarla como header.
 */
suspend inline fun <reified P, T> writeCall(
    fallbackMessage: String,
    pendingOperationDao: PendingOperationDao,
    json: Json,
    type: OperationType,
    summary: String,
    payload: P,
    pathParams: String? = null,
    block: suspend (idempotencyKey: String) -> T,
): WriteOutcome<T> {
    val idempotencyKey = UUID.randomUUID().toString()
    return try {
        WriteOutcome.Applied(block(idempotencyKey))
    } catch (e: HttpException) {
        throw ApiException(extractMessage(e.response()?.errorBody()) ?: fallbackMessage)
    } catch (e: IOException) {
        pendingOperationDao.insert(
            PendingOperationEntity(
                idempotencyKey = idempotencyKey,
                type = type.name,
                summary = summary,
                payloadJson = json.encodeToString(payload),
                pathParams = pathParams,
                createdAt = System.currentTimeMillis(),
                status = PendingOperationStatus.PENDING.name,
            ),
        )
        WriteOutcome.Queued
    }
}
