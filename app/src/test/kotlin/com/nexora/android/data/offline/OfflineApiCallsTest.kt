package com.nexora.android.data.offline

import com.nexora.android.data.common.ApiException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@Serializable
private data class Dummy(val value: String)

private const val CACHE_KEY = "dummy"
private const val FALLBACK = "fallback de error"

private fun httpException(status: Int, body: String): HttpException {
    val responseBody = body.toResponseBody("application/json".toMediaType())
    return HttpException(Response.error<Dummy>(status, responseBody))
}

class OfflineApiCallsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `cachedApiCall exitoso devuelve el valor y refresca el cache`() = runTest {
        val cache = OfflineCache(FakeCachedResponseDao(), json)

        val result = cachedApiCall(cache, CACHE_KEY, FALLBACK) { Dummy("del servidor") }

        assertEquals(Dummy("del servidor"), result)
        assertEquals(Dummy("del servidor"), cache.get<Dummy>(CACHE_KEY))
    }

    @Test
    fun `cachedApiCall sin conexion devuelve lo ultimo cacheado`() = runTest {
        val cache = OfflineCache(FakeCachedResponseDao(), json)
        cache.put(CACHE_KEY, Dummy("guardado antes"))

        val result = cachedApiCall<Dummy>(cache, CACHE_KEY, FALLBACK) { throw IOException("sin red") }

        assertEquals(Dummy("guardado antes"), result)
    }

    @Test
    fun `cachedApiCall sin conexion y sin cache previo lanza ApiException`() = runTest {
        val cache = OfflineCache(FakeCachedResponseDao(), json)

        try {
            cachedApiCall<Dummy>(cache, CACHE_KEY, FALLBACK) { throw IOException("sin red") }
            fail("debería haber lanzado ApiException")
        } catch (e: ApiException) {
            assertEquals(FALLBACK, e.message)
        }
    }

    @Test
    fun `cachedApiCall con error http usa el mensaje del backend`() = runTest {
        val cache = OfflineCache(FakeCachedResponseDao(), json)

        try {
            cachedApiCall<Dummy>(cache, CACHE_KEY, FALLBACK) {
                throw httpException(404, """{"status":404,"error":"Not Found","message":"No encontrado."}""")
            }
            fail("debería haber lanzado ApiException")
        } catch (e: ApiException) {
            assertEquals("No encontrado.", e.message)
        }
    }

    @Test
    fun `writeCall exitoso no encola nada`() = runTest {
        val pendingOperationDao = FakePendingOperationDao()

        val outcome = writeCall(
            fallbackMessage = FALLBACK,
            pendingOperationDao = pendingOperationDao,
            json = json,
            type = OperationType.CREATE_ACCOUNT,
            summary = "Cuenta nueva: Débito",
            payload = Dummy("payload"),
        ) { key -> Dummy("aplicado con $key") }

        assertTrue(outcome is WriteOutcome.Applied)
        assertTrue(pendingOperationDao.entities.isEmpty())
    }

    @Test
    fun `writeCall sin conexion encola la operacion y devuelve Queued`() = runTest {
        val pendingOperationDao = FakePendingOperationDao()

        val outcome = writeCall(
            fallbackMessage = FALLBACK,
            pendingOperationDao = pendingOperationDao,
            json = json,
            type = OperationType.CREATE_ACCOUNT,
            summary = "Cuenta nueva: Débito",
            payload = Dummy("payload"),
            pathParams = "card-1",
        ) { throw IOException("sin red") }

        assertEquals(WriteOutcome.Queued, outcome)
        assertEquals(1, pendingOperationDao.entities.size)
        val queued = pendingOperationDao.entities.single()
        assertEquals(OperationType.CREATE_ACCOUNT.name, queued.type)
        assertEquals("card-1", queued.pathParams)
        assertEquals(PendingOperationStatus.PENDING.name, queued.status)
        assertEquals(Dummy("payload"), json.decodeFromString<Dummy>(queued.payloadJson))
    }

    @Test
    fun `writeCall con error http no encola nada y propaga el mensaje`() = runTest {
        val pendingOperationDao = FakePendingOperationDao()

        try {
            writeCall(
                fallbackMessage = FALLBACK,
                pendingOperationDao = pendingOperationDao,
                json = json,
                type = OperationType.CREATE_ACCOUNT,
                summary = "Cuenta nueva: Débito",
                payload = Dummy("payload"),
            ) {
                throw httpException(400, """{"status":400,"error":"Bad Request","message":"Monto inválido."}""")
            }
            fail("debería haber lanzado ApiException")
        } catch (e: ApiException) {
            assertEquals("Monto inválido.", e.message)
        }
        assertFalse(pendingOperationDao.entities.isNotEmpty())
    }

    @Test
    fun `cada llamada a writeCall genera una Idempotency-Key distinta`() = runTest {
        val pendingOperationDao = FakePendingOperationDao()
        val keys = mutableSetOf<String>()

        repeat(3) {
            writeCall(
                fallbackMessage = FALLBACK,
                pendingOperationDao = pendingOperationDao,
                json = json,
                type = OperationType.CREATE_TRANSACTION,
                summary = "Movimiento",
                payload = Dummy("payload"),
            ) { key -> keys += key; throw IOException("sin red") }
        }

        assertEquals(3, keys.size)
        assertEquals(3, pendingOperationDao.entities.map { it.idempotencyKey }.toSet().size)
    }
}
