package com.nexora.android.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nexora.android.data.offline.PendingOperationDao
import com.nexora.android.data.offline.PendingOperationStatus
import java.io.IOException

/**
 * Drena [PendingOperationDao] en orden de creación, reintentando cada
 * operación contra la API real con su misma Idempotency-Key (nunca se
 * regenera entre reintentos — así un reintento de una operación que el
 * servidor ya aplicó, pero cuya confirmación no llegó a guardarse
 * localmente, no la duplica: ver IdempotencyFilter en nexora-api). Cómo se
 * reconstruye cada llamada según el tipo vive en [OperationDispatcher].
 *
 * No toca ningún caché de lectura al terminar: la próxima vez que la
 * pantalla correspondiente recargue online, el GET normal ya trae los
 * datos reales y lo refresca solo (ver OfflineCache). Intentarlo aquí
 * requeriría reconstruir, por tipo de operación, cómo insertar la
 * respuesta real dentro de cada lista cacheada — se deja así por ahora.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val pendingOperationDao: PendingOperationDao,
    private val dispatcher: OperationDispatcher,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pending = pendingOperationDao.listByStatus(PendingOperationStatus.PENDING.name)
        for (operation in pending) {
            val result = runCatching { dispatcher.apply(operation) }
            when {
                result.isSuccess -> pendingOperationDao.delete(operation.idempotencyKey)
                result.exceptionOrNull() is IOException -> return Result.retry() // sigue sin conexión: se reintenta más tarde, el resto queda pendiente
                else -> pendingOperationDao.updateStatus(
                    key = operation.idempotencyKey,
                    status = PendingOperationStatus.FAILED.name,
                    attempts = operation.attempts + 1,
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
        return Result.success()
    }
}
