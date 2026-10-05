package com.nexora.android.data.scheduledcharge

import com.nexora.android.data.common.apiCall
import com.nexora.android.data.offline.OfflineCache
import com.nexora.android.data.offline.cachedApiCall
import java.util.UUID

private fun cacheKeyScheduledCharges(accountId: String?) = "scheduled-charges:${accountId ?: "all"}"

/**
 * Cargos programados (A15). Offline (A8): la lista se lee del caché sin
 * conexión; las escrituras (alta, edición, pausa, reanudación, cancelación)
 * NO se encolan — requieren conexión, igual que la edición de planes MSI/MCI:
 * el alta puede registrar de inmediato cargos atrasados y la reanudación
 * puede fallar con un 400 legible (cuenta/categoría inválida), así que el
 * usuario tiene que ver el resultado real del backend en el momento.
 */
class ScheduledChargeRepository(
    private val scheduledChargeApi: ScheduledChargeApi,
    private val offlineCache: OfflineCache,
) {
    suspend fun list(accountId: String?, fallbackError: String): List<ScheduledCharge> =
        cachedApiCall(offlineCache, cacheKeyScheduledCharges(accountId), fallbackError) { scheduledChargeApi.list(accountId) }

    /**
     * Idempotency-Key nueva por intento de alta (IdempotencyFilter en nexora-api): un doble
     * toque no debería crear dos cargos — el ViewModel ya bloquea el botón mientras guarda.
     */
    suspend fun create(request: ScheduledChargeRequest, fallbackError: String): ScheduledCharge =
        apiCall(fallbackError) { scheduledChargeApi.create(UUID.randomUUID().toString(), request) }

    suspend fun update(id: String, request: ScheduledChargeRequest, fallbackError: String): ScheduledCharge =
        apiCall(fallbackError) { scheduledChargeApi.update(id, request) }

    suspend fun pause(id: String, fallbackError: String): ScheduledCharge =
        apiCall(fallbackError) { scheduledChargeApi.pause(id) }

    suspend fun resume(id: String, fallbackError: String): ScheduledCharge =
        apiCall(fallbackError) { scheduledChargeApi.resume(id) }

    suspend fun cancel(id: String, fallbackError: String) {
        apiCall(fallbackError) { scheduledChargeApi.cancel(id) }
    }
}
